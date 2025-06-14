package com.example.athlo.controlador

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.example.athlo.modelo.AppDatabase
import com.example.athlo.modelo.entreno.ResumenEntreno
import com.example.athlo.modelo.mapa.RegistroRuta
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

object InicioController {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    @SuppressLint("ConstantLocale")
    private val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

    // Meta realista de calorías a gastar por actividad diaria, no el metabolismo basal
    private fun calcularMetaDeGasto(objetivo: String): Int {
        return when (objetivo.lowercase()) {
            "perder peso" -> 500   // déficit calórico por ejercicio
            "mantener peso" -> 300 // actividad básica saludable
            "ganar masa" -> 200    // suficiente activación sin pérdida
            else -> 300
        }
    }

    suspend fun obtenerMetaDiaria(context: Context): Int {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return 300
        return try {
            val snap = firestore.collection("usuarios").document(uid).get().await()
            val objetivo = snap.getString("objetivo") ?: "mantener peso"
            calcularMetaDeGasto(objetivo)
        } catch (e: Exception) {
            300
        }
    }

    suspend fun obtenerCaloriasConsumidasHoy(context: Context): Int {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Log.e("CALORIAS", "No hay usuario logueado, no se puede calcular calorías")
            return 0
        }

        val hoyStr = dateFormat.format(Date())

        return try {
            val resumenesSnap = firestore.collection("usuarios").document(uid)
                .collection("resumenesEntrenos").get().await()
            val rutasSnap = firestore.collection("usuarios").document(uid)
                .collection("registros_ruta").get().await()

            val entrenos = resumenesSnap.documents.mapNotNull {
                try {
                    it.toObject(ResumenEntreno::class.java)
                } catch (e: Exception) {
                    Log.e("CALORIAS", "Error deserializando resumen: ${it.id} → ${e.message}")
                    null
                }
            }.filter { dateFormat.format(it.fecha) == hoyStr }

            val rutas = rutasSnap.documents.mapNotNull {
                try {
                    it.toObject(RegistroRuta::class.java)
                } catch (e: Exception) {
                    Log.e("CALORIAS", "Error deserializando ruta: ${it.id} → ${e.message}")
                    null
                }
            }.filter { dateFormat.format(Date(it.fecha)) == hoyStr }

            val totalEntrenos = entrenos.sumOf { it.calorias }
            val totalRutas = rutas.sumOf { it.caloriasEstimadas }

            val total = totalEntrenos + totalRutas
            context.getSharedPreferences("athlo_cache", Context.MODE_PRIVATE)
                .edit().putInt("kcalHoy", total).apply()
            return total

        } catch (e: Exception) {
            Log.e("CALORIAS", "Fallo en Firestore. Usando Room: ${e.message}", e)
            val dao = AppDatabase.obtenerInstancia(context).entrenamientoDao()
            val resumenes = dao.obtenerResumenes()
            val totalLocal = resumenes.filter {
                dateFormat.format(Date(it.fecha)) == hoyStr
            }.sumOf { it.calorias }

            Log.d("CALORIAS", "Total desde Room (offline): $totalLocal")
            context.getSharedPreferences("athlo_cache", Context.MODE_PRIVATE)
                .edit().putInt("kcalHoy", totalLocal).apply()
            return totalLocal

        }
    }


    fun escucharMetaDiaria(
        context: Context,
        onMetaCambiada: (Int) -> Unit
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val docRef = firestore.collection("usuarios").document(uid)

        docRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

            // Evitar trabajo en el hilo principal
            CoroutineScope(Dispatchers.Default).launch {
                val objetivo = snapshot.getString("objetivo") ?: "mantener peso"
                val meta = calcularMetaDeGasto(objetivo)
                withContext(Dispatchers.Main) {
                    onMetaCambiada(meta)
                }
            }
        }
    }

}
