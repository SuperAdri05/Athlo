package com.example.athlo.modelo.mapa

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "puntos_ruta")
data class PuntoRuta(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val altitud: Double = 0.0,
    val entrenamientoId: String,
    val latitud: Double,
    val longitud: Double,
    val timestamp: Long,
    val sincronizado: Boolean = false
)
