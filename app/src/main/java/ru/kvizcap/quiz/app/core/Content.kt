package ru.kvizcap.quiz.app.core

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

data class Question(
    val id: String,
    val topic: TopicId,
    val difficulty: Difficulty,
    val text: String,
    val options: List<String>,
    val correct: Int,
    val hint: String,
    val explanation: String,
)

data class Statement(val id: Int, val text: String, val isTrue: Boolean, val explanation: String)

class Content(val questions: List<Question>, val statements: List<Statement>) {
    private val byId = questions.associateBy { it.id }

    fun question(id: String): Question? = byId[id]
    fun ofTopic(topic: TopicId) = questions.filter { it.topic == topic }

    companion object {
        private val files = listOf("q_core.json", "q_more.json", "q_extra.json")

        private fun read(context: Context, lang: String, file: String): String {
            val path = "content/$lang/$file"
            val stream = runCatching { context.assets.open(path) }.getOrElse { context.assets.open("content/ru/$file") }
            return stream.bufferedReader().use { it.readText() }
        }

        fun load(context: Context, lang: String): Content {
            val questions = mutableListOf<Question>()
            files.forEach { file ->
                val root = Json.parseToJsonElement(read(context, lang, file)) as JsonObject
                root.forEach { (key, value) ->
                    val topic = TopicId.of(key)
                    value.jsonArray.forEachIndexed { index, element ->
                        val a = element as JsonArray
                        questions += Question(
                            id = "${key}_$index",
                            topic = topic,
                            difficulty = Difficulty.of(a[0].jsonPrimitive.content.first()),
                            text = a[1].jsonPrimitive.content,
                            options = a[2].jsonArray.map { it.jsonPrimitive.content },
                            correct = a[3].jsonPrimitive.int,
                            hint = a[4].jsonPrimitive.content,
                            explanation = a[5].jsonPrimitive.content,
                        )
                    }
                }
            }
            val statements = Json.parseToJsonElement(read(context, lang, "statements.json"))
                .jsonArray.mapIndexed { index, element ->
                    val a = element.jsonArray
                    Statement(index, a[0].jsonPrimitive.content, a[1].jsonPrimitive.boolean, a[2].jsonPrimitive.content)
                }
            return Content(questions, statements)
        }
    }
}
