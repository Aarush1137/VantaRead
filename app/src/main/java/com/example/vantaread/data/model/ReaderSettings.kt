package com.example.vantaread.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

data class ReaderSettings(
    val themeMode: ReaderTheme = ReaderTheme.VANTA_BLACK,
    val fontSizeSp: Int = 18,
    val fontType: ReaderFont = ReaderFont.SERIF,
    val horizontalMarginDp: Int = 16,
    val lineHeight: Float = 1.5f,
    val textAlignment: String = "Left",
    val accentColorHex: String = "#8A2BE2" // Vanta Purple Default
)

enum class ReaderTheme(val backgroundColor: Color, val textColor: Color) {
    VANTA_BLACK(Color(0xFF000000), Color(0xFFE0E0E0)),
    CHARCOAL(Color(0xFF1C1C1E), Color(0xFFE5E5EA)),
    SEPIA(Color(0xFFF4ECD8), Color(0xFF5B4636)),
    LIGHT(Color(0xFFFFFFFF), Color(0xFF1C1C1E))
}

enum class ReaderFont(val fontFamily: FontFamily, val androidTypeface: android.graphics.Typeface) {
    SERIF(FontFamily.Serif, android.graphics.Typeface.SERIF),
    SANS_SERIF(FontFamily.SansSerif, android.graphics.Typeface.SANS_SERIF),
    MONOSPACE(FontFamily.Monospace, android.graphics.Typeface.MONOSPACE)
}
