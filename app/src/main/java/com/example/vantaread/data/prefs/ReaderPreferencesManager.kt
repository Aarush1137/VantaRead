package com.example.vantaread.data.prefs

import android.content.Context
import android.content.SharedPreferences
import com.example.vantaread.data.model.AppAccent
import com.example.vantaread.data.model.ReaderFont
import com.example.vantaread.data.model.ReaderSettings
import com.example.vantaread.data.model.ReaderTheme
import com.example.vantaread.data.source.SourceCatalog
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

    private val _theme = MutableStateFlow(prefs.getString("app_theme", "system") ?: "system")
    val theme: StateFlow<String> = _theme.asStateFlow()

    private val _accent = MutableStateFlow(
        runCatching {
            AppAccent.valueOf(prefs.getString("app_accent", AppAccent.VANTA_PURPLE.name) ?: AppAccent.VANTA_PURPLE.name)
        }.getOrDefault(AppAccent.VANTA_PURPLE)
    )
    val accent: StateFlow<AppAccent> = _accent.asStateFlow()

    private val _defaultSource = MutableStateFlow(
        SourceCatalog.normalize(
            prefs.getString("default_source", SourceCatalog.DEFAULT_SOURCE_ID) ?: SourceCatalog.DEFAULT_SOURCE_ID
        )
    )
    val defaultSource: StateFlow<String> = _defaultSource.asStateFlow()

    private fun loadSettings(): ReaderSettings {
        val themeName = prefs.getString("theme", ReaderTheme.VANTA_BLACK.name) ?: ReaderTheme.VANTA_BLACK.name
        val fontName = prefs.getString("font", ReaderFont.SERIF.name) ?: ReaderFont.SERIF.name
        val fontSize = prefs.getInt("font_size", 18)
        val horizontalMargin = prefs.getInt("margin", 16)
        val lineHeight = prefs.getFloat("line_height", 1.5f)
        val textAlignment = prefs.getString("text_alignment", "Left") ?: "Left"
        val ttsVoiceName = prefs.getString("tts_voice_name", null)
        val ttsLocaleTag = prefs.getString("tts_locale_tag", "en-US") ?: "en-US"
        val ttsSpeechRate = prefs.getFloat("tts_speech_rate", 1.0f)

        return ReaderSettings(
            themeMode = runCatching { ReaderTheme.valueOf(themeName) }.getOrDefault(ReaderTheme.VANTA_BLACK),
            fontSizeSp = fontSize,
            fontType = runCatching { ReaderFont.valueOf(fontName) }.getOrDefault(ReaderFont.SERIF),
            horizontalMarginDp = horizontalMargin,
            lineHeight = lineHeight,
            textAlignment = textAlignment,
            ttsVoiceName = ttsVoiceName,
            ttsLocaleTag = ttsLocaleTag,
            ttsSpeechRate = ttsSpeechRate
        )
    }

    fun setTheme(theme: String) {
        prefs.edit().putString("app_theme", theme).apply()
        _theme.value = theme
    }

    fun setAccent(accent: AppAccent) {
        prefs.edit().putString("app_accent", accent.name).apply()
        _accent.value = accent
    }

    fun setDefaultSource(sourceId: String) {
        val normalized = SourceCatalog.normalize(sourceId)
        prefs.edit().putString("default_source", normalized).apply()
        _defaultSource.value = normalized
    }

    private val _batchDownloadAmount = MutableStateFlow(prefs.getInt("batch_download", 0))
    val batchDownloadAmount: StateFlow<Int> = _batchDownloadAmount.asStateFlow()

    fun setBatchDownloadAmount(amount: Int) {
        prefs.edit().putInt("batch_download", amount).apply()
        _batchDownloadAmount.value = amount
    }

    fun updateSettings(newSettings: ReaderSettings) {
        prefs.edit().apply {
            putString("theme", newSettings.themeMode.name)
            putString("font", newSettings.fontType.name)
            putInt("font_size", newSettings.fontSizeSp)
            putInt("margin", newSettings.horizontalMarginDp)
            putFloat("line_height", newSettings.lineHeight)
            putString("text_alignment", newSettings.textAlignment)
            putString("tts_voice_name", newSettings.ttsVoiceName)
            putString("tts_locale_tag", newSettings.ttsLocaleTag)
            putFloat("tts_speech_rate", newSettings.ttsSpeechRate)
            apply()
        }
        _settings.value = newSettings
    }
}
