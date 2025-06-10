// Pantalla de login de la app Athlo
// Gestiona inicio de sesión con correo/contraseña, Google y recuperación de cuenta
// Incluye opción para recordar usuario y mantener sesión activa con SharedPreferences

@file:Suppress("DEPRECATION")

package com.example.athlo.vista.registro

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.athlo.controlador.LoginController
import com.example.athlo.ui.theme.AthloTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun PantallaLogin(navController: NavController) {

    val context = LocalContext.current

    // Preferencias compartidas para guardar si se debe recordar la sesión
    val sharedPref = context.getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)
    val savedCorreo = sharedPref.getString("correo", "") ?: ""
    val recordarSesionGuardada = sharedPref.getBoolean("recordarSesion", false)

    // Estados que controlan los campos de entrada
    var correo by remember { mutableStateOf(savedCorreo) }
    var contrasena by remember { mutableStateOf("") }
    var contrasenaVisible by remember { mutableStateOf(false) }
    var recordarUsuario by remember { mutableStateOf(savedCorreo.isNotBlank() || recordarSesionGuardada) }

    // Estados para mostrar errores en los campos
    var errorCorreo by remember { mutableStateOf<String?>(null) }
    var errorContrasena by remember { mutableStateOf<String?>(null) }

    // Lanza el intent de login con Google y maneja la respuesta
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            LoginController.loginConGoogle(context, account, navController)
        } catch (e: ApiException) {
            errorCorreo = "Fallo Google Sign-In: ${e.message}"
        }
    }

    // Configuración de Google Sign-In
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(context.getString(R.string.default_web_client_id))
        .requestEmail()
        .build()
    val googleSignInClient = GoogleSignIn.getClient(context, gso)

    // Contenedor general de la pantalla
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

            // Campo para correo
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

            // Campo para contraseña con opción de mostrar u ocultar
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

            // Checkbox para recordar usuario
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = recordarUsuario,
                    onCheckedChange = { recordarUsuario = it }
                )
                Text("Recordar usuario")
            }

            // Botón de recuperación de contraseña
            TextButton(
                onClick = {
                    if (correo.isNotBlank()) {
                        LoginController.enviarCorreoRecuperacion(context, correo)
                    } else {
                        errorCorreo = "Introduce tu correo para recuperar la contraseña"
                    }
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("¿Olvidaste tu contraseña?")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón para iniciar sesión con correo y contraseña
            Button(
                onClick = {
                    var error = false
                    if (correo.isBlank()) {
                        errorCorreo = "El correo no puede estar vacío"
                        error = true
                    }
                    if (contrasena.isBlank()) {
                        errorContrasena = "La contraseña no puede estar vacía"
                        error = true
                    }

                    if (!error) {
                        LoginController.loginConCorreo(
                            context = context,
                            correo = correo,
                            contrasena = contrasena,
                            recordarUsuario = recordarUsuario,
                            navController = navController,
                            onError = { errCorreo, errContrasena ->
                                errorCorreo = errCorreo
                                errorContrasena = errContrasena
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Iniciar sesión")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botón para iniciar sesión con Google
            OutlinedButton(
                onClick = {
                    val signInIntent = googleSignInClient.signInIntent
                    launcher.launch(signInIntent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_google),
                    contentDescription = "Google",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Iniciar sesión con Google")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navegación hacia la pantalla de registro
            TextButton(onClick = {
                navController.navigate("registro")
            }) {
                Text("¿No tienes cuenta? Regístrate")
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun VistaPreviaPantallaLogin() {
    AthloTheme {
        // No se puede previsualizar el navController en tiempo de diseño
    }
}
