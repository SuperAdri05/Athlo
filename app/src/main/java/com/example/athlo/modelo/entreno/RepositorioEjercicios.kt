package com.example.athlo.modelo.entreno

import android.content.Context
import com.example.athlo.modelo.AppDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class RepositorioEjercicios(private val context: Context) {

    private val dao = AppDatabase.obtenerInstancia(context).ejercicioDao()
    private val firestore = FirebaseFirestore.getInstance()

    /** 🔄 Sincroniza Firestore → Room */
    suspend fun sincronizarDesdeFirestore() {
        try {
            val snapshot = firestore.collection("ejercicios").get().await()

            val lista = snapshot.documents.mapNotNull { doc ->
                // id = identificador del documento
                val id = doc.id
                val nombre = doc.getString("nombre") ?: return@mapNotNull null
                val musculo = doc.getString("musculo") ?: ""
                val descripcion = doc.getString("descripcion") ?: ""
                val foto = doc.getString("foto") ?: ""
                val video = doc.getString("video") ?: ""

                EjercicioDisponible(
                    id = id,
                    nombre = nombre,
                    musculo = musculo,
                    descripcion = descripcion,
                    foto = foto,
                    video = video
                )
            }

            dao.borrarTodos()
            lista.forEach { dao.insertar(it) }

        } catch (e: Exception) {
            // TODO: log o manejo de error según tus necesidades
        }
    }

    /** 📥 Devuelve los ejercicios guardados localmente (Room) */
    suspend fun obtenerLocales(): List<EjercicioDisponible> = dao.obtenerTodos()

    /** ➕ Crea ejercicio en Firestore y Room */
    suspend fun crearEnFirestore(ejercicio: EjercicioDisponible) {
        // Para no guardar el campo id dentro del doc, construimos un mapa sin él
        val data = hashMapOf(
            "nombre" to ejercicio.nombre,
            "musculo" to ejercicio.musculo,
            "descripcion" to ejercicio.descripcion,
            "foto" to ejercicio.foto,
            "video" to ejercicio.video
        )

        firestore.collection("ejercicios")
            .document(ejercicio.id.toString())  // id numérico como identificador
            .set(data)
            .await()

        dao.insertar(ejercicio)
    }
}
