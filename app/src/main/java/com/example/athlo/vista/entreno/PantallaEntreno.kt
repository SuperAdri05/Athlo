package com.example.athlo.vista.entreno

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.athlo.R
import com.example.athlo.controlador.EntrenoController
import com.example.athlo.controlador.EntrenoController.cargarEntrenos
import com.example.athlo.modelo.entreno.Entrenamiento
import com.example.athlo.modelo.entreno.EntrenoViewModel
import com.example.athlo.modelo.entreno.toEntity
import com.google.accompanist.flowlayout.FlowRow
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PantallaEntreno(
    navController: NavHostController,
    listaEntrenos: MutableState<List<Entrenamiento>>,
    viewModel: EntrenoViewModel,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var cargando by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var mostrarAccionesId by remember { mutableStateOf<String?>(null) }
    var entrenoEditando by remember { mutableStateOf<Entrenamiento?>(null) }
    var entrenoParaEliminar by remember { mutableStateOf<Entrenamiento?>(null) }

    val mostrarDialogoInterrupcion = remember { mutableStateOf<Entrenamiento?>(null) }

    val playHandler: (Entrenamiento) -> Unit = { seleccionado ->
        val actual = viewModel.entrenamientoEnCurso

        when {
            // Si ya hay uno en curso y el seleccionado es el mismo → abrirlo directamente
            actual != null && actual.id == seleccionado.id -> {
                viewModel.estaMinimizado = false
                navController.navigate("pantalla_ejecutar_entreno")
            }

            // Si hay uno en curso y el seleccionado es diferente → preguntar
            actual != null && actual.id != seleccionado.id -> {
                mostrarDialogoInterrupcion.value = seleccionado
            }

            // Si no hay entrenamiento en curso → iniciar normalmente
            else -> {
                if (viewModel.entrenamientoEnCurso == null) {
                    viewModel.entrenamientoEnCurso = seleccionado
                    viewModel.tiempoAcumulado = 0
                }
                viewModel.entrenamientoSeleccionado = seleccionado
                viewModel.estaMinimizado = false
                navController.navigate("pantalla_ejecutar_entreno")
            }
        }
    }


    // Inicializar y refrescar
    LaunchedEffect(Unit) {
        cargarEntrenos(context, viewModel, listaEntrenos) { errorMsg = it; cargando = false }
        while (true) {
            delay(10000)
            cargarEntrenos(context, viewModel, listaEntrenos) { errorMsg = it }
        }
    }

    val entrenos = listaEntrenos.value
        .sortedBy { it.nombre.lowercase() }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    if (mostrarAccionesId != null) mostrarAccionesId = null; awaitRelease()
                })
            }
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onPress = {
                        if (mostrarAccionesId != null) mostrarAccionesId = null; awaitRelease()
                    })
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = CircleShape
                            )
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        val miFuente = FontFamily(Font(R.font.fuente2))

                        Text(
                            text = "ENTRENOS",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = miFuente
                            )
                        )
                    }
                }

                when {
                    cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }

                    entrenos.isEmpty() -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No hay entrenamientos")
                    }

                    else -> {
                        val isDark = isSystemInDarkTheme()
                        val tintColor = if (isDark) Color.White else Color.Black

                        Box(Modifier.fillMaxSize()) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_logo),
                                contentDescription = "Logo Marca de Agua",
                                colorFilter = ColorFilter.tint(tintColor),
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .fillMaxSize()
                                    .alpha(0.05f)
                            )

                            SwipeRefresh(
                                state = rememberSwipeRefreshState(isRefreshing),
                                onRefresh = {
                                    isRefreshing = true
                                    scope.launch {
                                        cargarEntrenos(context, viewModel, listaEntrenos) {
                                            errorMsg = it; isRefreshing = false
                                        }
                                    }
                                }
                            ) {

                                LazyColumn(
                                    Modifier
                                        .fillMaxSize()
                                        .padding(16.dp)
                                ) {
                                    items(entrenos) { entreno ->
                                        val selected = mostrarAccionesId == entreno.id
                                        Card(
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (selected)
                                                    MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                            ),
                                            elevation = CardDefaults.cardElevation(8.dp)
                                        ) {
                                            Row(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp)
                                                    .combinedClickable(
                                                        onClick = {
                                                            viewModel.entrenamientoSeleccionado =
                                                                entreno
                                                            navController.navigate("detalle_entreno")
                                                        },
                                                        onLongClick = {
                                                            mostrarAccionesId = entreno.id
                                                        }
                                                    ),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        Modifier
                                                            .size(48.dp)
                                                            .background(
                                                                MaterialTheme.colorScheme.primary.copy(
                                                                    alpha = 0.2f
                                                                ),
                                                                CircleShape
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            "💪",
                                                            style = MaterialTheme.typography.headlineSmall
                                                        )
                                                    }
                                                    Spacer(Modifier.width(12.dp))
                                                    Column {
                                                        Text(
                                                            entreno.nombre,
                                                            style = MaterialTheme.typography.titleLarge
                                                        )
                                                        Text(
                                                            entreno.descripcion,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(
                                                                4.dp
                                                            ),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            AssistChip(
                                                                onClick = {},
                                                                label = {
                                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                                        Box(
                                                                            Modifier
                                                                                .size(10.dp)
                                                                                .background(
                                                                                    colorPorNivel(
                                                                                        entreno.nivel
                                                                                    ),
                                                                                    CircleShape
                                                                                )
                                                                        )
                                                                        Spacer(Modifier.width(4.dp))
                                                                        Text("${entreno.nivel}")
                                                                    }
                                                                },
                                                                colors = AssistChipDefaults.assistChipColors(
                                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                                    labelColor = MaterialTheme.colorScheme.onSurface
                                                                )
                                                            )

                                                            AssistChip(
                                                                onClick = {},
                                                                label = { Text("${entreno.duracionMin} min") },
                                                                colors = AssistChipDefaults.assistChipColors(
                                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                                    labelColor = MaterialTheme.colorScheme.onSurface
                                                                )
                                                            )
                                                        }
                                                    }
                                                }

                                                // Icono de ejecución alineado a la derecha
                                                IconButton(onClick = { playHandler(entreno) }) {
                                                    Icon(
                                                        Icons.Filled.PlayArrow,
                                                        contentDescription = "Ejecutar",
                                                        modifier = Modifier.size(36.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    errorMsg?.let {
                        Snackbar(modifier = Modifier.align(Alignment.TopCenter)) {
                            Text(it)
                        }
                    }
                }
            }
        }

        // FAB crear entreno
        FloatingActionButton(
            onClick = {
                scope.launch {
                    val puede = EntrenoController.puedeCrearNuevoEntreno(context)
                    if (puede) {
                        navController.navigate("crear_entreno")
                    } else {
                        android.widget.Toast.makeText(
                            context,
                            "Máximo 10 entrenamientos permitidos",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Añadir", tint = Color.White)
        }
        FloatingActionButton(
            onClick = { navController.navigate("historial_entrenos") },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.secondary
        ) {
            Icon(Icons.Filled.History, contentDescription = "Historial", tint = Color.White)
        }


        // Acciones flotantes encima del botón de crear entreno
        if (mostrarAccionesId != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 130.dp)
            ) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    val entreno = entrenos.find { it.id == mostrarAccionesId }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        entreno?.let { e ->
                            FloatingActionButton(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                onClick = { entrenoEditando = e; mostrarAccionesId = null }
                            ) { Icon(Icons.Filled.Edit, contentDescription = "Editar") }

                            FloatingActionButton(
                                containerColor = MaterialTheme.colorScheme.error,
                                onClick = { entrenoParaEliminar = e; mostrarAccionesId = null }
                            ) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar") }
                        }
                    }
                }
            }
        }


        // Confirmar eliminar
        entrenoParaEliminar?.let { e ->
            AlertDialog(
                onDismissRequest = { entrenoParaEliminar = null },
                title = { Text("Eliminar entreno") },
                text = { Text("¿Seguro que deseas eliminar '${e.nombre}'?") },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                            EntrenoController.borrarEntrenamiento(uid, e.id)
                            cargarEntrenos(context, viewModel, listaEntrenos) { errorMsg = it }
                            entrenoParaEliminar = null
                        }
                    }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        entrenoParaEliminar = null
                    }) { Text("Cancelar") }
                }
            )
        }

        // Editar diálogo
        entrenoEditando?.let { e ->
            var nombre by remember { mutableStateOf(e.nombre) }
            var descripcion by remember { mutableStateOf(e.descripcion) }
            var duracion by remember { mutableStateOf(e.duracionMin.toString()) }
            var nivel by remember { mutableStateOf(e.nivel) }
            val niveles = listOf("Principiante", "Intermedio", "Avanzado")

            AlertDialog(
                onDismissRequest = { entrenoEditando = null },
                title = { Text("Editar entreno") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Nombre: máximo 20 letras/números/espacios
                        OutlinedTextField(
                            value = nombre,
                            onValueChange = {
                                if (it.length <= 20 && it.all { c -> c.isLetterOrDigit() || c.isWhitespace() }) {
                                    nombre = it
                                }
                            },
                            label = { Text("Nombre") }
                        )

                        // Descripción: máximo 60 caracteres (permite letras, números, espacios y signos básicos)
                        OutlinedTextField(
                            value = descripcion,
                            onValueChange = {
                                if (it.length <= 60 && it.all { c -> c.code in 32..126 }) {
                                    descripcion = it
                                }
                            },
                            label = { Text("Descripción") }
                        )

                        // Duración: solo dígitos y máx 3 cifras (hasta 9999)
                        OutlinedTextField(
                            value = duracion,
                            onValueChange = {
                                if (it.length <= 3 && it.all(Char::isDigit)) {
                                    duracion = it
                                }
                            },
                            label = { Text("Duración min") }
                        )

                        FlowRow(
                            mainAxisSpacing = 8.dp,
                            crossAxisSpacing = 8.dp
                        ) {
                            niveles.forEach { opt ->
                                val esSeleccionado = nivel.equals(opt, ignoreCase = true)
                                AssistChip(
                                    onClick = { nivel = opt },
                                    label = { Text(opt) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (esSeleccionado) colorPorNivel(opt) else MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = if (esSeleccionado) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }

                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                            val actualizado = e.copy(
                                nombre = nombre,
                                descripcion = descripcion,
                                duracionMin = duracion.toIntOrNull() ?: e.duracionMin,
                                nivel = nivel
                            )
                            EntrenoController.guardarEntrenamientoCompleto(
                                actualizado.toEntity(),
                                emptyList()
                            )
                            EntrenoController.subirEntrenamientoUsuario(uid, actualizado.toEntity())
                            cargarEntrenos(context, viewModel, listaEntrenos) { errorMsg = it }
                            entrenoEditando = null
                        }
                    }) { Text("Guardar") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        entrenoEditando = null
                    }) { Text("Cancelar") }
                }
            )
        }


        PanelEntrenoMinimizado(
            viewModel = viewModel,
            navController = navController,
            mostrarDialogo = mostrarDialogoInterrupcion
        )

    }
}


fun colorPorNivel(nivel: String): Color = when (nivel.lowercase()) {
    "principiante" -> Color(0xFF4CAF50)
    "intermedio" -> Color(0xFFFFC107)
    "avanzado" -> Color(0xFFF44336)
    else -> Color.Gray
}

