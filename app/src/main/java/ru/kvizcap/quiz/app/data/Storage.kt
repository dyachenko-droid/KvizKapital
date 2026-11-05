package ru.kvizcap.quiz.app.data

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** One atomic root retains the legacy Profile shape; old builds ignore the native session fields. */
class ProgressFile(private val file: File) {
    private var writtenRevision = -1L
    private val backup = File(file.path + ".bak")

    @Synchronized fun load(): SavedGame {
        if (!file.exists() && !backup.exists()) return SavedGame(ru.kvizcap.quiz.app.core.Profile())
        return runCatching { ProgressCodec.decodeStored(file.readText()) }.getOrElse { failure ->
            if (backup.exists()) ProgressCodec.decodeStored(backup.readText()) else throw failure
        }
    }

    @Synchronized fun save(save: SavedGame, revision: Long) {
        if (revision <= writtenRevision) return
        val bytes = ProgressCodec.encodeStored(save).toByteArray(Charsets.UTF_8)
        require(bytes.size <= ProgressCodec.MAX_BYTES)
        if (file.exists()) {
            val previous = file.readBytes()
            if (runCatching { ProgressCodec.decodeStored(previous.toString(Charsets.UTF_8)) }.isSuccess) atomicWrite(backup, previous)
        }
        atomicWrite(file, bytes)
        writtenRevision = revision
    }

    private fun atomicWrite(target: File, bytes: ByteArray) {
        val temp = File(target.path + ".tmp")
        FileOutputStream(temp).use { out -> out.write(bytes); out.fd.sync() }
        Files.move(temp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }
}

class Storage(context: Context) {
    private val files = ProgressFile(File(context.filesDir, "profile.json"))
    fun load(): SavedGame = files.load()
    fun save(save: SavedGame, revision: Long) = files.save(save, revision)
}
