package com.example.cinestack.data.local

import android.content.Context
import android.content.SharedPreferences

class ProfileManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_profile", Context.MODE_PRIVATE)

    fun saveProfile(name: String, apiKey: String) {
        prefs.edit().apply {
            putString("user_name", name)
            putString("tmdb_api_key", apiKey)
            apply()
        }
    }

    fun getUserName(): String = prefs.getString("user_name", "") ?: ""
    
    fun getApiKey(): String = prefs.getString("tmdb_api_key", "201b96b1a333e456ac48450aaf405103") ?: "201b96b1a333e456ac48450aaf405103"
}
