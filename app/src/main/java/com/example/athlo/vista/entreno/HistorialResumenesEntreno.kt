package com.example.athlo.vista.entreno

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.athlo.controlador.EntrenoController
import com.example.athlo.modelo.entreno.EntrenoViewModel
import com.example.athlo.modelo.entreno.ResumenConNombre
import com.example.athlo.modelo.entreno.ResumenEntreno
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat

@SuppressLint("SimpleDateFormat")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialResumenesEntreno(
    navController: NavHostController,
    viewModel: EntrenoViewModel
) {
    val context = LocalContext.current
    var listaResumenes by remember { mutableStateOf<List<ResumenConNombre>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        cargando = true
        val resumenes = EntrenoController.obtenerTodosLosResumenes(context)
        val entrenamientosLocales = EntrenoController.obtenerEntrenamientos()

        val lista = withContext(Dispatchers.IO) {
            resumenes.map { resumen ->
                // Log de depuración
                println("🧪 Buscando nombre para entrenamientoId: ${resumen.entrenamientoId}")

                val nombreLocal = entrenamientosLocales.find { it.id == resumen.entrenamientoId }?.nombre
                val nombreFinal = nombreLocal ?: EntrenoController.obtenerNombreDeEntrenamiento(resumen.entrenamientoId)

                println("✅ Resultado: $nombreFinal")

                ResumenConNombre(resumen, nombreFinal)
            }.sortedByDescending { it.resumen.fecha }
        }

        listaResumenes = lista
        cargando = false
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📚 Historial") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { inner ->
        Box(
            modifier = Modifier
                .padding(inner)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            when {
                cargando -> {
                    CircularProgressIndicator()
                }

                listaResumenes.isEmpty() -> {
                    Text(
                        text = "No hay entrenamientos realizados aún.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    LazyColumn(modifier = Modifier.padding(16.dp)) {
                        items(listaResumenes) { item ->
                            val resumen = item.resumen
                            val nombre = item.nombreEntreno

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clickable {
                                        viewModel.resumenActual = resumen
                                        navController.navigate("pantalla_resumen_entreno")
                                    },
                                elevation = CardDefaults.cardElevation(4.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "🏋️ $nombre",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "🗓 ${SimpleDateFormat("dd/MM/yyyy").format(resumen.fecha)}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "⏱ ${resumen.duracionSec / 60}m ${resumen.duracionSec % 60}s · 🔥 ${resumen.calorias} kcal · 🏋️ ${resumen.pesoTotal} kg",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                    }
                }
            }
        }
    }
}
