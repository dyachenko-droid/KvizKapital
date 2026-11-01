package ru.kvizcap.quiz.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ru.kvizcap.quiz.app.R

@Immutable
data class AppColors(
    val dark: Boolean,
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val ink: Color,
    val ink2: Color,
    val pri: Color,
    val priDeep: Color,
    val priInk: Color,
    val priSoft: Color,
    val priSoftInk: Color,
    val hero: Color,
    val heroInk2: Color,
    val gold: Color,
    val goldInk: Color,
    val goldSoft: Color,
    val goldText: Color,
    val mint: Color,
    val mintSoft: Color,
    val mintInk: Color,
    val coral: Color,
    val coralSoft: Color,
    val coralInk: Color,
    val flameSoft: Color,
    val flameInk: Color,
    val sky: Color,
    val ground: Color,
    val groundL: Color,
    val groundR: Color,
    val shadow: Color,
    val scrim: Color,
)

val LightColors = AppColors(
    dark = false,
    bg = Color(0xFFF3F7F2), surface = Color(0xFFFFFFFF), surface2 = Color(0xFFE9F2EC), line = Color(0xFFD9E6DD),
    ink = Color(0xFF0F1F17), ink2 = Color(0xFF4A5E53),
    pri = Color(0xFF0E6B47), priDeep = Color(0xFF0A4F34), priInk = Color(0xFFFFFFFF), priSoft = Color(0xFFD5EEDF), priSoftInk = Color(0xFF0A4F34),
    hero = Color(0xFF0B4D34), heroInk2 = Color(0xFFB9DCC9),
    gold = Color(0xFFE9B44C), goldInk = Color(0xFF3D2A05), goldSoft = Color(0xFFFBEFD3), goldText = Color(0xFF8A5A08),
    mint = Color(0xFF2FBF7A), mintSoft = Color(0xFFDCF5E8), mintInk = Color(0xFF0B5A35),
    coral = Color(0xFFE2614B), coralSoft = Color(0xFFFCE4DF), coralInk = Color(0xFF9A2F1F),
    flameSoft = Color(0xFFFDE9D6), flameInk = Color(0xFF8A3D0A),
    sky = Color(0xFFE1F0E5), ground = Color(0xFFBFDDC4), groundL = Color(0xFF9CCBA7), groundR = Color(0xFF86B993),
    shadow = Color(0x1A0B4D34), scrim = Color(0x730A1912),
)

val DarkColors = AppColors(
    dark = true,
    bg = Color(0xFF0B130F), surface = Color(0xFF131E18), surface2 = Color(0xFF1A2921), line = Color(0xFF26382E),
    ink = Color(0xFFE6F1EA), ink2 = Color(0xFF9EB4A8),
    pri = Color(0xFF4ED39A), priDeep = Color(0xFF1F7A55), priInk = Color(0xFF062016), priSoft = Color(0xFF16392A), priSoftInk = Color(0xFFA6EBC9),
    hero = Color(0xFF0F3B2A), heroInk2 = Color(0xFF9CCDB4),
    gold = Color(0xFFF0C062), goldInk = Color(0xFF2E2005), goldSoft = Color(0xFF3A2D12), goldText = Color(0xFFF3D08A),
    mint = Color(0xFF57E0A0), mintSoft = Color(0xFF12372A), mintInk = Color(0xFFA9F0CB),
    coral = Color(0xFFFF8A75), coralSoft = Color(0xFF3D1D18), coralInk = Color(0xFFFFC4B8),
    flameSoft = Color(0xFF3A2414), flameInk = Color(0xFFFFC58F),
    sky = Color(0xFF0E1B15), ground = Color(0xFF1D3A2B), groundL = Color(0xFF132A1F), groundR = Color(0xFF0E2118),
    shadow = Color(0x4D000000), scrim = Color(0x8C000000),
)

val Unbounded = FontFamily(
    Font(R.font.unbounded_semibold, FontWeight.SemiBold),
    Font(R.font.unbounded_bold, FontWeight.Bold),
)

val Manrope = FontFamily(
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
)

object Type {
    val display = TextStyle(fontFamily = Unbounded, fontWeight = FontWeight.SemiBold, fontSize = 44.sp, lineHeight = 46.sp, fontFeatureSettings = "tnum")
    val number = TextStyle(fontFamily = Unbounded, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 22.sp, fontFeatureSettings = "tnum")
    val numberSmall = TextStyle(fontFamily = Unbounded, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 15.sp, fontFeatureSettings = "tnum")
    val title = TextStyle(fontFamily = Unbounded, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 30.sp)
    val titleSmall = TextStyle(fontFamily = Unbounded, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 24.sp)
    val heading = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp)
    val question = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp)
    val body = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 22.sp)
    val label = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 18.sp)
    val small = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 17.sp)
    val tiny = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 13.sp)
    val caps = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 14.sp, letterSpacing = 0.9.sp)
    val button = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, lineHeight = 20.sp)
}

val LocalColors = staticCompositionLocalOf { LightColors }

object App {
    val c: AppColors @Composable get() = LocalColors.current
}

@Composable
fun KvizTheme(dark: Boolean, content: @Composable () -> Unit) {
    val colors = if (dark) DarkColors else LightColors
    val scheme = if (dark) darkColorScheme(primary = colors.pri, background = colors.bg, surface = colors.surface)
    else lightColorScheme(primary = colors.pri, background = colors.bg, surface = colors.surface)
    CompositionLocalProvider(LocalColors provides colors) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
