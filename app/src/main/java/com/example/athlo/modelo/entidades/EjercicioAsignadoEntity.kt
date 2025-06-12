package com.example.athlo.modelo.entidades

import androidx.room.Entity

@Entity(tableName = "ejercicios_asignados", primaryKeys = ["id"])
data class EjercicioAsignadoEntity(
    val id: String = "",
    val entrenamientoId: String = "",
    val nombre: String = "",
    val series: Int = 0,
    val repeticiones: Int = 0,
    val peso: Float = 0f,
    val foto: String = "",
    val video: String = "",
    val idEjercicioFirestore: String = ""
)


