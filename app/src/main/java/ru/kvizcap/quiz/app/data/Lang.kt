package ru.kvizcap.quiz.app.data

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import java.util.Locale

data class Language(val tag: String, val native: String)

object Lang {
    const val FALLBACK = "ru"
    private const val PREFS = "lang"
    private const val KEY = "tag"

    val all = listOf(
        Language("ru", "Русский"),
        Language("en", "English"),
        Language("be", "Беларуская"),
        Language("kk", "Қазақша"),
        Language("ky", "Кыргызча"),
        Language("uz", "Oʻzbekcha"),
        Language("tg", "Тоҷикӣ"),
        Language("az", "Azərbaycanca"),
        Language("hy", "Հայերեն"),
        Language("ro", "Română"),
    )

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun selected(context: Context): String =
        if (Build.VERSION.SDK_INT >= 33) {
            context.getSystemService(LocaleManager::class.java).applicationLocales.let { if (it.isEmpty) "" else normalize(it[0].language) }
        } else prefs(context).getString(KEY, "").orEmpty()

    fun resolve(context: Context): String = normalize(selected(context).ifEmpty { Resources.getSystem().configuration.locales[0].language })

    private fun normalize(language: String): String = when (language) {
        "mo" -> "ro"
        else -> language
    }.takeIf { tag -> all.any { it.tag == tag } } ?: FALLBACK

    fun apply(activity: Activity, tag: String) {
        if (Build.VERSION.SDK_INT >= 33) {
            activity.getSystemService(LocaleManager::class.java).applicationLocales =
                if (tag.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
        } else {
            prefs(activity).edit().putString(KEY, tag).apply()
            activity.recreate()
        }
    }

    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= 33) return base
        val tag = prefs(base).getString(KEY, "").orEmpty()
        val locale = if (tag.isEmpty()) Resources.getSystem().configuration.locales[0] else Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        if (tag.isEmpty()) return base
        val conf = Configuration(base.resources.configuration)
        conf.setLocale(locale)
        return base.createConfigurationContext(conf)
    }
}
