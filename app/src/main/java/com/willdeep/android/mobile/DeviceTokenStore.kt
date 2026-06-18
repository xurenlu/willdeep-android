package com.willdeep.android.mobile

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray

data class StoredGatewayCredential(
    val baseUrl: String,
    val fallbackBaseUrls: List<String> = emptyList(),
    val deviceToken: String,
    val desktopName: String,
    val protocolVersion: String,
) {
    fun connectionBaseUrls(): List<String> {
        return connectionBaseUrls(baseUrl, fallbackBaseUrls)
    }
}

class DeviceTokenStore(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val preferences = EncryptedSharedPreferences.create(
        context,
        "willdeep_mobile_gateway",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun load(): StoredGatewayCredential? {
        val baseUrl = preferences.getString(KEY_BASE_URL, null) ?: return null
        val token = preferences.getString(KEY_DEVICE_TOKEN, null) ?: return null
        return StoredGatewayCredential(
            baseUrl = baseUrl,
            fallbackBaseUrls = decodeFallbackBaseUrls(preferences.getString(KEY_FALLBACK_BASE_URLS, null)),
            deviceToken = token,
            desktopName = preferences.getString(KEY_DESKTOP_NAME, null) ?: "WillDeep Mac",
            protocolVersion = preferences.getString(KEY_PROTOCOL_VERSION, null) ?: MOBILE_GATEWAY_PROTOCOL_VERSION,
        )
    }

    fun save(credential: StoredGatewayCredential) {
        preferences.edit()
            .putString(KEY_BASE_URL, credential.baseUrl)
            .putString(KEY_FALLBACK_BASE_URLS, encodeFallbackBaseUrls(credential.fallbackBaseUrls))
            .putString(KEY_DEVICE_TOKEN, credential.deviceToken)
            .putString(KEY_DESKTOP_NAME, credential.desktopName)
            .putString(KEY_PROTOCOL_VERSION, credential.protocolVersion)
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val KEY_BASE_URL = "base_url"
        const val KEY_FALLBACK_BASE_URLS = "fallback_base_urls"
        const val KEY_DEVICE_TOKEN = "device_token"
        const val KEY_DESKTOP_NAME = "desktop_name"
        const val KEY_PROTOCOL_VERSION = "protocol_version"
    }
}

private fun encodeFallbackBaseUrls(urls: List<String>): String {
    val array = JSONArray()
    urls.forEach { array.put(it) }
    return array.toString()
}

private fun decodeFallbackBaseUrls(raw: String?): List<String> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(raw)
        List(array.length()) { index -> array.optString(index).trim() }
            .filter { it.isNotBlank() }
    }.getOrDefault(emptyList())
}
