package com.example.athlo.vista.entreno

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.athlo.controlador.EntrenoController
import com.example.athlo.modelo.entreno.EntrenoViewModel
import com.example.athlo.modelo.entreno.ResumenEjercicio
import com.example.athlo.modelo.entreno.ResumenEntreno
import com.example.athlo.modelo.entreno.SetData
import com.example.athlo.modelo.entreno.guardarEstadoCompleto
import com.example.athlo.modelo.entreno.limpiarEstadoEntreno
import com.example.athlo.modelo.entreno.toDomain
import com.example.athlo.servicio.EntrenoService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("ContextCastToActivity")
@Composable
fun PantallaEjecutarEntreno(
    navController: NavHostController,
    viewModel: EntrenoViewModel,
    onVolver: () -> Unit,
    onIrResumen: (ResumenEntreno) -> Unit,
) {
    val entrenamientoEnCurso = viewModel.entrenamientoEnCurso

    // Si no hay entrenamiento, lanza la navegación hacia atrás
    LaunchedEffect(entrenamientoEnCurso) {
        if (entrenamientoEnCurso == null || entrenamientoEnCurso.id.isBlank()) {
            Log.d("PantallaEjecutar", "No hay entreno en curso → saliendo")
            onVolver()
        }
    }

    // Si no hay entrenamiento aún, no mostramos nada (para evitar pantalla blanca)
    if (entrenamientoEnCurso == null || entrenamientoEnCurso.id.isBlank()) {
        Box(Modifier.fillMaxSize()) {
            Text("Cargando...", modifier = Modifier.align(Alignment.Center))
        }
        return
    }

    var entrenamiento by remember { mutableStateOf(entrenamientoEnCurso) }
    val scope = rememberCoroutineScope()
    var cargandoEjercicios by remember { mutableStateOf(true) }

    val context = LocalContext.current
    var debeGuardarEstado by remember { mutableStateOf(true) }

    // Estado principal
    var seconds by remember { mutableIntStateOf(viewModel.tiempoAcumulado) }
    var showDialogFinish by remember { mutableStateOf(false) }
    var showDialogCancel by remember { mutableStateOf(false) }
    var isMinimized by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }

    val anterioresPorEjercicio = remember { mutableMapOf<String, List<SetData>>() }

    val anteriores = viewModel.anteriores
    val esperados = viewModel.esperados
    val hechos = viewModel.hechos
    val completadas = viewModel.completadas

    var showDialogBack by remember { mutableStateOf(false) }

    // Cronómetro
    LaunchedEffect(Unit) {
        val serviceIntent = Intent(context, EntrenoService::class.java).apply {
            putExtra(EntrenoService.EXTRA_TIEMPO, "00:00")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // En lugar de while infinito, usamos una corrutina controlada
        while (viewModel.entrenamientoEnCurso != null && !isMinimized) {
            delay(1000)
            if (!isPaused) {
                seconds++

                if (seconds % 5 == 0) {
                    val tiempoStr = "%02d:%02d".format(seconds / 60, seconds % 60)
                    val updateIntent = Intent(context, EntrenoService::class.java).apply {
                        putExtra(EntrenoService.EXTRA_TIEMPO, tiempoStr)
                    }
                    context.startService(updateIntent)
                }
            }
        }
    }


    val activity = LocalContext.current as? android.app.Activity
    BackHandler {
        showDialogCancel = false
        showDialogFinish = false
        showDialogBack = true
    }

    val lifecycleOwner = LocalLifecycleOwner.current

// Guarda al cerrar la pantalla o al salir de la app
    DisposableEffect(Unit) {
        onDispose {
            if (debeGuardarEstado && !isMinimized && viewModel.entrenamientoEnCurso != null) {
                Log.d("DISPOSE", "Guardando estado entreno antes de salir")
                guardarEstadoCompleto(context, viewModel)
            } else {
                Log.d(
                    "DISPOSE",
                    "No se guarda, debeGuardarEstado=$debeGuardarEstado, isMinimized=$isMinimized"
                )
            }
        }

    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && !isMinimized) {
                viewModel.entrenamientoEnCurso = entrenamiento
                viewModel.tiempoAcumulado = seconds
                viewModel.estaMinimizado = true
                guardarEstadoCompleto(context, viewModel)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Carga ejercicios
    LaunchedEffect(entrenamiento.id) {
        cargandoEjercicios = true
        EntrenoController.init(context)

        // Cargar ejercicios desde Firestore
        val ejerciciosRemote = EntrenoController.obtenerEjerciciosDeEntrenoDesdeFirestore(entrenamiento.id)
        if (ejerciciosRemote.isNotEmpty()) {
            entrenamiento = entrenamiento.copy(ejercicios = ejerciciosRemote.map { it.toDomain() })
        }

        // 🔁 Intentar primero obtener sets anteriores desde Firestore
        val anterioresFirestore = EntrenoController.obtenerUltimosSetsDesdeFirestore(entrenamiento.id)

        val anterioresCargados = if (anterioresFirestore.isNotEmpty()) {
            Log.d("ANTERIOR", "Usando datos anteriores desde Firestore")
            anterioresFirestore
        } else {
            Log.d("ANTERIOR", "No hay datos en Firestore, usando Room")
            EntrenoController.obtenerUltimosSetsPorEjercicio(context)
        }

        anterioresCargados.forEach { (nombreEjercicio, listaSets) ->
            listaSets.forEachIndexed { idx, set ->
                val clave = nombreEjercicio.lowercase().trim() to idx
                viewModel.anteriores[clave] = "${set.peso}x${set.repeticiones}"
            }
        }

        cargandoEjercicios = false
    }



    Scaffold(
        topBar = {
            // Panel superior con sombra ligera
            Surface(
                tonalElevation = 4.dp,
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 16.dp)
                ) {
                    IconButton(
                        onClick = {
                            isPaused = true
                            isMinimized = true

                            // Guarda el estado para restaurarlo desde PantallaEntreno
                            viewModel.entrenamientoEnCurso = entrenamiento
                            viewModel.tiempoAcumulado = seconds
                            viewModel.estaMinimizado = true
                            guardarEstadoCompleto(context, viewModel)


                            onVolver()
                        },
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Minimizar"
                        )
                    }


                    // Cronómetro centrado
                    Text(
                        text = "%02d:%02d".format(seconds / 60, seconds % 60),
                        fontSize = 20.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TextButton(onClick = { showDialogFinish = true }) {
                            Text("Terminar", color = MaterialTheme.colorScheme.error)
                        }
                        IconButton(onClick = { showDialogCancel = true }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                tint = MaterialTheme.colorScheme.error,
                                contentDescription = "Cancelar entrenamiento"
                            )
                        }
                    }
                }
            }
        }

    ) { inner ->
        Box(Modifier.fillMaxSize()) {
            // Contenido principal
            if (!isMinimized) {
                if (cargandoEjercicios) {
                    // Indicador de carga mientras se descargan ejercicios
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                } else {
                    Column(
                        Modifier
                            .padding(inner)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LazyColumn(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(entrenamiento.ejercicios, key = { it.id }) { ejercicio ->
                                var expanded by remember { mutableStateOf(false) }
                                Card(
                                    Modifier
                                        .fillMaxWidth()
                                        .animateContentSize()
                                        .clickable { expanded = !expanded },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(Modifier.padding(0.dp)) {
                                        // Cabecera con fondo azul y foto
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.primary)
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            // Imagen
                                            Surface(
                                                shape = CircleShape,
                                                shadowElevation = 4.dp,
                                                color = Color.White,
                                                modifier = Modifier.size(70.dp)
                                            ) {
                                                androidx.compose.foundation.Image(
                                                    painter = rememberAsyncImagePainter(ejercicio.foto),
                                                    contentDescription = "Foto ejercicio",
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(4.dp)
                                                        .clip(CircleShape)
                                                )
                                            }

                                            // Título y expansión
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = ejercicio.nombre,
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        color = MaterialTheme.colorScheme.onPrimary,
                                                        fontSize = 18.sp
                                                    ),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    IconButton(
                                                        onClick = {
                                                            navController.navigate("info_ejercicio/${ejercicio.idEjercicioFirestore}")
                                                        }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Info,
                                                            contentDescription = "Info del ejercicio",
                                                            tint = MaterialTheme.colorScheme.onPrimary
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { expanded = !expanded }
                                                    ) {
                                                        Icon(
                                                            imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                                            contentDescription = "Expandir ejercicio",
                                                            tint = MaterialTheme.colorScheme.onPrimary
                                                        )
                                                    }
                                                }
                                            }
                                        }


                                            if (expanded) {
                                            Column {
                                                val screenWidth =
                                                    LocalContext.current.resources.displayMetrics.widthPixels / LocalContext.current.resources.displayMetrics.density
                                                val colWidth =
                                                    screenWidth / 5  // 5 columnas: SERIE, ANTERIOR, KG, REPS, CHECK
                                                // Cabecera
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        Modifier.width(colWidth.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            "SERIE",
                                                            style = MaterialTheme.typography.labelSmall
                                                        )
                                                    }
                                                    Box(
                                                        Modifier.width(colWidth.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            "ANTERIOR",
                                                            style = MaterialTheme.typography.labelSmall
                                                        )
                                                    }
                                                    Box(
                                                        Modifier.width(colWidth.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            "KG",
                                                            style = MaterialTheme.typography.labelSmall
                                                        )
                                                    }
                                                    Box(
                                                        Modifier.width(colWidth.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            "REPS",
                                                            style = MaterialTheme.typography.labelSmall
                                                        )
                                                    }
                                                    Box(
                                                        Modifier.width(colWidth.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        // Tick
                                                    }
                                                }


                                                val id = ejercicio.id
                                                val seriesTotales =
                                                    viewModel.seriesPorEjercicio.getOrPut(id) { ejercicio.series }

                                                repeat(seriesTotales) { idx ->

                                                    val claveNombre = ejercicio.nombre.lowercase().trim()
                                                    val key = claveNombre to idx
                                                    val anterior = anteriores[key] ?: "-"


                                                    val (pesoEsp, repsEsp) = esperados.getOrPut(key) {
                                                        ejercicio.peso.toString() to ejercicio.repeticiones.toString()
                                                    }
                                                    val (pesoReal, repsReal) = hechos.getOrPut(key) { "" to "" }
                                                    val completado =
                                                        completadas.getOrPut(key) { false }

                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 8.dp)
                                                            .background(
                                                                if (completado) MaterialTheme.colorScheme.primary.copy(
                                                                    alpha = 0.1f
                                                                ) else Color.Transparent
                                                            ),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        // SERIE
                                                        Box(
                                                            Modifier.width(colWidth.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text("${idx + 1}")
                                                        }

                                                        // ANTERIOR
                                                        Box(
                                                            Modifier.width(colWidth.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(anterior, color = Color.Gray)
                                                        }

                                                        // KG
                                                        Box(
                                                            modifier = Modifier
                                                                .width(colWidth.dp)
                                                                .height(40.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            BasicTextField(
                                                                value = pesoReal,
                                                                onValueChange = { input ->
                                                                    // Permitir solo números, coma y máximo dos decimales
                                                                    val normalizado = input.replace(',', '.')
                                                                    val regex = Regex("^\\d{0,3}(\\.\\d{0,2})?$")
                                                                    if (regex.matches(normalizado)) {
                                                                        hechos[key] = normalizado to repsReal
                                                                    }

                                                                },
                                                                keyboardOptions = KeyboardOptions(
                                                                    keyboardType = KeyboardType.Number
                                                                ),
                                                                singleLine = true,
                                                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                                    color = MaterialTheme.colorScheme.onSurface,
                                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                                ),
                                                                decorationBox = { innerTextField ->
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .fillMaxSize()
                                                                            .padding(horizontal = 4.dp),
                                                                        contentAlignment = Alignment.Center
                                                                    ) {
                                                                        if (pesoReal.isBlank()) {
                                                                            Text(
                                                                                text = pesoEsp,
                                                                                color = Color.Gray,
                                                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                                                )
                                                                            )
                                                                        }
                                                                        innerTextField()
                                                                    }
                                                                }
                                                            )

                                                        }

                                                        // REPS
                                                        Box(
                                                            modifier = Modifier
                                                                .width(colWidth.dp)
                                                                .height(40.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            BasicTextField(
                                                                value = repsReal,
                                                                onValueChange = {
                                                                    val filtered =
                                                                        it.filter { char -> char.isDigit() }
                                                                            .take(2)
                                                                    hechos[key] =
                                                                        pesoReal to filtered
                                                                },
                                                                keyboardOptions = KeyboardOptions(
                                                                    keyboardType = KeyboardType.Number
                                                                ),
                                                                singleLine = true,
                                                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                                    color = MaterialTheme.colorScheme.onSurface,
                                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                                ),
                                                                decorationBox = { innerTextField ->
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .fillMaxSize()
                                                                            .padding(horizontal = 4.dp),
                                                                        contentAlignment = Alignment.Center
                                                                    ) {
                                                                        if (repsReal.isBlank()) {
                                                                            Text(
                                                                                text = repsEsp,
                                                                                color = Color.Gray,
                                                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                                                )
                                                                            )
                                                                        }
                                                                        innerTextField()
                                                                    }
                                                                }
                                                            )
                                                        }


                                                        // CHECK
                                                        Box(
                                                            Modifier.width(colWidth.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            IconButton(onClick = {
                                                                completadas[key] = !completado
                                                            }) {
                                                                Icon(
                                                                    imageVector = Icons.Filled.Check,
                                                                    contentDescription = "Completado",
                                                                    tint = if (completado) MaterialTheme.colorScheme.primary else Color.Gray
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                Spacer(Modifier.height(8.dp))
                                                TextButton(onClick = {
                                                    val key = ejercicio.nombre to seriesTotales
                                                    // Buscar si hay datos anteriores para esa serie
                                                    val setAnterior =
                                                        anterioresPorEjercicio[ejercicio.nombre]?.getOrNull(
                                                            seriesTotales
                                                        )
                                                    Log.d("ANTERIORES DEBUG", "Cargados: ${anterioresPorEjercicio.keys}")

                                                    val textoAnterior =
                                                        setAnterior?.let { "${it.peso}x${it.repeticiones}" }
                                                            ?: "-"

                                                    esperados[key] =
                                                        ejercicio.peso.toString() to ejercicio.repeticiones.toString()
                                                    anteriores[key] = textoAnterior
                                                    hechos[key] = "" to ""
                                                    completadas[key] = false
                                                    viewModel.seriesPorEjercicio[ejercicio.id] =
                                                        seriesTotales + 1

                                                }) {
                                                    Icon(
                                                        Icons.Filled.Add,
                                                        contentDescription = null
                                                    )
                                                    Spacer(Modifier.width(8.dp))
                                                    Text("Agregar Serie")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // Diálogo botón atrás
            if (showDialogBack) {
                AlertDialog(
                    onDismissRequest = { showDialogBack = false },
                    title = { Text("¿Qué deseas hacer?") },
                    text = { Text("Estás en medio de un entrenamiento. ¿Qué deseas hacer?") },
                    confirmButton = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    showDialogBack = false
                                    showDialogFinish = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50), // verde
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                Text("✅ Finalizar entreno")
                            }

                            Button(
                                onClick = {
                                    showDialogBack = false
                                    isPaused = true
                                    isMinimized = true
                                    viewModel.entrenamientoEnCurso = entrenamiento
                                    viewModel.tiempoAcumulado = seconds
                                    viewModel.estaMinimizado = true
                                    guardarEstadoCompleto(context, viewModel)
                                    onVolver()
                                },

                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2196F3), // azul
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                Text("📥 Minimizar y volver")
                            }

                            OutlinedButton(
                                onClick = { showDialogBack = false },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFF44336) // rojo
                                ),
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                Text("❌ Cancelar")
                            }
                        }
                    },
                    dismissButton = {}
                )
            }

            if (showDialogFinish) {
                AlertDialog(
                    onDismissRequest = { showDialogFinish = false },
                    title = { Text("Finalizar entrenamiento") },
                    text = { Text("¿Estás seguro de que deseas finalizar y guardar el entrenamiento?") },
                    confirmButton = {
                        TextButton(onClick = {
                            showDialogFinish = false

                            // ✅ Generar resumen
                            val resumenId = java.util.UUID.randomUUID().toString()
                            val ejerciciosResumen = entrenamiento.ejercicios.map { ej ->
                                val sets = (0 until (viewModel.seriesPorEjercicio[ej.id] ?: ej.series)).mapNotNull { idx ->
                                    val key = ej.nombre.lowercase().trim() to idx
                                    val (pesoStr, repsStr) = viewModel.hechos[key] ?: return@mapNotNull null

                                    val pesoNormalizado = pesoStr.replace(",", ".")
                                    val peso = pesoNormalizado.toFloatOrNull()
                                    val reps = repsStr.toIntOrNull()

                                    if (peso != null && reps != null && reps > 0) {
                                        SetData(peso = peso, repeticiones = reps)
                                    } else null
                                }

                                ResumenEjercicio(nombre = ej.nombre, sets = sets)
                            }


                            val pesoTotal = ejerciciosResumen.flatMap { it.sets }
                                .sumOf { (it.peso * it.repeticiones).toDouble() }
                                .toFloat()

                            val caloriasEstimadas = (pesoTotal / 100 * 5).toInt()

                            val resumen = ResumenEntreno(
                                id = resumenId,
                                fecha = java.util.Date(),
                                duracionSec = seconds,
                                calorias = caloriasEstimadas,
                                pesoTotal = pesoTotal,
                                ejercicios = ejerciciosResumen,
                                entrenamientoId = entrenamiento.id
                            )


                            debeGuardarEstado = false

                            // Guardamos el resumen ANTES de limpiar el ViewModel
                            viewModel.resumenActual = resumen

                            // Navegamos a la pantalla de resumen ANTES de borrar el estado
                            onIrResumen(resumen)

                        }) {
                            Text("Sí, finalizar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDialogFinish = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            if (showDialogCancel) {
                AlertDialog(
                    onDismissRequest = { showDialogCancel = false },
                    title = { Text("Cancelar entrenamiento") },
                    text = { Text("¿Estás seguro de que deseas cancelar el entrenamiento actual? Se perderán los datos.") },
                    confirmButton = {
                        TextButton(onClick = {
                            showDialogCancel = false
                            context.stopService(Intent(context, EntrenoService::class.java))
                            debeGuardarEstado = false
                            limpiarEstadoEntreno(context)

                            scope.launch {
                                viewModel.resetEntreno()
                                delay(100)
                                onVolver()
                            }
                        }) {
                            Text("Sí, cancelar", color = MaterialTheme.colorScheme.error)
                        }

                    },
                    dismissButton = {
                        TextButton(onClick = { showDialogCancel = false }) {
                            Text("No")
                        }
                    }
                )
            }


        }
    }
}
