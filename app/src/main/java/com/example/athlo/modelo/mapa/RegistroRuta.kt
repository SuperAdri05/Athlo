package com.example.athlo.modelo.mapa

data class RegistroRuta(
    val id: String = "",
    val usuarioId: String = "",
    val duracionSegundos: Long = 0L,
    val distanciaMetros: Float = 0f,
    val caloriasEstimadas: Int = 0,
    val fecha: Long = System.currentTimeMillis()
)

