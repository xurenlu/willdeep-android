package com.willdeep.android.push

import android.content.Context
import android.content.Intent
import android.util.Log
import com.umeng.commonsdk.UMConfigure
import com.umeng.message.PushAgent
import com.umeng.message.UmengMessageHandler
import com.umeng.message.UmengNotificationClickHandler
import com.umeng.message.api.UPushRegisterCallback
import com.umeng.message.entity.UMessage
import com.willdeep.android.BuildConfig

object MobilePushManager {
    private const val TAG = "MobilePush"
    const val ACTION_CLIENT_ID_UPDATED = "com.willdeep.android.push.CLIENT_ID_UPDATED"
    const val EXTRA_PROVIDER = "provider"
    const val EXTRA_CLIENT_ID = "client_id"
    const val PROVIDER_UMENG = "umeng"

    fun preInit(context: Context) {
        if (!isUmengConfigured()) return
        runCatching {
            UMConfigure.preInit(
                context.applicationContext,
                BuildConfig.UMENG_APPKEY,
                BuildConfig.UMENG_CHANNEL,
            )
        }.onFailure { error ->
            Log.w(TAG, "Umeng preInit failed: ${error.message}")
        }
    }

    fun register(context: Context) {
        if (!isUmengConfigured()) {
            Log.i(TAG, "Umeng push is disabled or UMENG_APPKEY/UMENG_MESSAGE_SECRET is blank.")
            return
        }
        val appContext = context.applicationContext
        runCatching {
            UMConfigure.setLogEnabled(BuildConfig.DEBUG)
            UMConfigure.init(
                appContext,
                BuildConfig.UMENG_APPKEY,
                BuildConfig.UMENG_CHANNEL,
                UMConfigure.DEVICE_TYPE_PHONE,
                BuildConfig.UMENG_MESSAGE_SECRET,
            )
            val agent = PushAgent.getInstance(appContext)
            agent.setMessageHandler(WillDeepUmengMessageHandler())
            agent.setNotificationClickHandler(WillDeepUmengClickHandler())
            agent.register(object : UPushRegisterCallback {
                override fun onSuccess(deviceToken: String) {
                    saveDeviceToken(appContext, deviceToken)
                    Log.i(TAG, "Umeng device token received.")
                }

                override fun onFailure(errorCode: String, errorMessage: String) {
                    Log.w(TAG, "Umeng register failed: $errorCode $errorMessage")
                }
            })
        }.onFailure { error ->
            Log.w(TAG, "Umeng initialize failed: ${error.message}")
        }
    }

    fun refreshStoredClientId(context: Context): String? {
        if (!isUmengConfigured()) return null
        val clientId = PushAgent.getInstance(context.applicationContext).registrationId.trim()
        if (clientId.isBlank()) return null
        saveDeviceToken(context.applicationContext, clientId)
        return clientId
    }

    fun isUmengConfigured(): Boolean {
        return BuildConfig.UMENG_PUSH_ENABLED &&
            BuildConfig.UMENG_APPKEY.isNotBlank() &&
            BuildConfig.UMENG_MESSAGE_SECRET.isNotBlank()
    }

    private fun saveDeviceToken(context: Context, deviceToken: String) {
        val token = deviceToken.trim()
        if (token.isBlank()) return
        MobilePushTokenStore(context).save(
            MobilePushToken(
                provider = PROVIDER_UMENG,
                clientId = token,
                appId = BuildConfig.UMENG_APPKEY,
            )
        )
        context.sendBroadcast(
            Intent(ACTION_CLIENT_ID_UPDATED)
                .setPackage(context.packageName)
                .putExtra(EXTRA_PROVIDER, PROVIDER_UMENG)
                .putExtra(EXTRA_CLIENT_ID, token)
        )
    }

    private class WillDeepUmengMessageHandler : UmengMessageHandler() {
        override fun dealWithCustomMessage(context: Context, msg: UMessage) {
            val payload = msg.custom?.trim().orEmpty()
            if (payload.isBlank()) {
                super.dealWithCustomMessage(context, msg)
                return
            }
            val handled = MobileRemotePushHandler.handleUmengPayload(context, payload)
            if (!handled) {
                super.dealWithCustomMessage(context, msg)
            }
        }
    }

    private class WillDeepUmengClickHandler : UmengNotificationClickHandler() {
        override fun dealWithCustomAction(context: Context, msg: UMessage) {
            val payload = msg.custom?.trim().orEmpty()
            if (payload.isNotBlank() && MobileRemotePushHandler.handleUmengPayload(context, payload)) {
                return
            }
            super.dealWithCustomAction(context, msg)
        }
    }
}
