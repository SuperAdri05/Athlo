package com.example.athlo.controlador

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.ktx.userProfileChangeRequest

// Este controlador se encarga de la lógica de registro de nuevos usuarios
object RegistroController {

    /**
     * Registra un nuevo usuario con nombre, correo y contraseña.
     * Valida los datos localmente y maneja errores.
     * @param onErrores callback para mostrar errores en la vista
     */
    fun registrarUsuario(
        context: Context,
        navController: NavController,
        nombre: String,
        correo: String,
        contrasena: String,
        confirmarContrasena: String,
        onErrores: (correoError: String?, contrasenaError: String?, confirmacionError: String?) -> Unit
    ) {
        val auth = FirebaseAuth.getInstance()

        var errorCorreo: String? = null
        var errorContrasena: String? = null
        var errorConfirmacion: String? = null
        var hayError = false

        if (correo.isBlank()) {
            errorCorreo = "El correo no puede estar vacío"
            hayError = true
        }
        if (contrasena.isBlank()) {
            errorContrasena = "La contraseña no puede estar vacía"
            hayError = true
        }
        if (confirmarContrasena.isBlank()) {
            errorConfirmacion = "Debes confirmar la contraseña"
            hayError = true
        }
        if (contrasena != confirmarContrasena) {
            errorConfirmacion = "Las contraseñas no coinciden"
            hayError = true
        }

        if (hayError) {
            onErrores(errorCorreo, errorContrasena, errorConfirmacion)
            return
        }

        auth.createUserWithEmailAndPassword(correo, contrasena)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val profileUpdates = userProfileChangeRequest {
                        displayName = nombre
                    }
                    user?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                user.reload().addOnSuccessListener {
                                    navController.navigate("verificar") {
                                        popUpTo("registro") { inclusive = true }
                                    }
                                }
                            }
                            else {
                            Toast.makeText(context, "Error al actualizar el perfil: ${updateTask.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    val exception = task.exception
                    if (exception is FirebaseAuthUserCollisionException) {
                        onErrores("Este correo ya está registrado", null, null)
                    } else {
                        Toast.makeText(context, "Error: ${exception?.message}", Toast.LENGTH_LONG).show()
                        Log.e("Registro", "Error al registrar", exception)
                    }
                }
            }
    }

}

