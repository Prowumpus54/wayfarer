package com.wayfarer.rpg

import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** App-private weights, not assets/APK content. Caller serializes import with inference. */
class GemmaModelStore(private val directory: File) {
    val modelFile: File get() = File(directory, "gemma.litertlm")
    fun installed(): Boolean = modelFile.isFile && modelFile.length() > 0

    fun importModel(input: InputStream) {
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create model directory" }
        val temporary = File.createTempFile("import-", ".partial", directory)
        try {
            temporary.outputStream().use { output -> input.copyTo(output) }
            check(temporary.length() > 0) { "Selected model is empty" }
            // Same filesystem atomic replacement preserves the previous model if copy fails.
            try {
                Files.move(
                    temporary.toPath(),
                    modelFile.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
                )
            } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                // The copy is already complete in app-private storage; only the final
                // same-filesystem replacement loses atomicity on older/providers' filesystems.
                Files.move(
                    temporary.toPath(),
                    modelFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        } finally {
            temporary.delete()
        }
    }
}
