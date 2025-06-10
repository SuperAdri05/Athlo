package com.example.athlo.vista.registro

import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.athlo.controlador.SesionController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit
import kotlin.math.min

@SuppressLint("DefaultLocale")
@Composable
fun PantallaVerificacionCorreo(navController: NavController) {
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser

    var mensajeEnviado by remember { mutableStateOf(false) }
    var intentosReenvio by remember { mutableIntStateOf(0) }
    var tiempoRestante by remember { mutableLongStateOf(0L) }

    // Enviar correo automáticamente al cargar pantalla
    LaunchedEffect(Unit) {
        Log.d("Verificacion", "Enviando correo de verificación inicial")
        user?.sendEmailVerification()
            ?.addOnSuccessListener {
                mensajeEnviado = true
                Log.d("Verificacion", "Correo de verificación enviado")
            }
            ?.addOnFailureListener {
                Toast.makeText(context, "Error al enviar verificación", Toast.LENGTH_SHORT).show()
                Log.e("Verificacion", "Fallo al enviar correo: ${it.message}")
            }
    }

    // Temporizador para reenviar
    LaunchedEffect(intentosReenvio) {
        if (intentosReenvio > 0) {
            val espera = min(60L * (intentosReenvio), 300L)
            tiempoRestante = espera
            Log.d("Verificacion", "Temporizador iniciado: $espera segundos")
            while (tiempoRestante > 0) {
                delay(1000)
                tiempoRestante--
            }
        }
    }

    // Formato de cuenta atrás
    val tiempoFormateado = String.format(
        "%02d:%02d",
        TimeUnit.SECONDS.toMinutes(tiempoRestante),
        tiempoRestante % 60
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Verifica tu correo", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Te hemos enviado un correo de verificación. Verifica tu dirección y luego presiona el botón de abajo.")

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            Log.d("Verificacion", "Botón presionado: comprobando verificación")
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Log.e("Verificacion", "Usuario actual es null")
                Toast.makeText(context, "Error: Usuario no encontrado", Toast.LENGTH_SHORT).show()
                return@Button
            }
            currentUser.reload().addOnCompleteListener { reloadTask ->
                if (reloadTask.isSuccessful) {
                    val verificado = currentUser.isEmailVerified
                    Log.d("Verificacion", "Estado de verificación: $verificado")
                    if (verificado) {
                        Toast.makeText(context, "Correo verificado", Toast.LENGTH_SHORT).show()
                        SesionController.comprobarRutaYRedirigir(context, navController)
                    }

                    else {
                        Toast.makeText(context, "Aún no has verificado el correo", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("Verificacion", "Error en reload(): ${reloadTask.exception?.message}")
                    Toast.makeText(context, "Error al comprobar verificación", Toast.LENGTH_SHORT).show()
                }
            }
        }) {
            Text("He verificado mi correo")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!mensajeEnviado) {
            CircularProgressIndicator()
        } else {
            Text("Correo enviado. Revisa tu bandeja de entrada.")
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (tiempoRestante > 0) {
            Text("Puedes reenviar en: $tiempoFormateado")
        }

        TextButton(
            onClick = {
                user?.sendEmailVerification()
                Toast.makeText(context, "Correo reenviado", Toast.LENGTH_SHORT).show()
                intentosReenvio++
                Log.d("Verificacion", "Correo reenviado. Intentos: $intentosReenvio")
            },
            enabled = tiempoRestante == 0L
        ) {
            Text("Reenviar correo de verificación")
        }

        TextButton(onClick = {
            FirebaseAuth.getInstance().signOut()
            navController.navigate("inicio") {
                popUpTo("verificar") { inclusive = true }
            }

        }) {
            Text("Cancelar")
        }
    }
}
