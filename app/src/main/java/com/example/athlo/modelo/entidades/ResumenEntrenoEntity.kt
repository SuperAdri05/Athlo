package com.example.athlo.modelo.entidades

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "resumenes_entrenos")
data class ResumenEntrenoEntity(
    @PrimaryKey val id: String,
    val fecha: Long,
    val duracionSec: Int,
    val calorias: Int,
    val pesoTotal: Float
)

// Relación 1-N con sets
@Entity(
    tableName = "resumenes_sets",
    foreignKeys = [ForeignKey(
        entity = ResumenEntrenoEntity::class,
        parentColumns = ["id"],
        childColumns = ["resumenId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class ResumenSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val entrenoId: String,
    val resumenId: String,
    val nombreEjercicio: String,
    val peso: Float,
    val repeticiones: Int
)