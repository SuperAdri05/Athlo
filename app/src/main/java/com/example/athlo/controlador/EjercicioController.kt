package com.example.athlo.controlador

import android.content.Context
import com.example.athlo.modelo.entreno.EjercicioDisponible
import com.example.athlo.modelo.entreno.RepositorioEjercicios

object EjercicioController {
    private var repositorio: RepositorioEjercicios? = null

    fun init(context: Context) {
        if (repositorio == null) {
            repositorio = RepositorioEjercicios(context)
        }
    }

    suspend fun obtenerEjercicios(): List<EjercicioDisponible> {
        return repositorio?.obtenerLocales() ?: emptyList()
    }

    suspend fun sincronizar() {
        repositorio?.sincronizarDesdeFirestore()
    }

    suspend fun crearEjercicio(ejercicio: EjercicioDisponible) {
        repositorio?.crearEnFirestore(ejercicio)
    }
    fun limpiarCache() {
        repositorio?.limpiarCache()
    }
}
