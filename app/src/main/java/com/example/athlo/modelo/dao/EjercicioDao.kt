package com.example.athlo.modelo.dao

import androidx.room.*
import com.example.athlo.modelo.entreno.EjercicioDisponible

@Dao
interface EjercicioDao {
    @Query("SELECT * FROM ejercicios_disponibles")
    suspend fun obtenerTodos(): List<EjercicioDisponible>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(ejercicio: EjercicioDisponible)

    @Query("DELETE FROM ejercicios_disponibles")
    suspend fun borrarTodos()
}
