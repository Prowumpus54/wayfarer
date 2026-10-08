package com.wayfarer.rpg

import android.content.Context
import android.net.Uri
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

enum class GemmaModelState { NOT_INSTALLED, INSTALLED, LOADING, READY, ERROR }
data class GemmaModelStatus(val state: GemmaModelState, val message: String)

/**
 * Google LiteRT-LM 0.18.0 Kotlin API, not MediaPipe LLM Inference.
 * https://developers.google.com/edge/litert-lm/android
 * User obtains compatible Gemma .litertlm weights and accepts their license externally.
 * No credential-free/fake download, bundled weights, or desktop transport here.
 */
class OnDeviceGemmaRuntime private constructor(context: Context) : GmRuntime {
    private val app = context.applicationContext
    private val store = GemmaModelStore(File(app.filesDir, "gemma"))
    private val mutex = Mutex()
    private var engine: Engine? = null
    private val mutableStatus = MutableStateFlow(
        if (store.installed()) GemmaModelStatus(GemmaModelState.INSTALLED, "Installed; loads when On-device or Auto is selected")
        else GemmaModelStatus(GemmaModelState.NOT_INSTALLED, "Not installed. Import a compatible Gemma .litertlm model.")
    )
    val status: StateFlow<GemmaModelStatus> = mutableStatus

    suspend fun prepare() = withContext(Dispatchers.IO) {
        mutex.withLock { if (store.installed()) initialize() }
    }

    suspend fun importModel(uri: Uri) = withContext(Dispatchers.IO) {
        mutex.withLock {
            mutableStatus.value = GemmaModelStatus(GemmaModelState.LOADING, "Importing model into private storage")
            try {
                // SAF grants access to this single document; copy streams without loading GBs into RAM.
                app.contentResolver.openInputStream(uri)?.use { store.importModel(it) }
                    ?: error("Cannot open selected document")
                closeEngine()
                initialize()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableStatus.value = GemmaModelStatus(GemmaModelState.ERROR,
                    "Import failed (${error.javaClass.simpleName}). Check model format and free storage; retry import.")
            }
        }
    }

    private fun closeEngine() {
        val previous = engine
        engine = null
        previous?.close()
    }

    private fun initialize(): Engine {
        engine?.let {
            mutableStatus.value = GemmaModelStatus(GemmaModelState.READY, "Ready: on-device Gemma (CPU)")
            return it
        }
        check(store.installed()) { "Model not installed. Import a Gemma .litertlm model." }
        mutableStatus.value = GemmaModelStatus(GemmaModelState.LOADING, "Loading on-device Gemma")
        var candidate: Engine? = null
        try {
            candidate = Engine(EngineConfig(
                modelPath = store.modelFile.absolutePath,
                backend = Backend.CPU(),
                cacheDir = app.cacheDir.absolutePath
            ))
            candidate.initialize()
            engine = candidate
            mutableStatus.value = GemmaModelStatus(GemmaModelState.READY, "Ready: on-device Gemma (CPU)")
            return candidate
        } catch (error: Exception) {
            runCatching { candidate?.close() }
            mutableStatus.value = GemmaModelStatus(GemmaModelState.ERROR,
                "Model could not load (${error.javaClass.simpleName}). Import a compatible model or retry.")
            throw error
        } catch (error: LinkageError) {
            runCatching { candidate?.close() }
            mutableStatus.value = GemmaModelStatus(GemmaModelState.ERROR, "LiteRT-LM native runtime unavailable on this device")
            throw IllegalStateException("LiteRT-LM native runtime unavailable on this device", error)
        }
    }

    override suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                initialize().createConversation().use { conversation ->
                    conversation.sendMessage(prompt).toString().also {
                        check(it.isNotBlank()) { "On-device Gemma returned an empty response" }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                runCatching { closeEngine() }
                if (!store.installed()) {
                    mutableStatus.value = GemmaModelStatus(GemmaModelState.NOT_INSTALLED, "Model not installed. Import a Gemma .litertlm model.")
                } else {
                    mutableStatus.value = GemmaModelStatus(GemmaModelState.ERROR, "On-device inference failed (${error.javaClass.simpleName}); retry or import a compatible model.")
                }
                throw IllegalStateException(mutableStatus.value.message, error)
            } catch (error: LinkageError) {
                runCatching { closeEngine() }
                mutableStatus.value = GemmaModelStatus(GemmaModelState.ERROR, "LiteRT-LM native runtime unavailable on this device")
                throw IllegalStateException(mutableStatus.value.message, error)
            }
        }
    }

    companion object {
        @Volatile private var instance: OnDeviceGemmaRuntime? = null
        fun get(context: Context): OnDeviceGemmaRuntime = instance ?: synchronized(this) {
            instance ?: OnDeviceGemmaRuntime(context).also { instance = it }
        }
    }
}
