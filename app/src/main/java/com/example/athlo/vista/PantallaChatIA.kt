package com.example.athlo.vista

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.athlo.controlador.ChatIAController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val CHAT_PREFS_KEY = "chat_prefs"
private const val CHAT_HISTORY_KEY = "mensajes"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaChatIA(navController: NavController) {
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences(CHAT_PREFS_KEY, Context.MODE_PRIVATE)
    val mensajes = remember { mutableStateListOf<String>() }
    var entradaUsuario by remember { mutableStateOf(TextFieldValue("")) }
    val scope = rememberCoroutineScope()
    var escribiendo by remember { mutableStateOf(false) }
    var mostrarDialogoConfirmacion by remember { mutableStateOf(false) }

    // Restaurar historial al entrar
    LaunchedEffect(Unit) {
        if (sharedPrefs.contains(CHAT_HISTORY_KEY)) {
            try {
                sharedPrefs.getString(CHAT_HISTORY_KEY, null)?.let {
                    mensajes.clear()
                    mensajes.addAll(it.split("\n"))
                }
            } catch (e: ClassCastException) {
                sharedPrefs.edit().remove(CHAT_HISTORY_KEY).apply()
                mensajes.clear()
                mensajes.add("Athlo IA: Hola, ¿en qué puedo ayudarte hoy?")
            }
        } else {
            mensajes.add("Athlo IA: Hola, ¿en qué puedo ayudarte hoy?")
        }
    }

    fun guardarHistorial() {
        val limitado = mensajes.takeLast(50)
        sharedPrefs.edit()
            .putString(CHAT_HISTORY_KEY, limitado.joinToString("\n"))
            .apply()
    }

    fun limpiarChat() {
        mensajes.clear()
        mensajes.add("Athlo IA: Hola, ¿en qué puedo ayudarte hoy?")
        sharedPrefs.edit().remove(CHAT_HISTORY_KEY).apply()
    }

    if (mostrarDialogoConfirmacion) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoConfirmacion = false },
            title = { Text("Limpiar chat") },
            text = { Text("¿Seguro que quieres borrar todo el historial del chat?") },
            confirmButton = {
                TextButton(onClick = {
                    limpiarChat()
                    mostrarDialogoConfirmacion = false
                }) {
                    Text("Sí")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoConfirmacion = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat IA") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { mostrarDialogoConfirmacion = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Limpiar chat")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(8.dp)
                .imePadding()
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                reverseLayout = false,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(mensajes) { mensaje ->
                    val esUsuario = mensaje.startsWith("Tú:")
                    val texto = mensaje.removePrefix("Tú:").removePrefix("Athlo IA:").trim()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (esUsuario) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            color = if (esUsuario) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium,
                            tonalElevation = 2.dp
                        ) {
                            Text(
                                text = texto,
                                modifier = Modifier.padding(12.dp),
                                color = if (esUsuario) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                if (escribiendo) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.medium,
                                    tonalElevation = 2.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .padding(end = 8.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Text(
                                            text = "Athlo IA está escribiendo...",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = entradaUsuario,
                    onValueChange = { entradaUsuario = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Escribe tu pregunta...") },
                    singleLine = true
                )
                IconButton(onClick = {
                    val mensaje = entradaUsuario.text.trim()
                    if (mensaje.isNotBlank()) {
                        mensajes.add("Tú: $mensaje")
                        entradaUsuario = TextFieldValue("")
                        escribiendo = true
                        scope.launch {
                            delay(100)
                            val respuesta = ChatIAController.enviarMensaje(mensaje)
                            mensajes.add("Athlo IA: $respuesta")
                            escribiendo = false
                            guardarHistorial()
                        }
                    }
                }) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}