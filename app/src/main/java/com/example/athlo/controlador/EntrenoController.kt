package com.example.athlo.controlador

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.runtime.MutableState
import com.example.athlo.modelo.*
import com.example.athlo.modelo.entidades.EjercicioAsignadoEntity
import com.example.athlo.modelo.entidades.EntrenamientoEntity
import com.example.athlo.modelo.entidades.ResumenEntrenoEntity
import com.example.athlo.modelo.entidades.ResumenSetEntity
import com.example.athlo.modelo.entreno.EjercicioAsignado
import com.example.athlo.modelo.entreno.EjercicioAsignadoRemote
import com.example.athlo.modelo.entreno.Entrenamiento
import com.example.athlo.modelo.entreno.EntrenoViewModel
import com.example.athlo.modelo.entreno.ResumenEjercicio
import com.example.athlo.modelo.entreno.ResumenEntreno
import com.example.athlo.modelo.entreno.SetData
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.util.Date

@Suppress("MemberVisibilityCanBePrivate")
object EntrenoController {

    /* ----------------------------------------------------
     * Dependencias BD y Firestore
     * --------------------------------------------------*/
    private var db: AppDatabase? = null
    @SuppressLint("StaticFieldLeak")
    private var firestore: FirebaseFirestore? = null

    fun init(context: Context) {
        if (db == null) db = AppDatabase.obtenerInstancia(context)
        if (firestore == null) firestore = FirebaseFirestore.getInstance()
    }

    /* ----------------------------------------------------
     * ROOM helpers
     * --------------------------------------------------*/
    suspend fun guardarEntrenamientoCompleto(ent: EntrenamientoEntity, ejercicios: List<EjercicioAsignadoEntity>) =
        db?.entrenamientoDao()?.insertarEntrenoCompleto(ent, ejercicios)

    suspend fun obtenerEntrenamientos(): List<EntrenamientoEntity> =
        db?.entrenamientoDao()?.obtenerEntrenamientos() ?: emptyList()

    suspend fun obtenerEjerciciosDeEntreno(id: String): List<EjercicioAsignadoEntity> =
        db?.entrenamientoDao()?.obtenerEjerciciosPorEntrenamiento(id) ?: emptyList()


    /* ----------------------------------------------------
     * Firestore helpers (por usuario)
     * --------------------------------------------------*/

    fun subirEntrenamientoUsuario(uid: String, ent: EntrenamientoEntity) {
        firestore
            ?.collection("usuarios")
            ?.document(uid)
            ?.collection("entrenamientos")
            ?.document(ent.id)
            ?.set(ent)
    }

    fun subirEjercicioAsignadoUsuario(uid: String, entId: String, ej: EjercicioAsignadoEntity) {
        val firestoreRef = firestore ?: return

        // ❗ VALIDACIÓN: debe tener ID válido de Firestore
        val ejercicioId = ej.idEjercicioFirestore
        if (ejercicioId.isNullOrBlank()) {
            println("❌ ID del ejercicio vacío. No se puede crear referencia a Firestore.")
            return
        }

        val refEjercicio = firestoreRef.collection("ejercicios").document(ejercicioId)

        // 🔄 Recuperamos el documento del ejercicio original
        refEjercicio.get().addOnSuccessListener { doc ->
            val foto = doc.getString("foto") ?: ""
            val video = doc.getString("video") ?: ""

            val remote = EjercicioAsignadoRemote(
                ejercicioRef = refEjercicio,
                nombre = ej.nombre,
                series = ej.series,
                repeticiones = ej.repeticiones,
                peso = ej.peso,
                foto = foto,
                video = video
            )

            firestoreRef
                .collection("usuarios")
                .document(uid)
                .collection("entrenamientos")
                .document(entId)
                .collection("ejerciciosAsignados")
                .document(ej.id)
                .set(remote)

            println("📦 Subiendo: ejercicioRef = ${remote.ejercicioRef?.path}")

        }.addOnFailureListener {
            println("❌ Error al recuperar documento $ejercicioId: ${it.message}")
        }
    }



    /* ----------------------------------------------------
     * Descargar entrenamientos de usuario y guardar en Room
     * (si lo necesitas en otra pantalla)
     * --------------------------------------------------*/
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun descargarEntrenosDeUsuario(uid: String) {
        firestore
            ?.collection("usuarios")
            ?.document(uid)
            ?.collection("entrenamientos")
            ?.get()
            ?.addOnSuccessListener { snap ->
                val remotos = snap.toObjects(EntrenamientoEntity::class.java)

                GlobalScope.launch(Dispatchers.IO) {
                    // ① Borra todo lo local
                    db?.entrenamientoDao()?.borrarTodosLosEntrenamientos()

                    // ② Inserta los recién bajados
                    remotos.forEach { db?.entrenamientoDao()?.insertarEntrenamiento(it) }
                }
            }
    }

