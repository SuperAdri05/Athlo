package com.example.athlo.servicio

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.athlo.R

class EntrenoService : Service() {

    companion object {
        const val CHANNEL_ID = "entreno_channel"
        const val NOTIF_ID = 101
        const val EXTRA_TIEMPO = "tiempo"
    }

    override fun onCreate() {
        super.onCreate()
        crearCanalNotificaciones()
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val tiempo = intent?.getStringExtra(EXTRA_TIEMPO) ?: "00:00"

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Entrenamiento en curso")
            .setContentText("Tiempo transcurrido: $tiempo")
            .setSmallIcon(R.drawable.ic_noti)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        startForeground(NOTIF_ID, notification)

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopForeground(true)
        super.onDestroy()
    }

    private fun crearCanalNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CHANNEL_ID,
                "Notificación de entreno",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(canal)
        }
    }
}
