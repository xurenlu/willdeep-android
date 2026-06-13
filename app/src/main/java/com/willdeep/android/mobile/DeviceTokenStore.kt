package com.willdeep.android.mobile

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

data class StoredGatewayCredential(
    val baseUrl: String,
    val deviceToken: String,
    val desktopName: String,
    val protocolVersion: String,
)

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
            deviceToken = token,
            desktopName = preferences.getString(KEY_DESKTOP_NAME, null) ?: "WillDeep Mac",
            protocolVersion = preferences.getString(KEY_PROTOCOL_VERSION, null) ?: MOBILE_GATEWAY_PROTOCOL_VERSION,
        )
    }

    fun save(credential: StoredGatewayCredential) {
        preferences.edit()
            .putString(KEY_BASE_URL, credential.baseUrl)
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
        const val KEY_DEVICE_TOKEN = "device_token"
        const val KEY_DESKTOP_NAME = "desktop_name"
        const val KEY_PROTOCOL_VERSION = "protocol_version"
    }
}
