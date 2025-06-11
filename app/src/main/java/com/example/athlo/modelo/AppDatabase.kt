package com.example.athlo.modelo

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.athlo.modelo.dao.PuntoRutaDao
import com.example.athlo.modelo.dao.EjercicioDao
import com.example.athlo.modelo.dao.EntrenamientoDao
import com.example.athlo.modelo.entidades.*
import com.example.athlo.modelo.entreno.EjercicioDisponible
import com.example.athlo.modelo.mapa.PuntoRuta

@Database(
    entities = [
        PuntoRuta::class,
        EjercicioDisponible::class,
        EntrenamientoEntity::class,
        EjercicioAsignadoEntity::class,
        ResumenEntrenoEntity::class,
        ResumenSetEntity::class
    ],
    version = 16
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun puntoRutaDao(): PuntoRutaDao
    abstract fun ejercicioDao(): EjercicioDao
    abstract fun entrenamientoDao(): EntrenamientoDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        fun obtenerInstancia(context: Context): AppDatabase {
            return instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "athlo_db"
                ).fallbackToDestructiveMigration()
                    .build().also { instancia = it }

            }
        }
    }
}
