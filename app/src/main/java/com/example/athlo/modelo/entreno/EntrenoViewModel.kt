package com.example.athlo.modelo.entreno

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class EntrenoViewModel : ViewModel() {
    var entrenamientoActual: Entrenamiento? = null
    var entrenamientoSeleccionado: Entrenamiento? = null

    var entrenamientoEnCurso by mutableStateOf<Entrenamiento?>(null)
    var tiempoAcumulado by mutableIntStateOf(0)
    var estaMinimizado by mutableStateOf(false)

    val anteriores = mutableStateMapOf<Pair<String, Int>, String>()
    val esperados = mutableStateMapOf<Pair<String, Int>, Pair<String, String>>()
    val hechos = mutableStateMapOf<Pair<String, Int>, Pair<String, String>>()
    val completadas = mutableStateMapOf<Pair<String, Int>, Boolean>()
    val seriesPorEjercicio = mutableStateMapOf<String, Int>()

    var resumenActual: ResumenEntreno? = null


    fun resetEntreno() {
        entrenamientoEnCurso = null
        entrenamientoSeleccionado = null
        estaMinimizado = false
        tiempoAcumulado = 0
        anteriores.clear()
        esperados.clear()
        hechos.clear()
        completadas.clear()
        seriesPorEjercicio.clear()
    }



}