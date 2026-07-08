package com.willdeep.android

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.willdeep.android.mobile.PatchProposal
import com.willdeep.android.mobile.PendingToolApproval

class MobileAttentionNotifier(
    private val context: Context,
) {
    fun showToolApproval(approval: PendingToolApproval) {
        if (!canPostNotifications()) return
        ensureChannel()

        val needsInput = approval.requiresAnswer || approval.requiresConfirmation
        val titleRes = if (needsInput) {
            R.string.notification_attention_input_title
        } else {
            R.string.notification_attention_approval_title
        }
        val body = approval.summary.ifBlank {
            approval.inputPreview.ifBlank {
                context.getString(R.string.notification_attention_open_hint)
            }
        }
        val builder = baseBuilder(
            targetType = MobileAttentionActions.TARGET_TOOL,
            targetId = approval.id,
            sessionId = approval.sessionId,
            title = context.getString(titleRes),
            body = body,
        )
            .setSubText(approval.title.ifBlank { approval.toolName })

        if (!needsInput) {
            builder.addAction(
                R.drawable.ic_approval,
                context.getString(R.string.approve_button),
                actionIntent(
                    action = MobileAttentionActions.ACTION_APPROVE_ATTENTION,
                    targetType = MobileAttentionActions.TARGET_TOOL,
                    targetId = approval.id,
                    sessionId = approval.sessionId,
                ),
            )
            builder.addAction(
                R.drawable.ic_close,
                context.getString(R.string.reject_button),
                actionIntent(
                    action = MobileAttentionActions.ACTION_REJECT_ATTENTION,
                    targetType = MobileAttentionActions.TARGET_TOOL,
                    targetId = approval.id,
                    sessionId = approval.sessionId,
                ),
            )
        }

        notify(MobileAttentionActions.TARGET_TOOL, approval.id, builder)
    }

    fun showPatchProposal(proposal: PatchProposal) {
        if (!canPostNotifications()) return
        ensureChannel()

        val body = proposal.summary.ifBlank {
            proposal.stats.ifBlank {
                proposal.path.ifBlank {
                    context.getString(R.string.notification_attention_open_hint)
                }
            }
        }
        val builder = baseBuilder(
            targetType = MobileAttentionActions.TARGET_PATCH,
            targetId = proposal.id,
            sessionId = proposal.sessionId,
            title = context.getString(R.string.notification_attention_patch_title),
            body = body,
        )
            .setSubText(proposal.title.ifBlank { proposal.path })
            .addAction(
                R.drawable.ic_approval,
                context.getString(R.string.approve_button),
                actionIntent(
                    action = MobileAttentionActions.ACTION_APPROVE_ATTENTION,
                    targetType = MobileAttentionActions.TARGET_PATCH,
                    targetId = proposal.id,
                    sessionId = proposal.sessionId,
                ),
            )
            .addAction(
                R.drawable.ic_close,
                context.getString(R.string.reject_button),
                actionIntent(
                    action = MobileAttentionActions.ACTION_REJECT_ATTENTION,
                    targetType = MobileAttentionActions.TARGET_PATCH,
                    targetId = proposal.id,
                    sessionId = proposal.sessionId,
                ),
            )

        notify(MobileAttentionActions.TARGET_PATCH, proposal.id, builder)
    }

    fun cancelToolApproval(approvalId: String) {
        cancel(MobileAttentionActions.TARGET_TOOL, approvalId)
    }

    fun cancelPatchProposal(patchId: String) {
        cancel(MobileAttentionActions.TARGET_PATCH, patchId)
    }

    private fun baseBuilder(
        targetType: String,
        targetId: String,
        sessionId: String?,
        title: String,
        body: String,
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_ATTENTION)
            .setSmallIcon(R.drawable.ic_approval)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(
                actionIntent(
                    action = MobileAttentionActions.ACTION_OPEN_ATTENTION,
                    targetType = targetType,
                    targetId = targetId,
                    sessionId = sessionId,
                )
            )
    }

    private fun actionIntent(
        action: String,
        targetType: String,
        targetId: String,
        sessionId: String?,
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(action)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(MobileAttentionActions.EXTRA_TARGET_TYPE, targetType)
            .putExtra(MobileAttentionActions.EXTRA_TARGET_ID, targetId)
        if (!sessionId.isNullOrBlank()) {
            intent.putExtra(MobileAttentionActions.EXTRA_SESSION_ID, sessionId)
        }
        return PendingIntent.getActivity(
            context,
            requestCode(action, targetType, targetId, sessionId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ATTENTION,
            context.getString(R.string.notification_attention_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_attention_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun notify(
        targetType: String,
        targetId: String,
        builder: NotificationCompat.Builder,
    ) {
        NotificationManagerCompat.from(context).notify(notificationId(targetType, targetId), builder.build())
    }

    private fun cancel(targetType: String, targetId: String) {
        NotificationManagerCompat.from(context).cancel(notificationId(targetType, targetId))
    }

    private fun notificationId(targetType: String, targetId: String): Int {
        return "willdeep-attention:$targetType:$targetId".hashCode() and Int.MAX_VALUE
    }

    private fun requestCode(
        action: String,
        targetType: String,
        targetId: String,
        sessionId: String?,
    ): Int {
        return "willdeep-attention:$action:$targetType:$targetId:${sessionId.orEmpty()}".hashCode() and Int.MAX_VALUE
    }

    private companion object {
        const val CHANNEL_ATTENTION = "willdeep_attention"
    }
}
