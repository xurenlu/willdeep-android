package com.willdeep.android.push

import android.content.Context

data class MobilePushToken(
    val provider: String,
    val clientId: String,
    val appId: String,
)

class MobilePushTokenStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "willdeep_mobile_push",
        Context.MODE_PRIVATE,
    )

    fun save(token: MobilePushToken) {
        preferences.edit()
            .putString(KEY_PROVIDER, token.provider)
            .putString(KEY_CLIENT_ID, token.clientId)
            .putString(KEY_APP_ID, token.appId)
            .apply()
    }

    fun load(): MobilePushToken? {
        val provider = preferences.getString(KEY_PROVIDER, null)?.trim().orEmpty()
        val clientId = preferences.getString(KEY_CLIENT_ID, null)?.trim().orEmpty()
        val appId = preferences.getString(KEY_APP_ID, null)?.trim().orEmpty()
        if (provider.isBlank() || clientId.isBlank()) return null
        return MobilePushToken(
            provider = provider,
            clientId = clientId,
            appId = appId,
        )
    }

    private companion object {
        const val KEY_PROVIDER = "provider"
        const val KEY_CLIENT_ID = "client_id"
        const val KEY_APP_ID = "app_id"
    }
}
