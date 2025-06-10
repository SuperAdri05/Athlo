package com.example.athlo.vista

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.athlo.R
import com.example.athlo.controlador.DatosUsuarioController
import com.example.athlo.controlador.SesionController
import com.google.firebase.auth.FirebaseAuth

@Composable
fun PantallaDatosIniciales(navController: NavController) {
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser

    var peso by remember { mutableStateOf("") }
    var altura by remember { mutableStateOf("") }
    var edad by remember { mutableStateOf("") }
    var genero by remember { mutableStateOf("") }
    var objetivo by remember { mutableStateOf("") }

    var errorVisible by remember { mutableStateOf(false) }
    var guardando by remember { mutableStateOf(false) }

    val opcionesGenero = listOf("Hombre", "Mujer", "Otro")
    val opcionesObjetivo = listOf("Perder peso", "Mantener peso", "Ganar masa")

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_logo),
            contentDescription = "Logo Athlo",
            modifier = Modifier.size(120.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
        )

        Text("Completa tu perfil", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = peso,
            onValueChange = {
                if (it.matches(Regex("""\d{0,3}([.,]?\d*)?"""))) peso = it.take(5)
            },
            label = { Text("Peso (kg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )


        OutlinedTextField(
            value = altura,
            onValueChange = {
                if (it.all(Char::isDigit)) altura = it.take(3)
            },
            label = { Text("Altura (cm)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )


        OutlinedTextField(
            value = edad,
            onValueChange = {
                if (it.all(Char::isDigit)) edad = it.take(3)
            },
            label = { Text("Edad") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Género", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                opcionesGenero.forEach { opcion ->
                    AssistChip(
                        onClick = { genero = opcion },
                        label = { Text(text = opcion, modifier = Modifier.padding(horizontal = 4.dp)) },
                        shape = CircleShape,
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (genero == opcion) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = if (genero == opcion) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Objetivo", style = MaterialTheme.typography.titleMedium)
            opcionesObjetivo.forEach { opcion ->
                AssistChip(
                    onClick = { objetivo = opcion },
                    label = { Text(text = opcion, modifier = Modifier.padding(horizontal = 4.dp)) },
                    shape = CircleShape,
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (objetivo == opcion) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = if (objetivo == opcion) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }

        if (errorVisible) {
            Text("Todos los campos son obligatorios", color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = {
                if (peso.isBlank() || altura.isBlank() || edad.isBlank() || genero.isBlank() || objetivo.isBlank() || user == null) {
                    errorVisible = true
                    return@Button
                }

                guardando = true
                DatosUsuarioController.guardarDatosIniciales(
                    uid = user.uid,
                    peso = peso,
                    altura = altura,
                    edad = edad,
                    genero = genero,
                    objetivo = objetivo,
                    onSuccess = {
                        Toast.makeText(context, "Datos guardados", Toast.LENGTH_SHORT).show()
                        SesionController.comprobarRutaYRedirigir(context, navController)
                    },
                    onFailure = {
                        guardando = false
                        Toast.makeText(context, "Error al guardar los datos", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (guardando) "Guardando..." else "Guardar y continuar")
        }
    }
}
