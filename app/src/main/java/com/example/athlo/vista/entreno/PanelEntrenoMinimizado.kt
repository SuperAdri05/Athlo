package com.example.athlo.vista.entreno

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.athlo.modelo.entreno.Entrenamiento
import com.example.athlo.modelo.entreno.EntrenoViewModel
import com.example.athlo.modelo.entreno.limpiarEstadoEntreno
import com.example.athlo.servicio.EntrenoService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("SuspiciousIndentation")
@Composable
fun PanelEntrenoMinimizado(
    viewModel: EntrenoViewModel,
    navController: NavHostController,
    mostrarDialogo: MutableState<Entrenamiento?>
) {
    val tiempo by remember { derivedStateOf { viewModel.tiempoAcumulado } }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val showDialogCancel = remember { mutableStateOf(false) }

    if (viewModel.entrenamientoEnCurso == null || !viewModel.estaMinimizado) return

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable {
                    viewModel.estaMinimizado = false
                    navController.navigate("pantalla_ejecutar_entreno")
                }
                .padding(vertical = 16.dp, horizontal = 24.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {
                    viewModel.estaMinimizado = false
                    navController.navigate("pantalla_ejecutar_entreno")
                }) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Restaurar")
                }

                Text(
                    text = "%02d:%02d".format(tiempo / 60, tiempo % 60),
                    style = MaterialTheme.typography.titleMedium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                            .clickable {
                                viewModel.estaMinimizado = false
                                navController.navigate("pantalla_ejecutar_entreno")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Reanudar",
                            tint = Color.White
                        )
                    }

                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF44336))
                            .clickable {
                                showDialogCancel.value = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cancelar",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Diálogo de interrupción al intentar iniciar otro entreno
        mostrarDialogo.value?.let { nuevoEntreno ->
            AlertDialog(
                onDismissRequest = { mostrarDialogo.value = null },
                title = { Text("Ya tienes un entrenamiento en curso") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = {
                            viewModel.estaMinimizado = false
                            navController.navigate("pantalla_ejecutar_entreno")
                            mostrarDialogo.value = null
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Reanudar el anterior")
                        }
                        Button(onClick = {
                            viewModel.entrenamientoEnCurso = nuevoEntreno
                            viewModel.entrenamientoSeleccionado = nuevoEntreno
                            viewModel.tiempoAcumulado = 0
                            viewModel.estaMinimizado = false
                            navController.navigate("pantalla_ejecutar_entreno")
                            mostrarDialogo.value = null
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Cancelar anterior y empezar este")
                        }
                        OutlinedButton(onClick = {
                            mostrarDialogo.value = null
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Cancelar")
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {}
            )
        }

        // Diálogo de confirmación para cancelar entrenamiento
        if (showDialogCancel.value) {
            AlertDialog(
                onDismissRequest = { showDialogCancel.value = false },
                title = { Text("Cancelar entrenamiento") },
                text = { Text("¿Estás seguro de que deseas cancelar el entrenamiento actual? Se perderán los datos.") },
                confirmButton = {
                    Button(onClick = {
                        showDialogCancel.value = false
                        context.stopService(Intent(context, EntrenoService::class.java))
                        scope.launch {
                            limpiarEstadoEntreno(context)
                            viewModel.resetEntreno()
                            delay(100)
                            navController.navigate("entreno")
                        }
                    }) {
                        Text("Sí, cancelar")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showDialogCancel.value = false }) {
                        Text("No")
                    }
                }
            )
        }
    }
}
