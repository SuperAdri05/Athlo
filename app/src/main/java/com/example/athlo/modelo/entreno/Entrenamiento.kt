package com.example.athlo.modelo.entreno

import com.example.athlo.modelo.entidades.EjercicioAsignadoEntity
import com.example.athlo.modelo.entidades.EntrenamientoEntity

data class Entrenamiento(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val nivel: String,
    val duracionMin: Int,
    val ejercicios: List<EjercicioAsignado> = emptyList()
)

fun Entrenamiento.toEntity(): EntrenamientoEntity {
    return EntrenamientoEntity(
        id = this.id,
        nombre = this.nombre,
        descripcion = this.descripcion,
        nivel = this.nivel,
        duracionMin = this.duracionMin
    )
}

data class EjercicioAsignado(
    val id: String,
    val nombre: String,
    var series: Int,
    val repeticiones: Int,
    val peso: Int,
    val foto: String = "",
    val video: String = "",
    var idEjercicioFirestore: String = ""
)


fun EjercicioAsignado.toEntity(entrenamientoId: String) = EjercicioAsignadoEntity(
    id = id,
    entrenamientoId = entrenamientoId,
    nombre = nombre,
    series = series,
    repeticiones = repeticiones,
    peso = peso,
    foto = foto,
    video = video
)

fun EjercicioAsignadoEntity.toDomain() = EjercicioAsignado(
    id = id,
    nombre = nombre,
    series = series,
    repeticiones = repeticiones,
    peso = peso,
    foto = foto,
    video = video,
    idEjercicioFirestore= idEjercicioFirestore
)

fun EntrenamientoEntity.toDomain() = Entrenamiento(
    id = this.id,
    nombre = this.nombre,
    descripcion = this.descripcion,
    nivel = this.nivel,
    duracionMin = this.duracionMin,
    ejercicios = emptyList()
)

