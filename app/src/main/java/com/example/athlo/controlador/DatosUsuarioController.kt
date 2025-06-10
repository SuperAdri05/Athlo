package com.example.athlo.controlador

import com.google.firebase.firestore.FirebaseFirestore

object DatosUsuarioController {

    fun guardarDatosIniciales(
        uid: String,
        peso: String,
        altura: String,
        edad: String,
        genero: String,
        objetivo: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()

        val datos = hashMapOf(
            "peso" to peso,
            "altura" to altura,
            "edad" to edad,
            "genero" to genero,
            "objetivo" to objetivo,
            "datosCompletos" to true
        )

        db.collection("usuarios").document(uid)
            .set(datos)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure() }
    }
}
