package com.example.athlo.vista.mapa

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.athlo.controlador.MapaController
import com.example.athlo.modelo.AppDatabase
import com.example.athlo.modelo.mapa.PuntoRuta
import com.example.athlo.modelo.mapa.RegistroRuta
import com.example.athlo.modelo.mapa.ResumenRutaViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PantallaHistorialRutas(navController: NavController, resumenViewModel: ResumenRutaViewModel) {
    val context = LocalContext.current
    var rutas by remember { mutableStateOf<List<RegistroRuta>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var idSeleccionado by remember { mutableStateOf<String?>(null) }

    // Cargar puntos al seleccionar una ruta
    LaunchedEffect(idSeleccionado) {
        idSeleccionado?.let { id ->
            val puntos = MapaController.obtenerPuntosRuta(context, id)
            resumenViewModel.puntosRuta = puntos
            resumenViewModel.resumen = rutas.find { it.id == id }
            navController.navigate("resumen_ruta")
            idSeleccionado = null
        }
    }



    // Cargar rutas desde Firebase
    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(uid)
                .collection("registros_ruta")
                .get()
                .await()

            rutas = snapshot.documents.mapNotNull { it.toObject(RegistroRuta::class.java) }
        }
        cargando = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Botón y título centrado
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        }

        Text(
            text = "📚 Historial",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (cargando) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (rutas.isEmpty()) {
            Text(
                "No hay rutas registradas.",
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                items(rutas.sortedByDescending { it.fecha }) { ruta ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { idSeleccionado = ruta.id }
                            .padding(horizontal = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(6.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 20.dp, horizontal = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = formatearFecha(ruta.fecha),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${"%.2f".format(ruta.distanciaMetros / 1000)} km • ${formatearTiempo(ruta.duracionSegundos)}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatearTiempo(segundos: Long): String {
    val min = segundos / 60
    val seg = segundos % 60
    return "${min}m ${seg}s"
}

private fun formatearFecha(millis: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(millis))
}

