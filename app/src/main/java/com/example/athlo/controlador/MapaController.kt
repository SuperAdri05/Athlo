package com.example.athlo.controlador

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.athlo.modelo.AppDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import android.os.Looper
import com.google.android.gms.location.*
import com.example.athlo.modelo.mapa.PuntoRuta
import com.example.athlo.modelo.mapa.RegistroRuta
import com.example.athlo.servicio.SeguimientoService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await


object MapaController {
    private var distanciaActual: Float = 0f
    private var tiempoInicio: Long = 0L
    private var tiempoUltimo: Long = 0L
    private var tiempoPausaAcumulado: Long = 0L
    private var puntosRuta: MutableList<PuntoRuta> = mutableListOf()

    private var mapLibreMap: MapLibreMap? = null

    enum class EstadoEntreno {
        Activo, Pausado, Detenido
    }

    var estadoEntreno by mutableStateOf(EstadoEntreno.Detenido)
        private set

    fun iniciarEntreno() {
        tiempoInicio = System.currentTimeMillis()
        tiempoUltimo = tiempoInicio
        tiempoPausaAcumulado = 0L
        estadoEntreno = EstadoEntreno.Activo
    }

    fun pausarOReanudarEntreno() {
        when (estadoEntreno) {
            EstadoEntreno.Activo -> {
                tiempoUltimo = System.currentTimeMillis()
                estadoEntreno = EstadoEntreno.Pausado
            }
            EstadoEntreno.Pausado -> {
                tiempoPausaAcumulado += System.currentTimeMillis() - tiempoUltimo
                estadoEntreno = EstadoEntreno.Activo
            }
            else -> {}
        }
    }

    fun detenerEntreno() {
        tiempoInicio = 0L
        tiempoPausaAcumulado = 0L
        tiempoUltimo = 0L
        estadoEntreno = EstadoEntreno.Detenido
    }

    @SuppressLint("MissingPermission")
    fun activarUbicacionYCentra(context: Context, map: MapLibreMap) {
        try {
            mapLibreMap = map // <- Guarda el mapa aquí

            val locationComponent = map.locationComponent
            locationComponent.activateLocationComponent(
                LocationComponentActivationOptions.builder(context, map.style!!)
                    .build()
            )
            locationComponent.isLocationComponentEnabled = true
            locationComponent.cameraMode = CameraMode.TRACKING
            locationComponent.renderMode = RenderMode.COMPASS

            val ubicacion: Location? = locationComponent.lastKnownLocation
            ubicacion?.let {
                centrarMapa(map, it)
            }

        } catch (e: Exception) {
            Log.e("MapaController", "Error al activar ubicación: ${e.message}")
        }
    }


    private fun centrarMapa(map: MapLibreMap, location: Location) {
        val posicion = CameraPosition.Builder()
            .target(LatLng(location.latitude, location.longitude))
            .zoom(16.0)
            .build()

        map.animateCamera(CameraUpdateFactory.newCameraPosition(posicion), 1000)
    }
    suspend fun sincronizarConFirebase(context: Context) {
        val dao = AppDatabase.obtenerInstancia(context).puntoRutaDao()
        val pendientes = dao.obtenerPendientes()
        val firestore = FirebaseFirestore.getInstance()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Agrupar los puntos por ID de entrenamiento
        pendientes.groupBy { it.entrenamientoId }.forEach { (entrenoId, puntos) ->

            val rutaRef = firestore.collection("usuarios")
                .document(uid)
                .collection("registros_ruta")
                .document(entrenoId)

            val puntosRef = rutaRef.collection("puntos_ruta")

            // Guardar cada punto como documento individual
            puntos.forEach { punto ->
                val puntoMap = mapOf(
                    "latitud" to punto.latitud,
                    "longitud" to punto.longitud,
                    "timestamp" to punto.timestamp
                )

                // Añadir documento al subgrupo
                puntosRef.add(puntoMap)
            }

            // Marcar como sincronizados en la BD local
            val ids = puntos.map { it.id }
            CoroutineScope(Dispatchers.IO).launch {
                dao.marcarComoSincronizados(ids)
            }

            Log.i("MapaController", "Subida completa para ruta $entrenoId")
        }
    }


