package com.example.athlo.vista.mapa

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.athlo.controlador.MapaController
import com.example.athlo.modelo.AppDatabase
import com.example.athlo.modelo.mapa.ResumenRutaViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

@SuppressLint("MissingPermission")
@Composable
fun PantallaMapa(navController: NavController, resumenViewModel: ResumenRutaViewModel) {
    val context = LocalContext.current
    val estadoEntreno = MapaController.estadoEntreno
    val entrenamientoId = remember { mutableStateOf<String?>(null) }
    val prefs = context.getSharedPreferences("entreno", 0)
    LaunchedEffect(Unit) {
        entrenamientoId.value = prefs.getString("id", null)
    }

    val segundos = remember { mutableLongStateOf(0L) }
    val distanciaMetros = remember { mutableFloatStateOf(0f) }
    val calorias = remember { mutableIntStateOf(0) }

    // Simulación de actualización dinámica
    LaunchedEffect(MapaController.estadoEntreno) {
        while (MapaController.estadoEntreno == MapaController.EstadoEntreno.Activo) {
            kotlinx.coroutines.delay(1000)

            segundos.longValue = MapaController.obtenerDuracion()
            distanciaMetros.floatValue = MapaController.obtenerDistanciaRecorrida()
            calorias.intValue = MapaController.obtenerCalorias()
        }
    }

    val mapLibreMap = remember { mutableStateOf<MapLibreMap?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Mapa
        AndroidView(
            factory = {
                MapView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    getMapAsync { map ->
                        mapLibreMap.value = map
                        map.setStyle("https://api.maptiler.com/maps/019644e0-aa3e-76fa-9d07-f2a7846f6f9a/style.json?key=Q9rFfJJB0EDEU0CRyJie") {
                            MapaController.activarUbicacionYCentra(context, map)
                            MapaController.activarSeguimientoPasivo(context, map)
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Botón historial
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    shape = CircleShape
                )
                .size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = { navController.navigate("historial_rutas") }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = "Ver historial",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        // Botón para centrar al usuario
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape)
                .size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = {
                mapLibreMap.value?.let { map ->
                    MapaController.centrarEnUbicacionActual(context, map)
                }
            }) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = "Centrar en ubicación",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }


        // PANEL INFERIOR CON DATOS Y BOTONES
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                // BOTONES DE CONTROL
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    when (estadoEntreno) {
                        MapaController.EstadoEntreno.Detenido, MapaController.EstadoEntreno.Pausado -> {
                            Button(
                                onClick = {
                                    if (estadoEntreno == MapaController.EstadoEntreno.Detenido) {
                                        val nuevoId = System.currentTimeMillis().toString()
                                        entrenamientoId.value = nuevoId
                                        MapaController.iniciarEntreno()
                                        MapaController.iniciarServicioSeguimiento(context, nuevoId)
                                        MapaController.detenerSeguimientoPasivo()
                                        MapaController.iniciarSeguimiento(context, nuevoId)
                                    } else {
                                        MapaController.pausarOReanudarEntreno()
                                    }
                                },
                                shape = CircleShape,
                                modifier = Modifier.size(72.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Iniciar", tint = Color.White)
                            }

                            if (estadoEntreno == MapaController.EstadoEntreno.Pausado) {
                                Spacer(modifier = Modifier.width(16.dp))
                                Button(
                                    onClick = {
                                        MapaController.detenerEntreno()
                                        MapaController.detenerServicioSeguimiento(context)
                                        MapaController.detenerSeguimiento()
                                        entrenamientoId.value?.let { id ->
                                            CoroutineScope(Dispatchers.IO).launch {
                                                val puntos = AppDatabase
                                                    .obtenerInstancia(context)
                                                    .puntoRutaDao()
                                                    .obtenerRuta(id)

                                                if (puntos.size < 2) {
                                                    withContext(Dispatchers.Main) {
                                                        Toast.makeText(
                                                            context,
                                                            "⚠️ El entrenamiento debe durar más tiempo.",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                    return@launch
                                                }

                                                MapaController.generarYSubirResumen(context, id)
                                                MapaController.sincronizarConFirebase(context)
                                                val resumen = MapaController.generarResumenLocal(context, id)

                                                withContext(Dispatchers.Main) {
                                                    resumenViewModel.resumen = resumen
                                                    resumenViewModel.puntosRuta = puntos
                                                    navController.navigate("resumen_ruta")
                                                }
                                            }
                                        }

                                        segundos.longValue = 0
                                    },
                                    shape = CircleShape,
                                    modifier = Modifier.size(64.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = "Detener", tint = Color.White)
                                }
                            }
                        }

                        MapaController.EstadoEntreno.Activo -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Button(
                                    onClick = { MapaController.pausarOReanudarEntreno() },
                                    shape = CircleShape,
                                    modifier = Modifier.size(64.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow)
                                ) {
                                    Icon(Icons.Default.Pause, contentDescription = "Pausar", tint = Color.Black)
                                }

                                Button(
                                    onClick = {
                                        MapaController.detenerEntreno()
                                        MapaController.detenerSeguimiento()
                                        entrenamientoId.value?.let { id ->
                                            CoroutineScope(Dispatchers.IO).launch {
                                                val puntos = AppDatabase
                                                    .obtenerInstancia(context)
                                                    .puntoRutaDao()
                                                    .obtenerRuta(id)

                                                if (puntos.size < 2) {
                                                    withContext(Dispatchers.Main) {
                                                        Toast.makeText(
                                                            context,
                                                            "⚠️ El entrenamiento debe durar más de 10 segundos.",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                    return@launch
                                                }

                                                MapaController.generarYSubirResumen(context, id)
                                                MapaController.sincronizarConFirebase(context)
                                                val resumen = MapaController.generarResumenLocal(context, id)

                                                withContext(Dispatchers.Main) {
                                                    resumenViewModel.resumen = resumen
                                                    resumenViewModel.puntosRuta = puntos
                                                    navController.navigate("resumen_ruta")
                                                }
                                            }
                                        }

                                        segundos.longValue = 0
                                    },
                                    shape = CircleShape,
                                    modifier = Modifier.size(64.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = "Detener", tint = Color.White)
                                }
                            }
                        }
                    }

                }

                Spacer(modifier = Modifier.height(16.dp))

                // DATOS DE ENTRENAMIENTO EN DOS FILAS
                val duracionMin = segundos.longValue / 60
                val duracionSeg = segundos.longValue % 60
                val velocidadMedia = if (segundos.longValue > 0) {
                    (distanciaMetros.floatValue / 1000f) / (segundos.longValue / 3600f)
                } else 0f

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("✏️ Distancia", style = MaterialTheme.typography.labelSmall)
                            Text("${"%.2f".format(distanciaMetros.floatValue / 1000)} km", style = MaterialTheme.typography.bodyLarge)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⏱️ Tiempo", style = MaterialTheme.typography.labelSmall)
                            Text("${duracionMin}m ${duracionSeg}s", style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🚀 Vel. media", style = MaterialTheme.typography.labelSmall)
                            Text("${"%.2f".format(velocidadMedia)} km/h", style = MaterialTheme.typography.bodyLarge)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔥 Calorías", style = MaterialTheme.typography.labelSmall)
                            Text("${calorias.intValue} kcal", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }

    }
}

