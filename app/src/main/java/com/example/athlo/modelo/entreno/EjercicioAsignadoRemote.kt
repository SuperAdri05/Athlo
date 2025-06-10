
/**
 * Objeto que viaja a Firestore dentro de
 * users/{uid}/entrenamientos/{ent}/ejerciciosAsignados/{ej}
 *
 * Solo incluye una referencia al ejercicio maestro
 * y la configuración (nombre, series, repeticiones, peso).
 */
package com.example.athlo.modelo.entreno

import com.google.firebase.firestore.DocumentReference

data class EjercicioAsignadoRemote(
    val ejercicioRef: DocumentReference? = null,
    val nombre: String = "",
    val series: Int = 0,
    val repeticiones: Int = 0,
    val peso: Int = 0,
    val foto: String = "",
    val video: String = ""
)