    /* ----------------------------------------------------
     * API principal que la UI debe llamar
     * --------------------------------------------------*/
    @OptIn(DelicateCoroutinesApi::class)
    fun guardarAsignacion(
        context: Context,
        uid: String,
        entrenamiento: Entrenamiento?,
        ejerciciosAsignados: List<EjercicioAsignado>
    ) {
        if (entrenamiento == null) return
        init(context)

        val entEntity = EntrenamientoEntity(
            entrenamiento.id,
            entrenamiento.nombre,
            entrenamiento.descripcion,
            entrenamiento.nivel,
            entrenamiento.duracionMin
        )

        // Aquí preservamos el idEjercicioFirestore
        val ejEntities = ejerciciosAsignados.map {
            EjercicioAsignadoEntity(
                id = it.id.ifBlank { java.util.UUID.randomUUID().toString() },
                entrenamientoId = entrenamiento.id,
                nombre = it.nombre,
                series = it.series,
                repeticiones = it.repeticiones,
                peso = it.peso,
                foto = it.foto,
                video = it.video,
                idEjercicioFirestore = it.idEjercicioFirestore
            )
        }

        GlobalScope.launch(Dispatchers.IO) {
            // 1️⃣ Local (Room)
            guardarEntrenamientoCompleto(entEntity, ejEntities)

            // 2️⃣ Firestore
            subirEntrenamientoUsuario(uid, entEntity)
            ejEntities.forEach { subirEjercicioAsignadoUsuario(uid, entEntity.id, it) }
        }
    }

    suspend fun obtenerEjerciciosDeEntrenoDesdeFirestore(entrenoId: String): List<EjercicioAsignadoEntity> {
        val lista = mutableListOf<EjercicioAsignadoEntity>()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: throw Exception("No hay usuario")
        val snap = firestore
            ?.collection("usuarios")
            ?.document(uid)
            ?.collection("entrenamientos")
            ?.document(entrenoId)
            ?.collection("ejerciciosAsignados")
            ?.get()
            ?.await()

        snap?.documents?.forEach { doc ->
            snapshotToEjercicioAsignadoEntity(doc, entrenoId)?.let { lista.add(it) }
        }



        return lista
    }

    fun snapshotToEjercicioAsignadoEntity(doc: DocumentSnapshot, entrenoId: String): EjercicioAsignadoEntity? {
        val remote = doc.toObject(EjercicioAsignadoRemote::class.java) ?: return null
        val masterId = remote.ejercicioRef?.id ?: ""
        return EjercicioAsignadoEntity(
            id = doc.id,
            entrenamientoId = entrenoId,
            nombre = remote.nombre,
            series = remote.series,
            repeticiones = remote.repeticiones,
            peso = remote.peso,
            foto = remote.foto,
            video = remote.video,
            idEjercicioFirestore = masterId
        )
    }


