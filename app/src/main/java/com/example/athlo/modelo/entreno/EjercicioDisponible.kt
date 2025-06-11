package com.example.athlo.modelo.entreno

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ejercicios_disponibles")
data class EjercicioDisponible(
    @PrimaryKey val id: String,
    val nombre: String,
    val musculo: String,
    val descripcion: List<String>,
    val foto: String,
    val video: String
)
