package ru.kvizcap.quiz.app.data

import android.content.Context
import kotlinx.serialization.json.Json
import ru.kvizcap.quiz.app.core.Profile
import java.io.File

class Storage(context: Context) {
    private val file = File(context.filesDir, "profile.json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun load(): Profile = runCatching { json.decodeFromString(Profile.serializer(), file.readText()) }.getOrDefault(Profile())

    fun save(profile: Profile) {
        val tmp = File(file.parentFile, "profile.json.tmp")
        tmp.writeText(json.encodeToString(Profile.serializer(), profile))
        tmp.renameTo(file)
    }
}
