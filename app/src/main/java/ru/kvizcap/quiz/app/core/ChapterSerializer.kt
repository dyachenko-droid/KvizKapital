package ru.kvizcap.quiz.app.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** Resource IDs belong to a build, so backup files store only the stable chapter index. */
object ChapterSerializer : KSerializer<Chapter> {
    override val descriptor = PrimitiveSerialDescriptor("CareerChapter", PrimitiveKind.INT)
    override fun serialize(encoder: Encoder, value: Chapter) = encoder.encodeInt(value.index)
    override fun deserialize(decoder: Decoder): Chapter = Career.chapters.getOrNull(decoder.decodeInt())
        ?: throw IllegalArgumentException("Unknown career chapter")
}
