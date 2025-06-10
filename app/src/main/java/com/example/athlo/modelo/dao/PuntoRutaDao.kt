package com.example.athlo.modelo.dao

import androidx.room.*
import com.example.athlo.modelo.mapa.PuntoRuta

@Dao
interface PuntoRutaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(punto: PuntoRuta)

    @Query("SELECT * FROM puntos_ruta WHERE entrenamientoId = :id ORDER BY timestamp ASC")
    suspend fun obtenerRuta(id: String): List<PuntoRuta>

    @Query("SELECT * FROM puntos_ruta WHERE sincronizado = 0")
    suspend fun obtenerPendientes(): List<PuntoRuta>

    @Query("UPDATE puntos_ruta SET sincronizado = 1 WHERE id IN (:ids)")
    suspend fun marcarComoSincronizados(ids: List<Int>)
}
