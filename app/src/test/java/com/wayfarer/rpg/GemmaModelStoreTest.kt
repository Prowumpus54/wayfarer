package com.wayfarer.rpg

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GemmaModelStoreTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun detectsInstalledModelAfterImportAndRestart() {
        val directory = temporary.newFolder()
        val store = GemmaModelStore(directory)
        assertFalse(store.installed())
        store.importModel(ByteArrayInputStream(byteArrayOf(1, 2, 3)))
        assertTrue(GemmaModelStore(directory).installed())
        assertArrayEquals(byteArrayOf(1, 2, 3), store.modelFile.readBytes())
        assertEquals(listOf("gemma.litertlm"), directory.list()!!.toList())
    }

    @Test fun interruptedImportPreservesExistingModelAndCleansPartial() {
        val directory = temporary.newFolder()
        val store = GemmaModelStore(directory)
        store.importModel(ByteArrayInputStream(byteArrayOf(9)))
        val broken = object : InputStream() {
            override fun read(): Int = throw IOException("Disconnected provider")
        }
        try {
            store.importModel(broken)
            fail("Expected import failure")
        } catch (_: IOException) {
            assertArrayEquals(byteArrayOf(9), store.modelFile.readBytes())
            assertEquals(listOf("gemma.litertlm"), directory.list()!!.toList())
        }
    }

    @Test fun emptyImportIsRejectedWithoutReplacingModel() {
        val store = GemmaModelStore(temporary.newFolder())
        store.importModel(ByteArrayInputStream(byteArrayOf(9)))
        try {
            store.importModel(ByteArrayInputStream(byteArrayOf()))
            fail("Expected empty model rejection")
        } catch (_: IllegalStateException) {
            assertArrayEquals(byteArrayOf(9), store.modelFile.readBytes())
        }
    }

    @Test fun replacementDoesNotAppendToOldWeights() {
        val store = GemmaModelStore(temporary.newFolder())
        store.importModel(ByteArrayInputStream(byteArrayOf(1, 2, 3)))
        store.importModel(ByteArrayInputStream(byteArrayOf(4)))
        assertArrayEquals(byteArrayOf(4), store.modelFile.readBytes())
    }
}
