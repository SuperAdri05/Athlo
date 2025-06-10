package com.example.athlo.controlador

import android.content.Context
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object SesionController {

    fun obtenerRutaInicio(context: Context, callback: (String) -> Unit) {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        if (user == null) {
            callback("login")
            return
        }

        if (!user.isEmailVerified) {
            callback("verificar")
            return
        }

        // Verificamos en Firestore si completó sus datos
        FirebaseFirestore.getInstance()
            .collection("usuarios")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                val datosCompletos = document.getBoolean("datosCompletos") ?: false
                if (datosCompletos) {
                    callback("inicio")
                } else {
                    callback("datos_iniciales")
                }
            }
            .addOnFailureListener {
                callback("inicio") // por seguridad
            }
    }
    fun comprobarRutaYRedirigir(context: Context, navController: NavController) {
        obtenerRutaInicio(context) { ruta ->
            navController.navigate(ruta) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

}
