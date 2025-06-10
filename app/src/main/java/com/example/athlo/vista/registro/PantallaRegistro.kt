// Pantalla de registro de usuario en Athlo
// Permite a nuevos usuarios registrarse con email y contraseña
// Valida campos, muestra errores y usa RegistroController para la lógica

@file:Suppress("DEPRECATION")

package com.example.athlo.vista.registro

// Imports necesarios para Jetpack Compose y recursos
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.athlo.R
import com.example.athlo.controlador.RegistroController
import com.example.athlo.ui.theme.AthloTheme

@Composable
fun PantallaRegistro(navController: NavController) {
    // Estados para los campos del formulario
    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var confirmarContrasena by remember { mutableStateOf("") }

    // Estados para mostrar/ocultar las contraseñas
    var contrasenaVisible by remember { mutableStateOf(false) }
    var confirmarContrasenaVisible by remember { mutableStateOf(false) }

    // Estados para errores en campos
    var errorCorreo by remember { mutableStateOf<String?>(null) }
    var errorContrasena by remember { mutableStateOf<String?>(null) }
    var errorConfirmacion by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    // Componente principal que ocupa toda la pantalla
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo de la app
            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "Logotipo de Athlo",
                modifier = Modifier
                    .size(240.dp)
                    .padding(bottom = 32.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
            )

            // Campo: nombre de usuario
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre de usuario") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo: correo electrónico
            OutlinedTextField(
                value = correo,
                onValueChange = {
                    correo = it
                    errorCorreo = null
                },
                label = { Text("Correo electrónico") },
                isError = errorCorreo != null,
                supportingText = {
                    if (errorCorreo != null) Text(errorCorreo!!, color = MaterialTheme.colorScheme.error)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo: contraseña
            OutlinedTextField(
                value = contrasena,
                onValueChange = {
                    contrasena = it
                    errorContrasena = null
                },
                label = { Text("Contraseña") },
                isError = errorContrasena != null,
                supportingText = {
                    if (errorContrasena != null) Text(errorContrasena!!, color = MaterialTheme.colorScheme.error)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (contrasenaVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val icon = if (contrasenaVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility
                    IconButton(onClick = { contrasenaVisible = !contrasenaVisible }) {
                        Icon(imageVector = icon, contentDescription = "Mostrar u ocultar contraseña")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo: confirmar contraseña
            OutlinedTextField(
                value = confirmarContrasena,
                onValueChange = {
                    confirmarContrasena = it
                    errorConfirmacion = null
                },
                label = { Text("Confirmar contraseña") },
                isError = errorConfirmacion != null,
                supportingText = {
                    if (errorConfirmacion != null) Text(errorConfirmacion!!, color = MaterialTheme.colorScheme.error)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (confirmarContrasenaVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val icon = if (confirmarContrasenaVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility
                    IconButton(onClick = { confirmarContrasenaVisible = !confirmarContrasenaVisible }) {
                        Icon(imageVector = icon, contentDescription = "Mostrar u ocultar confirmación")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Botón de registro: llama a RegistroController para manejar la lógica
            Button(
                onClick = {
                    RegistroController.registrarUsuario(
                        context = context,
                        navController = navController,
                        nombre = nombre,
                        correo = correo,
                        contrasena = contrasena,
                        confirmarContrasena = confirmarContrasena,
                        onErrores = { eCorreo, eContra, eConfirm ->
                            errorCorreo = eCorreo
                            errorContrasena = eContra
                            errorConfirmacion = eConfirm
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Registrarse")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón para volver a la pantalla de login
            TextButton(onClick = {
                navController.navigate("login")
            }) {
                Text("¿Ya tienes cuenta? Inicia sesión")
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun VistaPreviaPantallaRegistro() {
    AthloTheme {
        // PantallaRegistro() requiere NavController en ejecución real
    }
}