    suspend fun cargarEntrenos(
        context: Context,
        viewModel: EntrenoViewModel,
        listaEntrenos: MutableState<List<Entrenamiento>>,
        onError: (String?) -> Unit
    ) {
        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            init(context)
            if (uid != null) descargarEntrenosDeUsuario(uid)
            onError(null)
        } catch (e: Exception) {
            onError("Sin conexión. Se muestran los entrenamientos guardados en el dispositivo.")
        } finally {
            listaEntrenos.value = EntrenoController.obtenerEntrenamientos()
                .map {
                    Entrenamiento(
                        id = it.id,
                        nombre = it.nombre,
                        descripcion = it.descripcion,
                        nivel = it.nivel,
                        duracionMin = it.duracionMin,
                        ejercicios = emptyList()
                    )
                }
        }
    }

    suspend fun borrarEntrenamiento(uid: String, entrenamientoId: String) {
        // Local (Room)
        db?.entrenamientoDao()?.borrarEjerciciosDeEntrenamiento(entrenamientoId)
        db?.entrenamientoDao()?.borrarPorId(entrenamientoId)

        // Remoto (Firestore)
        firestore
            ?.collection("usuarios")
            ?.document(uid)
            ?.collection("entrenamientos")
            ?.document(entrenamientoId)
            ?.delete()

        firestore
            ?.collection("usuarios")
            ?.document(uid)
            ?.collection("entrenamientos")
            ?.document(entrenamientoId)
            ?.collection("ejerciciosAsignados")
            ?.get()
            ?.addOnSuccessListener { snapshot ->
                snapshot.documents.forEach { it.reference.delete() }
            }
    }

    fun borrarEjercicioAsignadoUsuario(
        context: Context,                      // recibe el Context para init()
        uid: String,
        entrenoId: String,
        ejercicioAsignadoId: String
    ) {
        init(context)                           // inicializa db y firestore si hace falta

        // ——— 1) Borra el doc en Firestore ———
        firestore
            ?.collection("usuarios")
            ?.document(uid)
            ?.collection("entrenamientos")
            ?.document(entrenoId)
            ?.collection("ejerciciosAsignados")
            ?.document(ejercicioAsignadoId)
            ?.delete()

        // ——— 2) Borra también en Room ———
        GlobalScope.launch(Dispatchers.IO) {
            db?.entrenamientoDao()?.deleteEjercicioById(ejercicioAsignadoId)
        }
    }

    fun borrarTodasAsignacionesUsuario(
        context: Context,
        uid: String,
        entrenoId: String
    ) {
        init(context)
        // Firestore: borra cada doc de la subcolección
        firestore
            ?.collection("usuarios")?.document(uid)
            ?.collection("entrenamientos")?.document(entrenoId)
            ?.collection("ejerciciosAsignados")
            ?.get()?.addOnSuccessListener { snap ->
                snap.documents.forEach { it.reference.delete() }
            }
        // Room: borra en bloque
        GlobalScope.launch(Dispatchers.IO) {
            db?.entrenamientoDao()?.deleteEjerciciosByEntrenoId(entrenoId)
        }
    }

    /**
     * Guarda el resumen de un entrenamiento bajo
     * usuarios/{uid}/resumenesEntrenos/{resumenId}
     */
    fun guardarResumenEntrenoUsuario(resumen: ResumenEntreno) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("No hay usuario autenticado")

        val resumenRef = firestore
            ?.collection("usuarios")
            ?.document(uid)
            ?.collection("resumenesEntrenos")
            ?.document(resumen.id)

        resumenRef?.set(resumen)
    }



    suspend fun guardarResumenEnLocal(context: Context, resumen: ResumenEntreno) {
        val db = AppDatabase.obtenerInstancia(context)

        val resumenEntity = ResumenEntrenoEntity(
            id = resumen.id,
            fecha = resumen.fecha.time,
            duracionSec = resumen.duracionSec,
            calorias = resumen.calorias,
            pesoTotal = resumen.pesoTotal
        )

        val sets = resumen.ejercicios.flatMap { ejercicio ->
            ejercicio.sets.map { set ->
                ResumenSetEntity(
                    entrenoId = resumen.id,
                    resumenId = resumen.id,
                    nombreEjercicio = ejercicio.nombre,
                    peso = set.peso,
                    repeticiones = set.repeticiones
                )
            }
        }

        db.entrenamientoDao().insertarResumen(resumenEntity)
        db.entrenamientoDao().insertarSetsResumen(sets)
    }

    suspend fun obtenerUltimosSetsPorEjercicio(context: Context): Map<String, List<SetData>> {
        init(context)
        val resumenes = db?.entrenamientoDao()?.obtenerResumenes()?.sortedByDescending { it.fecha }
        Log.d("DEBUG", "Resumenes encontrados: ${resumenes?.size}")
        val mapa = mutableMapOf<String, List<SetData>>()

        if (!resumenes.isNullOrEmpty()) {
            val ultimo = resumenes.first()
            val sets = db?.entrenamientoDao()?.obtenerSetsResumen(ultimo.id) ?: emptyList()
            Log.d("DEBUG", "Sets encontrados en resumen ${ultimo.id}: ${sets.size}")
            sets.groupBy { it.nombreEjercicio.lowercase().trim() }.forEach { (nombre, lista) ->
                mapa[nombre] = lista.map { SetData(it.peso, it.repeticiones) }
            }
        }


        return mapa
    }

    suspend fun puedeCrearNuevoEntreno(context: Context): Boolean {
        init(context)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return false
        return try {
            val snap = firestore
                ?.collection("usuarios")
                ?.document(uid)
                ?.collection("entrenamientos")
                ?.get()
                ?.await()
            val remotos = snap?.toObjects(EntrenamientoEntity::class.java) ?: emptyList()
            remotos.size < 10
        } catch (e: Exception) {
            // Si hay error, mira los locales
            val locales = db?.entrenamientoDao()?.obtenerEntrenamientos() ?: emptyList()
            locales.size < 10
        }
    }

    suspend fun obtenerTodosLosResumenes(context: Context): List<ResumenEntreno> {
        init(context)
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val firestore = FirebaseFirestore.getInstance()

        return try {
            // 🔄 Intentar descargar de Firestore
            val snapshot = firestore.collection("usuarios")
                .document(uid ?: throw Exception("Usuario no autenticado"))
                .collection("resumenesEntrenos")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    val id = doc.getString("id") ?: return@mapNotNull null
                    val fecha = doc.getDate("fecha") ?: Date()
                    val duracion = doc.getLong("duracionSec")?.toInt() ?: 0
                    val calorias = doc.getDouble("calorias")?.toInt() ?: 0
                    val pesoTotal = doc.getDouble("pesoTotal")?.toFloat() ?: 0f
                    val entrenamientoId = doc.getString("entrenamientoId") ?: ""
                    val ejerciciosRaw = doc.get("ejercicios") as? List<Map<String, Any>> ?: emptyList()

                    val ejercicios = ejerciciosRaw.map { ejercicioMap ->
                        val nombre = ejercicioMap["nombre"] as? String ?: ""
                        val setsRaw = ejercicioMap["sets"] as? List<Map<String, Any>> ?: emptyList()
                        val sets = setsRaw.map { set ->
                            SetData(
                                peso = (set["peso"] as? Number)?.toFloat() ?: 0f,
                                repeticiones = (set["repeticiones"] as? Number)?.toInt() ?: 0
                            )
                        }
                        ResumenEjercicio(nombre = nombre, sets = sets)
                    }
                    ResumenEntreno(
                        id = id,
                        fecha = fecha,
                        duracionSec = duracion,
                        calorias = calorias,
                        pesoTotal = pesoTotal,
                        entrenamientoId = entrenamientoId,
                        ejercicios = ejercicios
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }


        } catch (e: Exception) {
            // 🔁 Fallback local con Room
            val dao = AppDatabase.obtenerInstancia(context).entrenamientoDao()
            val resumenes = dao.obtenerResumenes()
            resumenes.map { resumen ->
                val sets = dao.obtenerSetsResumen(resumen.id)
                val ejercicios = sets.groupBy { it.nombreEjercicio }.map { (nombre, lista) ->
                    ResumenEjercicio(
                        nombre = nombre,
                        sets = lista.map { SetData(it.peso, it.repeticiones) }
                    )
                }

                ResumenEntreno(
                    id = resumen.id,
                    fecha = Date(resumen.fecha),
                    duracionSec = resumen.duracionSec,
                    calorias = resumen.calorias,
                    pesoTotal = resumen.pesoTotal,
                    entrenamientoId = "",
                    ejercicios = ejercicios
                )
            }
        }
    }

    suspend fun obtenerNombreDeEntrenamiento(entrenamientoId: String): String {
        return try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return "Entrenamiento"
            val doc = FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(uid)
                .collection("entrenamientos")
                .document(entrenamientoId)
                .get()
                .await()

            doc.getString("nombre") ?: "Entrenamiento"
        } catch (e: Exception) {
            "Entrenamiento"
        }
    }

    suspend fun obtenerUltimosSetsDesdeFirestore(entrenamientoId: String): Map<String, List<SetData>> {
        val resumenesRef = Firebase.firestore
            .collection("usuarios")
            .document(FirebaseAuth.getInstance().currentUser?.uid ?: return emptyMap())
            .collection("resumenesEntrenos")
            .whereEqualTo("entrenamientoId", entrenamientoId)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .limit(1)

        val snapshot = resumenesRef.get().await()
        val resumen = snapshot.documents.firstOrNull() ?: return emptyMap()

        val ejercicios = resumen.get("ejercicios") as? List<Map<String, Any>> ?: return emptyMap()

        val resultado = mutableMapOf<String, List<SetData>>()
        for (ej in ejercicios) {
            val nombre = (ej["nombre"] as? String)?.lowercase()?.trim() ?: continue
            val sets = (ej["sets"] as? List<Map<String, Any>>)?.mapNotNull {
                val peso = (it["peso"] as? Number)?.toFloat() ?: return@mapNotNull null
                val reps = (it["repeticiones"] as? Number)?.toInt() ?: return@mapNotNull null
                SetData(peso, reps)
            } ?: continue
            resultado[nombre] = sets
        }

        return resultado
    }

}