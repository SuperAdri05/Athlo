package com.example.athlo.modelo.entidades

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import androidx.annotation.Keep

@Keep
@IgnoreExtraProperties        // Firestore ignorará campos desconocidos
@Entity(tableName = "entrenamientos")
data class EntrenamientoEntity(
    @PrimaryKey
    @get:PropertyName("id")     // asegura mismo nombre de campo
    val id: String = "",

    val nombre: String = "",
    val descripcion: String = "",
    val nivel: String = "",
    val duracionMin: Int = 0
)
