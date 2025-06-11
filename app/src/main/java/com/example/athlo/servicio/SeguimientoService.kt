package com.example.athlo.servicio

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.example.athlo.R
import com.example.athlo.modelo.AppDatabase
import com.example.athlo.modelo.mapa.PuntoRuta
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SeguimientoService : LifecycleService() {
    companion object {
        const val ACTION_STOP = "com.example.athlo.STOP_SEGUIMIENTO"
    }
    private lateinit var locationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    override fun onCreate() {
        super.onCreate()
        locationClient = LocationServices.getFusedLocationProviderClient(this)
        iniciarNotificacion()
        iniciarSeguimiento()
    }

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }
        return super.onStartCommand(intent, flags, startId)
    }

    @SuppressLint("ForegroundServiceType")
    private fun iniciarNotificacion() {
        val canalId = "canal_seguimiento"
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(canalId, "Seguimiento activo", NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(canal)
        }

        val notif = NotificationCompat.Builder(this, canalId)
            .setContentTitle("Entrenamiento en curso")
            .setContentText("Tu carrera está siendo registrada")
            .setSmallIcon(R.drawable.ic_noti)
            .setOngoing(true)
            .build()

        startForeground(1, notif)
    }

    @SuppressLint("MissingPermission")
    private fun iniciarSeguimiento() {
        val id = getSharedPreferences("entreno", MODE_PRIVATE).getString("id", null) ?: return

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { loc ->
                    val punto = PuntoRuta(
                        entrenamientoId = id,
                        latitud = loc.latitude,
                        longitud = loc.longitude,
                        altitud = loc.altitude,
                        timestamp = System.currentTimeMillis()
                    )
                    CoroutineScope(Dispatchers.IO).launch {
                        AppDatabase.obtenerInstancia(this@SeguimientoService)
                            .puntoRutaDao()
                            .insertar(punto)
                    }
                }
            }
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000).build()
        locationClient.requestLocationUpdates(request, locationCallback!!, Looper.getMainLooper())
    }

    override fun onDestroy() {
        locationCallback?.let {
            locationClient.removeLocationUpdates(it)
        }
        stopForeground(true)
        super.onDestroy()
    }
}
