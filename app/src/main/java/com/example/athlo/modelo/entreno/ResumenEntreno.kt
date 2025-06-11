package com.example.athlo.modelo.entreno

import com.google.errorprone.annotations.Keep
import java.util.Date

data class ResumenEntreno(
    val id: String = "",            // UUID del resumen
    val fecha: Date = Date(),       // Fecha y hora de finalización
    val duracionSec: Int = 0,       // Duración total en segundos
    val calorias: Int = 0,          // Calorías estimadas
    val pesoTotal: Float = 0f,      // Suma de (peso × reps) de todas las series
    val ejercicios: List<ResumenEjercicio> = emptyList(), // Datos por ejercicio
    val entrenamientoId: String
    )

@Keep
data class ResumenEjercicio(
    val nombre: String = "",               // Nombre del ejercicio
    val sets: List<SetData> = emptyList()  // Lista de series
)

@Keep
data class SetData(
    val peso: Float = 0f,         // Peso levantado en esta serie
    val repeticiones: Int = 0     // Reps realizadas
)

data class ResumenConNombre(
    val resumen: ResumenEntreno,
    val nombreEntreno: String
)
