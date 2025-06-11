package com.example.athlo.vista.entreno

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.athlo.controlador.EntrenoController
import com.example.athlo.modelo.entreno.ResumenEntreno
import com.example.athlo.modelo.entreno.limpiarEstadoEntreno
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale


@SuppressLint("SimpleDateFormat")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaResumenEntreno(
    resumen: ResumenEntreno,
    onCancelar: () -> Unit,
    onGuardar: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📊 Resultados") },
                navigationIcon = {
                    IconButton(onClick = onCancelar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 📅 Fecha centrada
            Text(
                text = "📅 ${SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(resumen.fecha)}",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // ⏱️ Tiempo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⏱️ %02d:%02d".format(resumen.duracionSec / 60, resumen.duracionSec % 60),
                    style = MaterialTheme.typography.headlineLarge
                )
            }

            // Línea divisora
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 1.dp)

            // 🔥 Estadísticas: Calorías y Peso
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔥 Calorías", style = MaterialTheme.typography.labelSmall)
                    Text("${resumen.calorias} kcal", style = MaterialTheme.typography.titleLarge)
                }
                androidx.compose.material3.Divider(
                    modifier = Modifier
                        .height(40.dp)
                        .width(1.dp)
                        .align(Alignment.CenterVertically)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🧮 Peso total", style = MaterialTheme.typography.labelSmall)
                    Text("${resumen.pesoTotal} kg", style = MaterialTheme.typography.titleLarge)
                }
            }

            // Línea divisora
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 1.dp)

            // 🏋️ Ejercicios
            Text("🏋️ Ejercicios realizados", style = MaterialTheme.typography.titleMedium)

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(resumen.ejercicios) { ejercicio ->
                    var expanded by remember { mutableStateOf(false) }

                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.background),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(
                            Modifier
                                .clickable { expanded = !expanded }
                                .padding(16.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = ejercicio.nombre,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Icon(
                                    imageVector = if (expanded)
                                        Icons.Filled.KeyboardArrowUp
                                    else
                                        Icons.Filled.KeyboardArrowDown,
                                    contentDescription = null
                                )
                            }

                            if (expanded) {
                                Spacer(Modifier.height(8.dp))
                                ejercicio.sets.forEachIndexed { idx, set ->
                                    Text(
                                        "Serie ${idx + 1}: ${set.repeticiones} reps × ${set.peso} kg",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 💾 Botón guardar
            Button(
                onClick = {
                    EntrenoController.guardarResumenEntrenoUsuario(resumen)
                    scope.launch {
                        EntrenoController.guardarResumenEnLocal(context, resumen)
                    }
                    onGuardar()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("💾 Guardar Resumen")
            }
        }
    }
}


