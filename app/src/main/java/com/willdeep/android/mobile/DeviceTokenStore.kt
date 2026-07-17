package com.willdeep.android.mobile

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject
import java.security.GeneralSecurityException
import java.security.MessageDigest

data class StoredGatewayCredential(
    val baseUrl: String,
    val fallbackBaseUrls: List<String> = emptyList(),
    val deviceToken: String,
    val desktopName: String,
    val protocolVersion: String,
    val relayRoom: String? = null,
    val lastDesktopResponseAtEpochMillis: Long = 0L,
) {
    val id: String
        get() = stableGatewayCredentialId(relayRoom, baseUrl)

    fun connectionBaseUrls(): List<String> {
        return connectionBaseUrls(baseUrl, fallbackBaseUrls)
    }
}

class DeviceTokenStore(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val preferences = createEncryptedPreferences(context, masterKey)

    fun load(): StoredGatewayCredential? {
        val credentials = loadAll()
        if (credentials.isEmpty()) return null
        val selectedId = preferences.getString(KEY_SELECTED_CREDENTIAL_ID, null)
        return credentials.firstOrNull { it.id == selectedId } ?: credentials.first()
    }

    fun loadAll(): List<StoredGatewayCredential> {
        preferences.getString(KEY_CREDENTIALS_JSON, null)?.let { raw ->
            return decodeStoredGatewayCredentials(raw)
        }
        return loadLegacyCredential()?.let(::listOf).orEmpty()
    }

    private fun loadLegacyCredential(): StoredGatewayCredential? {
        val baseUrl = preferences.getString(KEY_BASE_URL, null) ?: return null
        val token = preferences.getString(KEY_DEVICE_TOKEN, null) ?: return null
        return StoredGatewayCredential(
            baseUrl = baseUrl,
            fallbackBaseUrls = decodeFallbackBaseUrls(preferences.getString(KEY_FALLBACK_BASE_URLS, null)),
            deviceToken = token,
            desktopName = preferences.getString(KEY_DESKTOP_NAME, null) ?: "WillDeep Mac",
            protocolVersion = preferences.getString(KEY_PROTOCOL_VERSION, null) ?: MOBILE_GATEWAY_PROTOCOL_VERSION,
            relayRoom = preferences.getString(KEY_RELAY_ROOM, null)?.takeIf { it.isNotBlank() },
        )
    }

    fun save(credential: StoredGatewayCredential) {
        val credentials = loadAll()
        val existing = credentials.firstOrNull { it.id == credential.id }
        val normalized = if (credential.lastDesktopResponseAtEpochMillis == 0L && existing != null) {
            credential.copy(lastDesktopResponseAtEpochMillis = existing.lastDesktopResponseAtEpochMillis)
        } else {
            credential
        }
        val updated = listOf(normalized) + credentials.filterNot { it.id == normalized.id }
        preferences.edit()
            .putString(KEY_CREDENTIALS_JSON, encodeStoredGatewayCredentials(updated))
            .putString(KEY_SELECTED_CREDENTIAL_ID, normalized.id)
            .remove(KEY_BASE_URL)
            .remove(KEY_FALLBACK_BASE_URLS)
            .remove(KEY_DEVICE_TOKEN)
            .remove(KEY_DESKTOP_NAME)
            .remove(KEY_PROTOCOL_VERSION)
            .remove(KEY_RELAY_ROOM)
            .apply()
    }

    fun select(credentialId: String): StoredGatewayCredential? {
        val credential = loadAll().firstOrNull { it.id == credentialId } ?: return null
        preferences.edit().putString(KEY_SELECTED_CREDENTIAL_ID, credential.id).apply()
        return credential
    }

    fun remove(credentialId: String): StoredGatewayCredential? {
        val remaining = loadAll().filterNot { it.id == credentialId }
        val nextSelected = remaining.firstOrNull()
        preferences.edit()
            .putString(KEY_CREDENTIALS_JSON, encodeStoredGatewayCredentials(remaining))
            .putString(KEY_SELECTED_CREDENTIAL_ID, nextSelected?.id)
            .apply()
        return nextSelected
    }

    fun updateLastDesktopResponse(credentialId: String, epochMillis: Long) {
        val credentials = loadAll()
        if (credentials.none { it.id == credentialId }) return
        val updated = credentials.map { credential ->
            if (credential.id == credentialId) {
                credential.copy(lastDesktopResponseAtEpochMillis = epochMillis)
            } else {
                credential
            }
        }
        preferences.edit()
            .putString(KEY_CREDENTIALS_JSON, encodeStoredGatewayCredentials(updated))
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "willdeep_mobile_gateway"
        const val KEY_CREDENTIALS_JSON = "credentials_json"
        const val KEY_SELECTED_CREDENTIAL_ID = "selected_credential_id"
        const val KEY_BASE_URL = "base_url"
        const val KEY_FALLBACK_BASE_URLS = "fallback_base_urls"
        const val KEY_DEVICE_TOKEN = "device_token"
        const val KEY_DESKTOP_NAME = "desktop_name"
        const val KEY_PROTOCOL_VERSION = "protocol_version"
        const val KEY_RELAY_ROOM = "relay_room"

        fun createEncryptedPreferences(
            context: Context,
            masterKey: MasterKey,
        ): SharedPreferences {
            return try {
                openEncryptedPreferences(context, masterKey)
            } catch (_: GeneralSecurityException) {
                // A restored or stale encrypted preference file can no longer be authenticated
                // against this installation's Android Keystore key. Pairing data is recoverable,
                // so reset only this secure store instead of crashing app startup.
                context.deleteSharedPreferences(PREFERENCES_NAME)
                openEncryptedPreferences(context, masterKey)
            }
        }

        fun openEncryptedPreferences(
            context: Context,
            masterKey: MasterKey,
        ): SharedPreferences {
            return EncryptedSharedPreferences.create(
                context,
                PREFERENCES_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }
    }
}

internal fun encodeStoredGatewayCredentials(credentials: List<StoredGatewayCredential>): String {
    return JSONArray().also { array ->
        credentials.forEach { credential ->
            array.put(
                JSONObject()
                    .put("base_url", credential.baseUrl)
                    .put("fallback_base_urls", JSONArray(credential.fallbackBaseUrls))
                    .put("device_token", credential.deviceToken)
                    .put("desktop_name", credential.desktopName)
                    .put("protocol_version", credential.protocolVersion)
                    .put("relay_room", credential.relayRoom.orEmpty())
                    .put("last_desktop_response_at", credential.lastDesktopResponseAtEpochMillis)
            )
        }
    }.toString()
}

internal fun decodeStoredGatewayCredentials(raw: String): List<StoredGatewayCredential> {
    return runCatching {
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val json = array.optJSONObject(index) ?: continue
                val baseUrl = json.optString("base_url").trim()
                val deviceToken = json.optString("device_token").trim()
                if (baseUrl.isBlank() || deviceToken.isBlank()) continue
                add(
                    StoredGatewayCredential(
                        baseUrl = baseUrl,
                        fallbackBaseUrls = decodeFallbackBaseUrls(
                            json.optJSONArray("fallback_base_urls")?.toString(),
                        ),
                        deviceToken = deviceToken,
                        desktopName = json.optString("desktop_name", "WillDeep Mac"),
                        protocolVersion = json.optString(
                            "protocol_version",
                            MOBILE_GATEWAY_PROTOCOL_VERSION,
                        ),
                        relayRoom = json.optString("relay_room").takeIf { it.isNotBlank() },
                        lastDesktopResponseAtEpochMillis = json.optLong(
                            "last_desktop_response_at",
                            0L,
                        ),
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
}

private fun stableGatewayCredentialId(relayRoom: String?, baseUrl: String): String {
    val source = relayRoom
        ?.trim()
        ?.trim('/')
        ?.takeIf { it.isNotBlank() }
        ?.let { "relay:$it" }
        ?: "gateway:${baseUrl.trim().trimEnd('/')}"
    return MessageDigest.getInstance("SHA-256")
        .digest(source.toByteArray(Charsets.UTF_8))
        .take(12)
        .joinToString(separator = "") { byte -> "%02x".format(byte) }
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
