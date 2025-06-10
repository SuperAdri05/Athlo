package com.example.athlo.vista.entreno

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.athlo.controlador.EntrenoController
import com.example.athlo.modelo.entreno.Entrenamiento
import com.example.athlo.modelo.entreno.EntrenoViewModel
import com.example.athlo.modelo.entidades.EntrenamientoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@SuppressLint("RememberReturnType", "UseOfNonLambdaOffsetOverload")
@Composable
fun shakeOffset(trigger: Boolean): Modifier {
    val offsetX = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        if (trigger) {
            offsetX.snapTo(0f)
            repeat(3) {
                offsetX.animateTo(
                    targetValue = 12f,
                    animationSpec = tween(durationMillis = 50)
                )
                offsetX.animateTo(
                    targetValue = -12f,
                    animationSpec = tween(durationMillis = 50)
                )
            }
            offsetX.animateTo(0f)
        }
    }

    return Modifier.offset(x = offsetX.value.dp)
}



@Composable
fun PantallaCrearEntreno(
    viewModel: EntrenoViewModel = viewModel(),
    onContinuar: () -> Unit,
    onCancelar: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var nombre by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var nivelSeleccionado by remember { mutableStateOf("Intermedio") }
    var duracion by remember { mutableStateOf("") }

    var errorNombre by remember { mutableStateOf(false) }
    var errorDescripcion by remember { mutableStateOf(false) }
    var errorDuracion by remember { mutableStateOf(false) }

    val niveles = listOf("Principiante", "Intermedio", "Avanzado")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Nuevo Entrenamiento", style = MaterialTheme.typography.headlineLarge)

        val maxNombre = 20
        val maxDescripcion = 100
        val maxDuracion = 3

        OutlinedTextField(
            value = nombre,
            onValueChange = {
                if (it.length <= maxNombre) {
                    nombre = it
                    errorNombre = false
                }
            },
            isError = errorNombre,
            label = { Text("Nombre del entrenamiento") },
            leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
            supportingText = {
                if (errorNombre) Text("Máximo $maxNombre caracteres", color = MaterialTheme.colorScheme.error)
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .then(shakeOffset(errorNombre))
        )

        OutlinedTextField(
            value = descripcion,
            onValueChange = {
                if (it.length <= maxDescripcion) {
                    descripcion = it
                    errorDescripcion = false
                }
            },
            isError = errorDescripcion,
            label = { Text("Descripción") },
            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
            supportingText = {
                if (errorDescripcion) Text("Máximo $maxDescripcion caracteres", color = MaterialTheme.colorScheme.error)
            },
            modifier = Modifier
                .fillMaxWidth()
                .then(shakeOffset(errorDescripcion))
        )

        OutlinedTextField(
            value = duracion,
            onValueChange = {
                if (it.length <= maxDuracion && it.all { char -> char.isDigit() }) {
                    duracion = it
                    errorDuracion = false
                }
            },
            isError = errorDuracion,
            label = { Text("Duración (min)") },
            leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
            supportingText = {
                if (errorDuracion) Text("Máximo $maxDuracion dígitos numéricos", color = MaterialTheme.colorScheme.error)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .then(shakeOffset(errorDuracion))
        )


        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
            Text("Nivel", fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                niveles.forEach { nivel ->
                    AssistChip(
                        onClick = { nivelSeleccionado = nivel },
                        label = { Text(nivel) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (nivelSeleccionado == nivel)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = if (nivelSeleccionado == nivel)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    errorNombre = nombre.isBlank()
                    errorDescripcion = descripcion.isBlank()
                    errorDuracion = duracion.isBlank() || duracion.toIntOrNull() == null

                    if (errorNombre || errorDescripcion || errorDuracion) return@Button

                    val entrenamiento = Entrenamiento(
                        id = System.currentTimeMillis().toString(),
                        nombre = nombre,
                        descripcion = descripcion,
                        nivel = nivelSeleccionado,
                        duracionMin = duracion.toInt()
                    )

                    viewModel.entrenamientoActual = entrenamiento
                    viewModel.entrenamientoSeleccionado = entrenamiento
                    EntrenoController.init(context)

                    val entrenamientoEntity = EntrenamientoEntity(
                        id = entrenamiento.id,
                        nombre = entrenamiento.nombre,
                        descripcion = entrenamiento.descripcion,
                        nivel = entrenamiento.nivel,
                        duracionMin = entrenamiento.duracionMin
                    )

                    coroutineScope.launch(Dispatchers.IO) {
                        EntrenoController.guardarEntrenamientoCompleto(
                            entrenamientoEntity,
                            emptyList()
                        )
                    }

                    onContinuar()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Continuar")
            }

            Spacer(modifier = Modifier.width(16.dp))

            OutlinedButton(
                onClick = onCancelar,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancelar")
            }
        }
    }
}

