package com.example.athlo.controlador

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.example.athlo.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

object PerfilController {

    fun obtenerUsuario(): FirebaseUser? = FirebaseAuth.getInstance().currentUser

    fun cerrarSesion(context: Context, onLogout: () -> Unit) {
        FirebaseAuth.getInstance().signOut()
        Toast.makeText(context, "Sesión cerrada", Toast.LENGTH_SHORT).show()
        onLogout()
    }

    fun actualizarNombre(
        context: Context,
        usuario: FirebaseUser?,
        nuevoNombre: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        usuario?.updateProfile(userProfileChangeRequest {
            displayName = nuevoNombre
        })?.addOnSuccessListener {
            Toast.makeText(context, "Nombre actualizado", Toast.LENGTH_SHORT).show()
            onSuccess()
        }?.addOnFailureListener {
            Toast.makeText(context, "Error al actualizar nombre", Toast.LENGTH_SHORT).show()
            onFailure()
        }
    }

    fun asignarNombreSiNoTiene(context: Context, usuario: FirebaseUser?, nombreEditable: String) {
        if (usuario != null && usuario.displayName.isNullOrBlank()) {
            val profileUpdates = userProfileChangeRequest {
                displayName = nombreEditable
            }
            usuario.updateProfile(profileUpdates)
                .addOnSuccessListener {
                    Toast.makeText(context, "Nombre asignado automáticamente", Toast.LENGTH_SHORT).show()
                }
        }
    }

    fun actualizarAvatar(
        context: Context,
        usuario: FirebaseUser?,
        avatar: Int,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val avatarString = when (avatar) {
            R.drawable.avatar_perro -> "avatar_perro"
            R.drawable.avatar_mono -> "avatar_mono"
            R.drawable.avatar_conejo -> "avatar_conejo"
            R.drawable.avatar_oso -> "avatar_oso"
            R.drawable.avatar_zorro -> "avatar_zorro"
            else -> "avatar_gato"
        }

        usuario?.updateProfile(userProfileChangeRequest {
            photoUri = Uri.parse(avatarString)
        })?.addOnSuccessListener {
            Toast.makeText(context, "Avatar actualizado", Toast.LENGTH_SHORT).show()
            onSuccess()
        }?.addOnFailureListener {
            Toast.makeText(context, "Error al actualizar avatar", Toast.LENGTH_SHORT).show()
            onFailure()
        }
    }

    fun actualizarDatoUsuario(
        uid: String,
        campo: String,
        valor: String,
        onSuccess: () -> Unit = {},
        onFailure: () -> Unit = {}
    ) {
        FirebaseFirestore.getInstance()
            .collection("usuarios")
            .document(uid)
            .update(campo, valor)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure() }
    }
}
