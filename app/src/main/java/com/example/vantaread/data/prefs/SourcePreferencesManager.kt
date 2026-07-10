package com.example.vantaread.data.prefs

import android.content.Context
import android.content.SharedPreferences
import com.example.vantaread.data.source.SourceCatalog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SourcePreferencesManager @Inject constructor(@ApplicationContext context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("source_prefs", Context.MODE_PRIVATE)

    private val _activeSourceId = MutableStateFlow(loadActiveSource())
    val activeSourceId: StateFlow<String> = _activeSourceId.asStateFlow()

    private fun loadActiveSource(): String {
        return SourceCatalog.normalize(
            prefs.getString("active_source", SourceCatalog.DEFAULT_SOURCE_ID) ?: SourceCatalog.DEFAULT_SOURCE_ID
        )
    }

    fun setActiveSource(sourceId: String) {
        val normalized = SourceCatalog.normalize(sourceId)
        prefs.edit().putString("active_source", normalized).apply()
        _activeSourceId.value = normalized
    }
}
