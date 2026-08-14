package com.jaminsmoke.personalcomander.data.sesion

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.jaminsmoke.personalcomander.MainActivity
import com.jaminsmoke.personalcomander.R

object RecogerNotificador {
    const val CANAL_ID = "recoger"

    fun asegurarCanal(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CANAL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CANAL_ID,
                context.getString(R.string.recoger_notif_channel),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.recoger_notif_channel_desc)
            },
        )
    }

    fun mostrar(context: Context, aviso: AvisoRecoger) {
        asegurarCanal(context)
        val pending = PendingIntent.getActivity(
            context,
            aviso.ticketId.hashCode(),
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notificacion = NotificationCompat.Builder(context, CANAL_ID)
            .setSmallIcon(R.drawable.ic_brand_shield)
            .setContentTitle(context.getString(R.string.recoger_notif_titulo))
            .setContentText(aviso.texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(aviso.texto))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try {
            NotificationManagerCompat.from(context)
                .notify(aviso.ticketId.hashCode(), notificacion)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS denegado (API 33+): el snackbar cubre el aviso
        }
    }
}
