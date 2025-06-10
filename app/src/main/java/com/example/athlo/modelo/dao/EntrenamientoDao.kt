package com.example.athlo.modelo.dao

import androidx.room.*
import com.example.athlo.modelo.entidades.EntrenamientoEntity
import com.example.athlo.modelo.entidades.EjercicioAsignadoEntity
import com.example.athlo.modelo.entidades.ResumenEntrenoEntity
import com.example.athlo.modelo.entidades.ResumenSetEntity

@Dao
interface EntrenamientoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarEntrenamiento(entrenamiento: EntrenamientoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarEjercicios(ejercicios: List<EjercicioAsignadoEntity>)

    @Transaction
    suspend fun insertarEntrenoCompleto(
        entrenamiento: EntrenamientoEntity,
        ejercicios: List<EjercicioAsignadoEntity>
    ) {
        insertarEntrenamiento(entrenamiento)
        insertarEjercicios(ejercicios)
    }

    @Query("SELECT * FROM entrenamientos")
    suspend fun obtenerEntrenamientos(): List<EntrenamientoEntity>

    @Query("SELECT * FROM ejercicios_asignados WHERE entrenamientoId = :entrenamientoId")
    suspend fun obtenerEjerciciosPorEntrenamiento(entrenamientoId: String): List<EjercicioAsignadoEntity>

    @Query("DELETE FROM entrenamientos")
    suspend fun borrarEntrenamientos()

    @Query("DELETE FROM ejercicios_asignados")
    suspend fun borrarEjercicios()

    @Query("DELETE FROM entrenamientos")
    suspend fun borrarTodosLosEntrenamientos()

    @Query("DELETE FROM entrenamientos WHERE id = :entrenamientoId")
    suspend fun borrarPorId(entrenamientoId: String)

    @Query("DELETE FROM ejercicios_asignados WHERE entrenamientoId = :entrenamientoId")
    suspend fun borrarEjerciciosDeEntrenamiento(entrenamientoId: String)

    @Query("DELETE FROM ejercicios_asignados WHERE id = :ejId")
    suspend fun deleteEjercicioById(ejId: String)

    @Query("DELETE FROM ejercicios_asignados WHERE entrenamientoId = :entrenoId")
    suspend fun deleteEjerciciosByEntrenoId(entrenoId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarResumen(resumen: ResumenEntrenoEntity)

    @Insert
    suspend fun insertarSetsResumen(sets: List<ResumenSetEntity>)

    @Query("SELECT * FROM resumenes_entrenos")
    suspend fun obtenerResumenes(): List<ResumenEntrenoEntity>

    @Query("SELECT * FROM resumenes_sets WHERE resumenId = :id")
    suspend fun obtenerSetsResumen(id: String): List<ResumenSetEntity>

}

