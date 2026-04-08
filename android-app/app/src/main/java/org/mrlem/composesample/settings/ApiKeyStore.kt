package org.mrlem.composesample.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiKeyStore @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var claudeApiKey: String
        get() = prefs.getString("claude_api_key", "") ?: ""
        set(value) { prefs.edit().putString("claude_api_key", value).apply() }
}
