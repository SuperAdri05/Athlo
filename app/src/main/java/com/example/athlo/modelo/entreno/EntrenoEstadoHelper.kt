package com.example.athlo.modelo.entreno

import android.content.Context
import android.util.Log
import com.example.athlo.controlador.EntrenoController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Data class persistente
private data class EstadoEntrenoPersistente(
    val entrenamientoId: String,
    val segundos: Int,
    val anteriores: Map<String, String>,
    val esperados: Map<String, Pair<String, String>>,
    val hechos: Map<String, Pair<String, String>>,
    val completadas: Map<String, Boolean>,
    val seriesPorEjercicio: Map<String, Int>
)

fun guardarEstadoCompleto(context: Context, viewModel: EntrenoViewModel) {
    val prefs = context.getSharedPreferences("entreno_estado", Context.MODE_PRIVATE)
    val gson = Gson()

    val estado = EstadoEntrenoPersistente(
        entrenamientoId = viewModel.entrenamientoEnCurso?.id ?: return,
        segundos = viewModel.tiempoAcumulado,
        anteriores = viewModel.anteriores.mapKeys { "${it.key.first}|${it.key.second}" },
        esperados = viewModel.esperados.mapKeys { "${it.key.first}|${it.key.second}" },
        hechos = viewModel.hechos.mapKeys { "${it.key.first}|${it.key.second}" },
        completadas = viewModel.completadas.mapKeys { "${it.key.first}|${it.key.second}" },
        seriesPorEjercicio = viewModel.seriesPorEjercicio.toMap()
    )

    prefs.edit()
        .putString("estado_entreno", gson.toJson(estado))
        .apply()
}

fun restaurarEstadoCompleto(context: Context, viewModel: EntrenoViewModel) {
    val prefs = context.getSharedPreferences("entreno_estado", Context.MODE_PRIVATE)
    val gson = Gson()
    val json = prefs.getString("estado_entreno", null) ?: return

    val type = object : TypeToken<EstadoEntrenoPersistente>() {}.type
    val estado: EstadoEntrenoPersistente = gson.fromJson(json, type)

    viewModel.tiempoAcumulado = estado.segundos

    estado.anteriores.forEach {
        val (nombre, idx) = it.key.split("|")
        viewModel.anteriores[nombre to idx.toInt()] = it.value
    }
    estado.esperados.forEach {
        val (nombre, idx) = it.key.split("|")
        viewModel.esperados[nombre to idx.toInt()] = it.value
    }
    estado.hechos.forEach {
        val (nombre, idx) = it.key.split("|")
        viewModel.hechos[nombre to idx.toInt()] = it.value
    }
    estado.completadas.forEach {
        val (nombre, idx) = it.key.split("|")
        viewModel.completadas[nombre to idx.toInt()] = it.value
    }
    estado.seriesPorEjercicio.forEach {
        viewModel.seriesPorEjercicio[it.key] = it.value
    }
}

fun limpiarEstadoEntreno(context: Context) {
    Log.d("RESTORE", "Estado de entrenamiento limpiado completamente")

    context.getSharedPreferences("entreno_estado", Context.MODE_PRIVATE)
        .edit()
        .clear()
        .apply()
}

fun restaurarEntrenoSiExiste(
    context: Context,
    viewModel: EntrenoViewModel,
    onRecuperado: () -> Unit
) {
    Log.d("RESTORE", "Comprobando si hay entrenamiento a restaurar")

    val prefs = context.getSharedPreferences("entreno_estado", Context.MODE_PRIVATE)
    val json = prefs.getString("estado_entreno", null)

    // Si no hay nada guardado, salimos
    if (json.isNullOrBlank()) return

    val gson = Gson()
    val type = object : TypeToken<EstadoEntrenoPersistente>() {}.type
    val estado: EstadoEntrenoPersistente = gson.fromJson(json, type)

    CoroutineScope(Dispatchers.IO).launch {
        EntrenoController.init(context)

        val entrenoEntity = EntrenoController
            .obtenerEntrenamientos()
            .find { it.id == estado.entrenamientoId }

        val ejercicios = EntrenoController
            .obtenerEjerciciosDeEntrenoDesdeFirestore(estado.entrenamientoId)
            .map { it.toDomain() }

        val entreno = entrenoEntity?.toDomain()?.copy(ejercicios = ejercicios)

        withContext(Dispatchers.Main) {
            Log.d("RESTORE", "Restaurado: ${entreno?.id}, tiempo: ${estado.segundos}s, ejercicios: ${ejercicios.size}")

            // ⚠️ SOLO restauramos si el entreno aún existe y tiene ejercicios
            if (entreno != null && ejercicios.isNotEmpty()) {
                viewModel.entrenamientoEnCurso = entreno
                viewModel.entrenamientoSeleccionado = entreno
                viewModel.estaMinimizado = true
                viewModel.tiempoAcumulado = estado.segundos

                restaurarEstadoCompleto(context, viewModel)
                onRecuperado()
            } else {
                // Si no existe, limpiaremos para evitar errores
                limpiarEstadoEntreno(context)
                Log.d("RESTORE", "Estado inválido. Limpieza automática.")

            }
        }
    }
}

