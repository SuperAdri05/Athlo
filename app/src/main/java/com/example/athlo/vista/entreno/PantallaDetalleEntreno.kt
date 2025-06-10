package com.example.athlo.vista.entreno

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.athlo.controlador.EntrenoController
import com.example.athlo.modelo.entreno.Entrenamiento
import com.example.athlo.modelo.entreno.EntrenoViewModel
import com.example.athlo.modelo.entreno.toDomain
import com.example.athlo.modelo.entreno.toEntity
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PantallaDetalleEntreno(
    viewModel: EntrenoViewModel,
    navController: NavHostController,
    onVolver: () -> Unit,
    onIniciar: () -> Unit,
    onAgregarEjercicios: () -> Unit,
) {


    val context = LocalContext.current

    BackHandler {
        navController.popBackStack("entreno", inclusive = false)
    }


    // --- Estados principales ---
    val seleccionado = viewModel.entrenamientoSeleccionado
    if (seleccionado == null) {
        LaunchedEffect(Unit) {
            navController.popBackStack("entreno", false)
        }
        return
    }

    var entrenamiento by remember { mutableStateOf(seleccionado) }
    var ejercicios by remember { mutableStateOf(entrenamiento.ejercicios) }
    var cargando by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Estado para mostrar acciones sobre un ejercicio concreto
    var mostrarAccionesId by remember { mutableStateOf<String?>(null) }
    var ejercicioEditando by remember {
        mutableStateOf<com.example.athlo.modelo.entreno.EjercicioAsignado?>(
            null
        )
    }
    var ejercicioParaEliminar by remember {
        mutableStateOf<com.example.athlo.modelo.entreno.EjercicioAsignado?>(
            null
        )
    }

    val mostrarDialogoInterrupcion = remember { mutableStateOf<Entrenamiento?>(null) }
    val enCurso = viewModel.entrenamientoEnCurso

    val mostrarDialogoEditarDuranteEntreno = remember { mutableStateOf(false) }
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = cargando)
    val scope = rememberCoroutineScope()

    suspend fun recargarEjercicios() {
        cargando = true
        errorMsg = null
        try {
            EntrenoController.init(context)
            val remotos =
                EntrenoController.obtenerEjerciciosDeEntrenoDesdeFirestore(entrenamiento.id)
            if (remotos.isNotEmpty()) {
                ejercicios = remotos.map { it.toDomain() }
                entrenamiento = entrenamiento.copy(ejercicios = ejercicios)
                if (viewModel.entrenamientoEnCurso?.id != entrenamiento.id) {
                    viewModel.entrenamientoSeleccionado = entrenamiento
                }
                EntrenoController.guardarEntrenamientoCompleto(
                    entrenamiento.toEntity(),
                    remotos
                )
            } else throw Exception("Firestore vacío")
        } catch (e: Exception) {
            val locales = EntrenoController.obtenerEjerciciosDeEntreno(entrenamiento.id)
            ejercicios = locales.map { it.toDomain() }
            entrenamiento = entrenamiento.copy(ejercicios = ejercicios)
            viewModel.entrenamientoSeleccionado = entrenamiento
        } finally {
            cargando = false
        }
    }

    LaunchedEffect(Unit) { recargarEjercicios() }

    // --- Sincronización Firestore → Room → UI ---
    LaunchedEffect(Unit) {
        try {
            EntrenoController.init(context)
            val remotos =
                EntrenoController.obtenerEjerciciosDeEntrenoDesdeFirestore(entrenamiento.id)
            if (remotos.isNotEmpty()) {
                ejercicios = remotos.map { it.toDomain() }
                entrenamiento = entrenamiento.copy(ejercicios = ejercicios)
                if (viewModel.entrenamientoEnCurso?.id != entrenamiento.id) {
                    viewModel.entrenamientoSeleccionado = entrenamiento
                }

                // Guardar localmente
                EntrenoController.guardarEntrenamientoCompleto(
                    entrenamiento.toEntity(),
                    remotos
                )
            } else throw Exception("Firestore vacío")
        } catch (e: Exception) {
            // Fallback a Room local
            val locales = EntrenoController.obtenerEjerciciosDeEntreno(entrenamiento.id)
            ejercicios = locales.map { it.toDomain() }
            entrenamiento = entrenamiento.copy(ejercicios = ejercicios)
            viewModel.entrenamientoSeleccionado = entrenamiento
        } finally {
            cargando = false
        }
    }

    // --- UI ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("💪 ${entrenamiento.nombre}", fontSize = 24.sp) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { inner ->
        Box(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (cargando) {
                // Indicador de carga
                CircularProgressIndicator(Modifier.align(Alignment.Center))
                return@Box
            }
            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = { scope.launch { recargarEjercicios() } }) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {

                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Datos generales
                        Text(
                            "Nivel: ${entrenamiento.nivel}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Duración: ${entrenamiento.duracionMin} min",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text("Descripción: ${entrenamiento.descripcion}")

                        Spacer(Modifier.height(16.dp))

                        // Botón central de iniciar entreno
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .shadow(8.dp, shape = CircleShape)
                                .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                                .clickable {
                                    when {
                                        enCurso != null && enCurso.id == entrenamiento.id -> {
                                            viewModel.estaMinimizado = false
                                            onIniciar()
                                        }

                                        enCurso != null && enCurso.id != entrenamiento.id -> {
                                            mostrarDialogoInterrupcion.value = entrenamiento
                                        }

                                        else -> {
                                            viewModel.entrenamientoSeleccionado = entrenamiento
                                            viewModel.entrenamientoEnCurso = entrenamiento
                                            viewModel.tiempoAcumulado = 0
                                            viewModel.estaMinimizado = false
                                            onIniciar()
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Iniciar entreno",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Ejercicios", style = MaterialTheme.typography.titleLarge)
                        errorMsg?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        if (enCurso != null) {
                            Text(
                                "Hay un entrenamiento en curso. No se puede editar ningún entrenamiento.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        // Lista de ejercicios con long-press para acciones
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(ejercicios, key = { it.id }) { ejercicio ->
                                var expanded by remember { mutableStateOf(false) }

                                // Cada ejercicio en un Box para superponer los botones
                                Box(Modifier.fillMaxWidth()) {
                                    // Tarjeta clicable / long-press
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animateContentSize()
                                            .combinedClickable(
                                                onClick = {
                                                    expanded = !expanded
                                                    mostrarAccionesId = null
                                                },
                                                onLongClick = {
                                                    if (enCurso == null) {
                                                        mostrarAccionesId = ejercicio.id
                                                    }
                                                }
                                            ),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Column(Modifier.padding(16.dp)) {
                                            // Fila principal
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                AsyncImage(
                                                    model = ejercicio.foto,
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .size(64.dp)
                                                        .background(
                                                            MaterialTheme.colorScheme.surface,
                                                            CircleShape
                                                        )
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        ejercicio.nombre,
                                                        style = MaterialTheme.typography.titleMedium
                                                    )
                                                }
                                                Icon(
                                                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                    contentDescription = null
                                                )
                                            }

                                            // Detalle desplegable
                                            if (expanded) {
                                                Spacer(Modifier.height(8.dp))
                                                Text("Series: ${ejercicio.series} · Reps: ${ejercicio.repeticiones} · Peso: ${ejercicio.peso} kg")
                                                Spacer(Modifier.height(4.dp))
                                                TextButton(onClick = {
                                                    val intent = Intent(
                                                        Intent.ACTION_VIEW,
                                                        Uri.parse(ejercicio.video)
                                                    )
                                                    context.startActivity(intent)
                                                }) {
                                                    Text(
                                                        "▶️ Ver video",
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Botones flotantes de Editar / Borrar
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = mostrarAccionesId == ejercicio.id && enCurso == null,
                                        enter = fadeIn(),
                                        exit = fadeOut(),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentSize(Alignment.TopEnd)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            // Editar
                                            IconButton(onClick = {
                                                if (enCurso == null) {
                                                    ejercicioEditando = ejercicio
                                                }
                                                mostrarAccionesId = null
                                            }) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = "Editar ejercicio"
                                                )
                                            }
                                            // Borrar (solo si queda >1)
                                            IconButton(onClick = {
                                                if (enCurso == null && ejercicios.size > 1) {
                                                    ejercicioParaEliminar = ejercicio
                                                } else if (ejercicios.size <= 1) {
                                                    Toast.makeText(
                                                        context,
                                                        "Debe quedar al menos 1 ejercicio",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                                mostrarAccionesId = null
                                            }) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Borrar ejercicio",
                                                    tint = if (ejercicios.size > 1)
                                                        MaterialTheme.colorScheme.error
                                                    else
                                                        Color.Gray
                                                )
                                            }

                                        }
                                    }
                                }
                            }
                            item {
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { if (enCurso == null) onAgregarEjercicios() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    enabled = enCurso == null
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Añadir ejercicio",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                Spacer(Modifier.height(80.dp)) // espacio para el panel inferior
                            }


                        }

                    }
                    if (enCurso == null) {
                        ejercicioEditando?.let { ej ->
                            var series by remember { mutableStateOf(ej.series.toString()) }
                            var reps by remember { mutableStateOf(ej.repeticiones.toString()) }
                            var peso by remember { mutableStateOf(ej.peso.toString()) }

                            AlertDialog(
                                onDismissRequest = { ejercicioEditando = null },
                                title = { Text("Editar '${ej.nombre}'") },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = series,
                                            onValueChange = {
                                                if (it.all(Char::isDigit)) series = it.take(2)
                                            },
                                            label = { Text("Series") }
                                        )
                                        OutlinedTextField(
                                            value = reps,
                                            onValueChange = {
                                                if (it.all(Char::isDigit)) reps = it.take(2)
                                            },
                                            label = { Text("Repeticiones") }
                                        )
                                        OutlinedTextField(
                                            value = peso,
                                            onValueChange = {
                                                if (it.all(Char::isDigit)) peso = it.take(3)
                                            },
                                            label = { Text("Peso (kg)") }
                                        )
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        val actualizado = ej.copy(
                                            series = series.toIntOrNull() ?: ej.series,
                                            repeticiones = reps.toIntOrNull() ?: ej.repeticiones,
                                            peso = peso.toIntOrNull() ?: ej.peso
                                        )
                                        ejercicios = ejercicios.map { if (it.id == ej.id) actualizado else it }
                                        entrenamiento = entrenamiento.copy(ejercicios = ejercicios)
                                        viewModel.entrenamientoSeleccionado = entrenamiento

                                        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@TextButton
                                        EntrenoController.guardarAsignacion(
                                            context,
                                            uid,
                                            entrenamiento,
                                            ejercicios
                                        )

                                        ejercicioEditando = null
                                    }) {
                                        Text("Guardar")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { ejercicioEditando = null }) {
                                        Text("Cancelar")
                                    }
                                }
                            )
                        }
                    }
                    // — Diálogo BORRAR ejercicio actualizado —
                    if (enCurso == null) {
                        ejercicioParaEliminar?.let { ej ->
                            AlertDialog(
                                onDismissRequest = { ejercicioParaEliminar = null },
                                title = { Text("Eliminar '${ej.nombre}'") },
                                text = { Text("¿Seguro que deseas eliminar este ejercicio?") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        // 1️⃣ Actualizamos la lista en memoria
                                        ejercicios = ejercicios.filter { it.id != ej.id }
                                        entrenamiento = entrenamiento.copy(ejercicios = ejercicios)
                                        viewModel.entrenamientoSeleccionado = entrenamiento

                                        // 2️⃣ Persistimos local + remoto
                                        val uid =
                                            FirebaseAuth.getInstance().currentUser?.uid
                                                ?: return@TextButton
                                        // ← Aquí pasamos `context` como primer parámetro
                                        EntrenoController.borrarEjercicioAsignadoUsuario(
                                            context,
                                            uid,
                                            entrenamiento.id,
                                            ej.id
                                        )
                                        EntrenoController.guardarAsignacion(
                                            context,
                                            uid,
                                            entrenamiento,
                                            ejercicios
                                        )

                                        ejercicioParaEliminar = null
                                    }) {
                                        Text("Eliminar", color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { ejercicioParaEliminar = null }) {
                                        Text("Cancelar")
                                    }
                                }
                            )
                        }
                    }
                    if (mostrarDialogoEditarDuranteEntreno.value) {
                        AlertDialog(
                            onDismissRequest = { mostrarDialogoEditarDuranteEntreno.value = false },
                            title = { Text("Entreno en curso") },
                            text = {
                                Text("Estás editando un entrenamiento que está en curso. ¿Qué deseas hacer?")
                            },
                            confirmButton = {
                                Column {
                                    Button(
                                        onClick = {
                                            viewModel.estaMinimizado = false
                                            navController.navigate("pantalla_ejecutar_entreno")
                                            mostrarDialogoEditarDuranteEntreno.value = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Seguir con el entreno")
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.entrenamientoEnCurso = null
                                            viewModel.estaMinimizado = false
                                            onAgregarEjercicios()
                                            mostrarDialogoEditarDuranteEntreno.value = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Finalizar y modificar el entreno")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            mostrarDialogoEditarDuranteEntreno.value = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Cancelar")
                                    }
                                }
                            },
                            dismissButton = {}
                        )
                    }
                }
                PanelEntrenoMinimizado(
                    viewModel = viewModel,
                    navController = navController,
                    mostrarDialogo = mostrarDialogoInterrupcion
                )
            }
        }
    }
}