    private var locationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun iniciarSeguimiento(context: Context, entrenamientoId: String) {
        if (estadoEntreno != EstadoEntreno.Activo) return

        // Reiniciar valores
        distanciaActual = 0f
        tiempoInicio = System.currentTimeMillis()
        puntosRuta.clear()

        locationClient = LocationServices.getFusedLocationProviderClient(context)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                if (estadoEntreno != EstadoEntreno.Activo) return
                val location = result.lastLocation ?: return
                val timestamp = System.currentTimeMillis()

                val punto = PuntoRuta(
                    entrenamientoId = entrenamientoId,
                    latitud = location.latitude,
                    longitud = location.longitude,
                    altitud = location.altitude,
                    timestamp = timestamp
                )

                CoroutineScope(Dispatchers.IO).launch {
                    val dao = AppDatabase.obtenerInstancia(context).puntoRutaDao()
                    dao.insertar(punto)
                }

                // Actualizar métricas
                if (puntosRuta.isNotEmpty()) {
                    val anterior = puntosRuta.last()
                    val distancia = calcularDistancia(
                        anterior.latitud, anterior.longitud,
                        punto.latitud, punto.longitud
                    )
                    distanciaActual += distancia
                }
                puntosRuta.add(punto)
                tiempoUltimo = timestamp

                // Actualizar cámara
                mapLibreMap?.let { map ->
                    val nuevaPos = LatLng(location.latitude, location.longitude)
                    map.animateCamera(CameraUpdateFactory.newLatLng(nuevaPos))
                }
            }
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 7000).build()
        locationClient?.requestLocationUpdates(request, locationCallback!!, Looper.getMainLooper())
    }


    fun detenerSeguimiento() {
        locationCallback?.let {
            locationClient?.removeLocationUpdates(it)
        }
        locationCallback = null
    }

    private fun subirResumenRuta(context: Context, registro: RegistroRuta) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            Log.e("MapaController", "No se pudo subir: usuario no autenticado")
            return
        }

        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("usuarios")
            .document(uid)
            .collection("registros_ruta")
            .document(registro.id)
            .set(registro)
            .addOnSuccessListener {
                Log.i("MapaController", "Resumen de ruta subido correctamente")
            }
            .addOnFailureListener {
                Log.e("MapaController", "Error al subir resumen de ruta: ${it.message}")
            }
    }

    suspend fun generarYSubirResumen(context: Context, entrenamientoId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val dao = AppDatabase.obtenerInstancia(context).puntoRutaDao()
        val puntos = dao.obtenerRuta(entrenamientoId)

        if (puntos.size < 2) return  // No hay datos suficientes

        val tiempoTotal = (puntos.last().timestamp - puntos.first().timestamp) / 1000L // segundos

        var distanciaTotal = 0f
        for (i in 1 until puntos.size) {
            val lat1 = puntos[i - 1].latitud
            val lon1 = puntos[i - 1].longitud
            val lat2 = puntos[i].latitud
            val lon2 = puntos[i].longitud
            distanciaTotal += calcularDistancia(lat1, lon1, lat2, lon2)
        }

        val caloriasEstimadas = calcularCalorias(distanciaTotal)

        val resumen = RegistroRuta(
            id = entrenamientoId,
            usuarioId = uid,
            duracionSegundos = tiempoTotal,
            distanciaMetros = distanciaTotal,
            caloriasEstimadas = caloriasEstimadas,
            fecha = System.currentTimeMillis()
        )

        subirResumenRuta(context, resumen)
    }
    private fun calcularDistancia(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val resultados = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, resultados)
        return resultados[0]
    }
    private fun calcularCalorias(distanciaMetros: Float): Int {
        // Aproximadamente 60 kcal por km caminando
        return (distanciaMetros / 1000f * 60).toInt()
    }

    suspend fun generarResumenLocal(context: Context, entrenamientoId: String): RegistroRuta {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "desconocido"
        val dao = AppDatabase.obtenerInstancia(context).puntoRutaDao()
        val puntos = dao.obtenerRuta(entrenamientoId)
        Log.d("DEBUG_RESUMEN", "Número de puntos recuperados: ${puntos.size}")


        val tiempo = (puntos.last().timestamp - puntos.first().timestamp) / 1000L
        var distancia = 0f
        for (i in 1 until puntos.size) {
            distancia += calcularDistancia(
                puntos[i - 1].latitud, puntos[i - 1].longitud,
                puntos[i].latitud, puntos[i].longitud
            )
        }
        val calorias = calcularCalorias(distancia)
        Log.d("DEBUG_RESUMEN", "Resumen: tiempo=${tiempo}s, distancia=${distancia}m, calorías=$calorias")

        return RegistroRuta(
            id = entrenamientoId,
            usuarioId = uid,
            duracionSegundos = tiempo,
            distanciaMetros = distancia,
            caloriasEstimadas = calorias
        )
    }

    fun obtenerDuracion(): Long {
        if (estadoEntreno == EstadoEntreno.Detenido || tiempoInicio == 0L) return 0L

        val ahora = when (estadoEntreno) {
            EstadoEntreno.Pausado -> tiempoUltimo
            else -> System.currentTimeMillis()
        }

        return ((ahora - tiempoInicio - tiempoPausaAcumulado) / 1000L)
    }

    fun obtenerDistanciaRecorrida(): Float = distanciaActual

    fun obtenerCalorias(): Int = calcularCalorias(distanciaActual)

    @SuppressLint("MissingPermission")
    fun centrarEnUbicacionActual(context: Context, map: MapLibreMap) {
        try {
            val location = map.locationComponent.lastKnownLocation
            location?.let {
                val posicion = CameraPosition.Builder()
                    .target(LatLng(it.latitude, it.longitude))
                    .zoom(16.0)
                    .build()
                map.animateCamera(CameraUpdateFactory.newCameraPosition(posicion), 1000)
            }
        } catch (e: Exception) {
            Log.e("MapaController", "Error al centrar: ${e.message}")
        }
    }

    suspend fun obtenerPuntosRuta(context: Context, idRuta: String): List<PuntoRuta> {
        val dao = AppDatabase.obtenerInstancia(context).puntoRutaDao()
        var puntos = dao.obtenerRuta(idRuta)

        if (puntos.size < 2) {
            // Intentar cargar desde Firebase
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return emptyList()

            val snapshot = FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(uid)
                .collection("registros_ruta")
                .document(idRuta)
                .collection("puntos_ruta")
                .get()
                .await()

            puntos = snapshot.documents.mapNotNull { doc ->
                val lat = doc.getDouble("latitud")
                val lon = doc.getDouble("longitud")
                val ts = doc.getLong("timestamp")
                if (lat != null && lon != null && ts != null) {
                    PuntoRuta(
                        entrenamientoId = idRuta,
                        latitud = lat,
                        longitud = lon,
                        timestamp = ts
                    )
                } else null
            }
        }

        return puntos
    }

    fun iniciarServicioSeguimiento(context: Context, id: String) {
        val prefs = context.getSharedPreferences("entreno", Context.MODE_PRIVATE)
        prefs.edit().putString("id", id).apply()

        val intent = Intent(context, SeguimientoService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun detenerServicioSeguimiento(context: Context) {
        context.stopService(Intent(context, SeguimientoService::class.java))
        context.getSharedPreferences("entreno", Context.MODE_PRIVATE).edit().clear().apply()
    }

    private var callbackMapa: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun activarSeguimientoPasivo(context: Context, map: MapLibreMap) {
        locationClient = LocationServices.getFusedLocationProviderClient(context)

        callbackMapa?.let {
            locationClient?.removeLocationUpdates(it)
        }

        callbackMapa = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                val pos = LatLng(location.latitude, location.longitude)
                map.animateCamera(CameraUpdateFactory.newLatLng(pos))
            }
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build()
        locationClient?.requestLocationUpdates(request, callbackMapa!!, Looper.getMainLooper())
    }

    fun detenerSeguimientoPasivo() {
        callbackMapa?.let {
            locationClient?.removeLocationUpdates(it)
        }
        callbackMapa = null
    }


}
