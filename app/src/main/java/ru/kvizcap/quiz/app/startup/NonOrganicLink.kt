package ru.kvizcap.quiz.app.startup

import java.net.URLDecoder
import java.net.URLEncoder

private val macroAliases = listOf(
    listOf("ad.id", "ad_id", "af_ad_id", "adgroup_id"),
    listOf("adset.id", "adset_id", "af_adset_id"),
    listOf("campaign.id", "campaign_id", "af_c_id"),
    listOf("ad.name", "ad_name", "af_ad", "adgroup", "adgroup_name", "ad"),
    listOf("adset.name", "adset_name", "adset", "af_adset"),
    listOf("campaign.name", "campaign_name", "campaign", "c"),
    listOf("app_id", "appsflyer_id"),
).flatMap { group -> group.map { it to group } }.toMap()

// Match whole macros once, including templates already escaped by a URL editor.
private val macroPattern = Regex(
    "\\{\\{([A-Za-z0-9_.-]+)}}|\\{([A-Za-z0-9_.-]+)}|" +
        "(?i:%7B%7B)([A-Za-z0-9_.-]+)(?i:%7D%7D)|(?i:%7B)([A-Za-z0-9_.-]+)(?i:%7D)",
)

/** Fills available macros, then appends UID and campaign parts without normalizing the address. */
fun nonOrganicLink(
    raw: String,
    appsFlyerId: String,
    campaign: String?,
    attribution: Map<String, String> = emptyMap(),
): String {
    val values = attribution.filterValues { it.isNotEmpty() }.toMutableMap()
    if (!campaign.isNullOrEmpty()) values["campaign"] = campaign
    val campaignName = values["campaign"] ?: macroAliases.getValue("campaign.name").firstNotNullOfOrNull { values[it] }
    val parts = campaignName?.takeIf { it.isNotEmpty() }?.split('_').orEmpty()
    values["app_id"] = appsFlyerId
    values["appsflyer_id"] = appsFlyerId
    parts.forEachIndexed { index, part ->
        values["sub${index + 1}"] = part
        values["sub_${index + 1}"] = part
    }
    val expanded = macroPattern.replace(raw) { match ->
        val key = match.groupValues.drop(1).first { it.isNotEmpty() }
        val value = values[key] ?: macroAliases[key.lowercase()]?.firstNotNullOfOrNull { values[it] }
        value?.let(::encodeQueryValue) ?: match.value
    }
    val fragmentAt = expanded.indexOf('#')
    val fragment = if (fragmentAt >= 0) expanded.substring(fragmentAt) else ""
    val address = if (fragmentAt >= 0) expanded.substring(0, fragmentAt) else expanded
    val queryAt = address.indexOf('?')
    val base = if (queryAt >= 0) address.substring(0, queryAt) else address
    val originalQuery = if (queryAt >= 0) address.substring(queryAt + 1) else ""
    val query = if (originalQuery.isEmpty()) emptyList() else originalQuery.split('&')
    val retained = query.filterNot { part ->
        val rawKey = part.substringBefore('=')
        val key = runCatching { URLDecoder.decode(rawKey, "UTF-8") }.getOrDefault(rawKey)
        key == "app_id" || Regex("sub_?[1-9][0-9]*").matches(key)
    }
    val parameters = mutableListOf("app_id=${encodeQueryValue(appsFlyerId)}")
    parts.forEachIndexed { index, part -> parameters += "sub${index + 1}=${encodeQueryValue(part)}" }
    return "$base?${(retained + parameters).joinToString("&")}$fragment"
}

private fun encodeQueryValue(value: String): String = URLEncoder.encode(value, "UTF-8")
    .replace("+", "%20").replace("*", "%2A").replace("%7E", "~")
