package com.example.vantaread.data.prefs

import android.content.Context
import android.content.SharedPreferences
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
        return prefs.getString("active_source", "novelfull") ?: "novelfull"
    }

    fun setActiveSource(sourceId: String) {
        prefs.edit().putString("active_source", sourceId).apply()
        _activeSourceId.value = sourceId
    }
}
