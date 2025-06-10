package com.example.athlo.vista.entreno

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.athlo.controlador.EjercicioController
import com.example.athlo.controlador.EntrenoController
import com.example.athlo.modelo.entreno.EjercicioAsignado
import com.example.athlo.modelo.entreno.EjercicioDisponible
import com.example.athlo.modelo.entreno.EntrenoViewModel
import com.example.athlo.modelo.entreno.toDomain
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * PantallaAsignarEjercicios – V11
 * • Sincroniza con Firestore o Room al iniciar.
 * • Guarda IDs originales para evitar duplicados.
 * • Botón fijo, tarjetas se pliegan al añadir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAsignarEjercicios(
    viewModel: EntrenoViewModel,
    onBack: () -> Unit,
    onFinalizar: (List<EjercicioAsignado>) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Carga ejercicios disponibles
    var ejercicios by remember { mutableStateOf(emptyList<EjercicioDisponible>()) }
    var cargando by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try {
            EjercicioController.init(context)
            EjercicioController.sincronizar()
            ejercicios = EjercicioController.obtenerEjercicios()
        } catch (e: Exception) {
            errorMsg = "No se pudieron cargar los ejercicios"
        } finally {
            cargando = false
        }
    }

    // UI state
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var showFilter by remember { mutableStateOf(false) }
    var grupoSeleccion by remember { mutableStateOf("Todos") }
    var expandedCards by remember { mutableStateOf(setOf<String>()) }

    // Estado de asignados y datos previos
    var asignados by remember { mutableStateOf(setOf<String>()) }
    val datosEjercicios = remember { mutableStateMapOf<String, Triple<String, String, String>>() }
    var inicial by remember { mutableStateOf<List<EjercicioAsignado>>(emptyList()) }

    BackHandler {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val entreno = viewModel.entrenamientoSeleccionado
        if (uid != null && entreno != null && inicial.isEmpty()) {
            scope.launch {
                EntrenoController.borrarEntrenamiento(uid, entreno.id)
            }
        }
        viewModel.entrenamientoActual = null
        viewModel.entrenamientoSeleccionado = null
        onBack()
    }

    // Cargar asignados reales desde Firestore o Room
    val entrenoId = viewModel.entrenamientoSeleccionado?.id ?: ""
    Log.d("PantallaAsignar", "EntrenoId actual: '$entrenoId'")
    // dentro de tu Composable, tras obtener entrenoId:
    LaunchedEffect(entrenoId) {
        if (entrenoId.isBlank()) return@LaunchedEffect
        // Esto lee SIEMPRE de Firestore o de Room, NO de memoria
        inicial = try {
            EntrenoController
                .obtenerEjerciciosDeEntrenoDesdeFirestore(entrenoId)
                .map { it.toDomain() }
        } catch (_: Exception) {
            EntrenoController
                .obtenerEjerciciosDeEntreno(entrenoId)
                .map { it.toDomain() }
        }
        asignados = inicial.map { it.nombre }.toSet()
        datosEjercicios.clear()
        inicial.forEach { ej ->
            datosEjercicios[ej.nombre] = Triple(
                ej.series.toString(),
                ej.repeticiones.toString(),
                ej.peso.toString()
            )
        }
    }


    val grupos =
        remember(ejercicios) { listOf("Todos") + ejercicios.map { it.musculo }.distinct().sorted() }
    val listaUI = ejercicios
        .filter { grupoSeleccion == "Todos" || it.musculo == grupoSeleccion }
        .filter { it.nombre.contains(searchQuery, ignoreCase = true) }
        .sortedWith(compareByDescending<EjercicioDisponible> { asignados.contains(it.nombre) }
            .thenBy { it.nombre })

    // Campo numérico
    @Composable
    fun NumberField(
        value: String,
        onChange: (String) -> Unit,
        label: String,
        maxLength: Int,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { input ->
                val filtered = input.filter { it.isDigit() }.take(maxLength)
                onChange(filtered)
            },
            label = { Text(text = label) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }


    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        val uid = FirebaseAuth.getInstance().currentUser?.uid
                        val entreno = viewModel.entrenamientoSeleccionado
                        if (uid != null && entreno != null && inicial.isEmpty()) {
                            scope.launch {
                                EntrenoController.borrarEntrenamiento(uid, entreno.id)
                            }
                        }
                        viewModel.entrenamientoActual = null
                        viewModel.entrenamientoSeleccionado = null
                        onBack()
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                },
                title = {
                    if (showSearch) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(text = "Buscar…") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(text = viewModel.entrenamientoActual?.nombre ?: "Entrenamiento")
                    }
                },
                actions = {
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null
                        )
                    }
                    Box {
                        IconButton(onClick = { showFilter = true }) {
                            Icon(
                                Icons.Filled.FilterList,
                                contentDescription = null
                            )
                        }
                        DropdownMenu(
                            expanded = showFilter,
                            onDismissRequest = { showFilter = false }) {
                            grupos.forEach { grupo ->
                                DropdownMenuItem(
                                    text = { Text(text = grupo) },
                                    onClick = { grupoSeleccion = grupo; showFilter = false }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { inner ->
        Box(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            when {
                cargando -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                errorMsg != null -> Text(
                    text = errorMsg!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(listaUI, key = { it.nombre }) { ejercicio ->
                            val expanded = ejercicio.nombre in expandedCards
                            val datos = datosEjercicios[ejercicio.nombre] ?: Triple("", "", "")
                            val camposOk =
                                datos.first.isNotBlank() && datos.second.isNotBlank() && datos.third.isNotBlank()
                            val agregado = ejercicio.nombre in asignados

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                                    .clickable {
                                        expandedCards =
                                            if (expanded) expandedCards - ejercicio.nombre else expandedCards + ejercicio.nombre
                                    }
                                    .animateContentSize(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (agregado)
                                        Color(0xFF90CAF9).copy(alpha = 0.3f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                elevation = CardDefaults.cardElevation(4.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {


                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(ejercicio.foto).build(),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                        )
                                        Column {
                                            Text(
                                                text = ejercicio.nombre,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = ejercicio.musculo,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }


                                    if (expanded) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(text = ejercicio.descripcion)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        NumberField(
                                            value = datos.first,
                                            onChange = { v ->
                                                datosEjercicios[ejercicio.nombre] =
                                                    Triple(v, datos.second, datos.third)
                                            },
                                            label = "Series",
                                            maxLength = 2
                                        )

                                        NumberField(
                                            value = datos.second,
                                            onChange = { v ->
                                                datosEjercicios[ejercicio.nombre] =
                                                    Triple(datos.first, v, datos.third)
                                            },
                                            label = "Reps",
                                            maxLength = 2
                                        )

                                        NumberField(
                                            value = datos.third,
                                            onChange = { v ->
                                                datosEjercicios[ejercicio.nombre] =
                                                    Triple(datos.first, datos.second, v)
                                            },
                                            label = "Peso (kg)",
                                            maxLength = 3
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        val uid = FirebaseAuth.getInstance().currentUser?.uid
                                            ?: return@Card
                                        Button(
                                            onClick = {
                                                if (!agregado && camposOk) {
                                                    val newEj = EjercicioAsignado(
                                                        id = UUID.randomUUID().toString(),
                                                        nombre = ejercicio.nombre,
                                                        series = datos.first.toInt(),
                                                        repeticiones = datos.second.toInt(),
                                                        peso = datos.third.toInt(),
                                                        foto = ejercicio.foto,
                                                        video = ejercicio.video,
                                                        idEjercicioFirestore = ejercicio.id
                                                    )
                                                    asignados = asignados + ejercicio.nombre
                                                    inicial = inicial + newEj

                                                    // Contrae la tarjeta tras añadir
                                                    expandedCards = expandedCards - ejercicio.nombre

                                                } else if (agregado) {
                                                    val asign =
                                                        inicial.first { it.nombre == ejercicio.nombre }
                                                    asignados = asignados - ejercicio.nombre
                                                    inicial =
                                                        inicial.filterNot { it.id == asign.id }
                                                }

                                            },
                                            // Sólo permitimos añadir si los campos están completos, o siempre quitar
                                            enabled = camposOk || agregado,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (agregado)
                                                    MaterialTheme.colorScheme.error
                                                else
                                                    MaterialTheme.colorScheme.primary
                                            ),
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(if (agregado) "Quitar" else "Añadir")
                                        }

                                    }
                                }
                            }
                        }
                    }
                    Button(
                        onClick = {
                            val uid = FirebaseAuth.getInstance().currentUser!!.uid
                            scope.launch {
                                // Borro all lo que había antes
                                EntrenoController.borrarTodasAsignacionesUsuario(
                                    context, uid, entrenoId
                                )
                                // Guardo la lista actual (inicial)
                                EntrenoController.guardarAsignacion(
                                    context, uid,
                                    viewModel.entrenamientoSeleccionado,
                                    inicial
                                )
                            }
                            // ③ Actualizo el VM y vuelvo
                            viewModel.entrenamientoActual = viewModel.entrenamientoSeleccionado
                                ?.copy(ejercicios = inicial)
                            viewModel.entrenamientoSeleccionado = viewModel.entrenamientoActual
                            onFinalizar(inicial)
                        },
                        enabled = inicial.isNotEmpty(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text("Aplicar cambios (${inicial.size})")
                    }
                }
            }
        }
    }
}
