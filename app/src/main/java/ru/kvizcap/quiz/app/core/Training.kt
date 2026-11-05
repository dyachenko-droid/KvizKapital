package ru.kvizcap.quiz.app.core

/** Avoid recommendations based on one lucky or unlucky answer. */
fun weakestTopic(profile: Profile): TopicId? = TopicId.entries
    .filter { (profile.stats.perTopic[it.key]?.answered ?: 0) >= 5 }
    .filter { profile.stats.perTopic.getValue(it.key).accuracy < 85 }
    .minWithOrNull(compareBy<TopicId> { profile.stats.perTopic.getValue(it.key).accuracy }
        .thenByDescending { profile.stats.perTopic.getValue(it.key).answered }.thenBy { it.ordinal })
