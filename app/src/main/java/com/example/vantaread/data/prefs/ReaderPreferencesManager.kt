package com.example.vantaread.data.prefs

import android.content.Context
import android.content.SharedPreferences
import com.example.vantaread.data.model.ReaderFont
import com.example.vantaread.data.model.ReaderSettings
import com.example.vantaread.data.model.ReaderTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReaderPreferencesManager @Inject constructor(@ApplicationContext context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("reader_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<ReaderSettings> = _settings.asStateFlow()

    private fun loadSettings(): ReaderSettings {
        val themeName = prefs.getString("theme", ReaderTheme.VANTA_BLACK.name) ?: ReaderTheme.VANTA_BLACK.name
        val fontName = prefs.getString("font", ReaderFont.SERIF.name) ?: ReaderFont.SERIF.name
        val fontSize = prefs.getInt("font_size", 18)
        val horizontalMargin = prefs.getInt("margin", 16)
        
        return ReaderSettings(
            themeMode = ReaderTheme.valueOf(themeName),
            fontSizeSp = fontSize,
            fontType = ReaderFont.valueOf(fontName),
            horizontalMarginDp = horizontalMargin
        )
    }

    fun updateSettings(newSettings: ReaderSettings) {
        prefs.edit().apply {
            putString("theme", newSettings.themeMode.name)
            putString("font", newSettings.fontType.name)
            putInt("font_size", newSettings.fontSizeSp)
            putInt("margin", newSettings.horizontalMarginDp)
            apply()
        }
        _settings.value = newSettings
    }
}
