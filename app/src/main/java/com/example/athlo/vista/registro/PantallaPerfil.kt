package com.example.athlo.vista.registro
import androidx.compose.material3.ExposedDropdownMenuBox
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.athlo.R
import com.example.athlo.controlador.PerfilController
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.pow
import kotlin.random.Random

@Composable
fun PantallaPerfil(navController: NavController) {
    val context = LocalContext.current
    val usuario = PerfilController.obtenerUsuario()
    val nombreInicial = usuario?.displayName
    val nombreAleatorio = "Usuario${Random.nextInt(1000, 9999)}"
    var nombreEditable by remember { mutableStateOf(nombreInicial ?: nombreAleatorio) }
    var editandoNombre by remember { mutableStateOf(nombreInicial == null) }

    val avatarNombre = usuario?.photoUrl?.toString() ?: "avatar_gato"
    val avatarRes = when (avatarNombre) {
        "avatar_perro" -> R.drawable.avatar_perro
        "avatar_mono" -> R.drawable.avatar_mono
        "avatar_conejo" -> R.drawable.avatar_conejo
        "avatar_oso" -> R.drawable.avatar_oso
        "avatar_zorro" -> R.drawable.avatar_zorro
        else -> R.drawable.avatar_gato
    }

    var peso by remember { mutableStateOf<String?>(null) }
    var altura by remember { mutableStateOf<String?>(null) }
    var edad by remember { mutableStateOf<String?>(null) }
    var objetivo by remember { mutableStateOf<String?>(null) }
    var editandoCampo by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        PerfilController.asignarNombreSiNoTiene(context, usuario, nombreEditable)
        usuario?.uid?.let { uid ->
            FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    peso = doc.getString("peso")
                    altura = doc.getString("altura")
                    edad = doc.getString("edad")
                    objetivo = doc.getString("objetivo")
                }
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
        }

        var mostrarDialogoAvatar by remember { mutableStateOf(false) }

        Image(
            painter = painterResource(id = avatarRes),
            contentDescription = "Avatar",
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .clickable { mostrarDialogoAvatar = true }
                .align(Alignment.CenterHorizontally)
        )

        if (mostrarDialogoAvatar) {
            AlertDialog(
                onDismissRequest = { mostrarDialogoAvatar = false },
                confirmButton = {},
                title = { Text("Selecciona tu avatar") },
                text = {
                    val avatares = listOf(
                        R.drawable.avatar_gato,
                        R.drawable.avatar_perro,
                        R.drawable.avatar_mono,
                        R.drawable.avatar_conejo,
                        R.drawable.avatar_oso,
                        R.drawable.avatar_zorro
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        avatares.chunked(3).forEach { fila ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                fila.forEach { avatarResId ->
                                    Image(
                                        painter = painterResource(id = avatarResId),
                                        contentDescription = "Avatar $avatarResId",
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                PerfilController.actualizarAvatar(
                                                    context,
                                                    usuario,
                                                    avatarResId,
                                                    onSuccess = { mostrarDialogoAvatar = false },
                                                    onFailure = { mostrarDialogoAvatar = false }
                                                )
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }



        if (editandoNombre) {
            OutlinedTextField(
                value = nombreEditable,
                onValueChange = { nombreEditable = it },
                label = { Text("Nombre de usuario") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    PerfilController.actualizarNombre(
                        context, usuario, nombreEditable,
                        onSuccess = { editandoNombre = false },
                        onFailure = {}
                    )
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Guardar nombre")
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(nombreEditable, style = MaterialTheme.typography.headlineSmall)
                IconButton(onClick = { editandoNombre = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar nombre")
                }
            }
        }

        HorizontalDivider()

        EditableDato("Peso (kg)", peso, "peso", editandoCampo, onEdit = { editandoCampo = it }) {
            peso = it
            usuario?.uid?.let { uid ->
                PerfilController.actualizarDatoUsuario(
                    uid, "peso", it,
                    onSuccess = {
                        Toast.makeText(context, "Peso guardado", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = {
                        Toast.makeText(context, "Error al guardar peso", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            editandoCampo = null
        }


        EditableDato("Altura (cm)", altura, "altura", editandoCampo, onEdit = { editandoCampo = it }) {
            altura = it
            usuario?.uid?.let { uid ->
                PerfilController.actualizarDatoUsuario(uid, "altura", it,
                    onSuccess = {
                    Toast.makeText(context, "Altura guardado", Toast.LENGTH_SHORT).show()
                },
                    onFailure = {
                        Toast.makeText(context, "Error al guardar altura", Toast.LENGTH_SHORT).show()
                    })
            }
            editandoCampo = null
        }

        EditableDato("Edad", edad, "edad", editandoCampo, onEdit = { editandoCampo = it }) {
            edad = it
            usuario?.uid?.let { uid ->
                PerfilController.actualizarDatoUsuario(uid, "edad", it,
                    onSuccess = {
                        Toast.makeText(context, "Peso guardado", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = {
                        Toast.makeText(context, "Error al guardar peso", Toast.LENGTH_SHORT).show()
                    })
            }
            editandoCampo = null
        }

        EditableObjetivo(objetivo, "objetivo", editandoCampo, onEdit = { editandoCampo = it }) {
            objetivo = it
            usuario?.uid?.let { uid ->
                PerfilController.actualizarDatoUsuario(
                    uid, "objetivo", it,
                    onSuccess = {
                        Toast.makeText(context, "Objetivo guardado", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = {
                        Toast.makeText(context, "Error al guardar objetivo", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            editandoCampo = null
        }


        HorizontalDivider()

        val imc = calcularIMC(peso?.toFloatOrNull(), altura?.toFloatOrNull())
        imc?.let {
            BarraIMC(imc = it)
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                PerfilController.cerrarSesion(context) {
                    navController.navigate("login") {
                        popUpTo("inicio") { inclusive = true }
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth()
        ) {
            Text("Cerrar sesión")
        }
    }
}

@Composable
fun EditableDato(
    label: String,
    valor: String?,
    campo: String,
    editandoCampo: String?,
    onEdit: (String) -> Unit,
    onGuardar: (String) -> Unit
) {
    var nuevoValor by remember { mutableStateOf(valor ?: "") }

    if (editandoCampo == campo) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = nuevoValor,
                onValueChange = {
                    if (it.matches(Regex("""\d{0,3}([.,]?\d{0,2})?"""))) nuevoValor = it
                },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val limpio = nuevoValor.replace(',', '.')
                    if (limpio.isNotBlank() && limpio.toFloatOrNull() != null && limpio.toFloat() > 0) {
                        onGuardar(limpio)
                    }
                },
                enabled = nuevoValor.toFloatOrNull() != null && nuevoValor.toFloatOrNull()!! > 0
            ) {
                Text("Guardar")
            }
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEdit(campo) }
        ) {
            Text("$label: ${valor ?: "No definido"}", fontSize = 18.sp)
            Icon(Icons.Default.Edit, contentDescription = "Editar $label")
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditableObjetivo(
    valor: String?,
    campo: String,
    editandoCampo: String?,
    onEdit: (String) -> Unit,
    onGuardar: (String) -> Unit
) {
    val opciones = listOf("Perder peso", "Mantener peso", "Ganar masa")
    var expanded by remember { mutableStateOf(false) }
    var seleccionActual by remember { mutableStateOf(valor ?: opciones.first()) }

    if (editandoCampo == campo) {
        Column {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = seleccionActual,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Objetivo") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .menuAnchor() // NECESARIO para que funcione el menú
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    opciones.forEach { opcion ->
                        DropdownMenuItem(
                            text = { Text(opcion) },
                            onClick = {
                                seleccionActual = opcion
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = { onGuardar(seleccionActual) }) {
                Text("Guardar")
            }
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEdit(campo) }
        ) {
            Text("Objetivo: ${valor ?: "No definido"}", fontSize = 18.sp)
            Icon(Icons.Default.Edit, contentDescription = "Editar objetivo")
        }
    }
}


fun calcularIMC(peso: Float?, alturaCm: Float?): Float? {
    if (peso != null && alturaCm != null && alturaCm > 0) {
        val alturaM = alturaCm / 100f
        return peso / alturaM.pow(2)
    }
    return null
}

@Composable
fun BarraIMC(imc: Float) {
    val valoresMin = 12f
    val valoresMax = 40f
    val isDark = isSystemInDarkTheme()
    val flechaColor = if (isDark) Color.White else Color.Black

    val estado = when {
        imc < 18.5f -> "Bajo peso"
        imc < 25f -> "Peso normal"
        else -> "Sobrepeso"
    }

    val colorEstado = when {
        imc < 18.5f -> Color(0xFF42A5F5)
        imc < 25f -> Color(0xFF66BB6A)
        else -> Color(0xFFEF5350)
    }

    val barraAlto = 32.dp

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "IMC: ${"%.1f".format(imc)} - $estado",
            color = colorEstado,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Medimos el ancho real del dispositivo usando Layout
        var barraAnchoPx by remember { mutableStateOf(0f) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    barraAnchoPx = coordinates.size.width.toFloat()
                }
        ) {
            // Flecha encima
            if (barraAnchoPx > 0f) {
                val userX = ((imc - valoresMin) / (valoresMax - valoresMin)).coerceIn(0f, 1f) * barraAnchoPx
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Flecha IMC",
                    tint = flechaColor,
                    modifier = Modifier
                        .absoluteOffset(x = with(LocalContext.current.resources.displayMetrics) {
                            (userX / density).dp
                        }, y = (-16).dp)
                        .size(24.dp)
                )
            }

            // Barra de IMC
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barraAlto)
            ) {
                val ancho = size.width
                val alto = size.height

                val x18_5 = ((18.5f - valoresMin) / (valoresMax - valoresMin)) * ancho
                val x25 = ((25f - valoresMin) / (valoresMax - valoresMin)) * ancho

                drawRect(Color(0xFF42A5F5), Offset(0f, 0f), Size(x18_5, alto))
                drawRect(Color(0xFF66BB6A), Offset(x18_5, 0f), Size(x25 - x18_5, alto))
                drawRect(Color(0xFFEF5350), Offset(x25, 0f), Size(ancho - x25, alto))

                drawLine(Color.Black, Offset(x18_5, 0f), Offset(x18_5, alto), strokeWidth = 3f)
                drawLine(Color.Black, Offset(x25, 0f), Offset(x25, alto), strokeWidth = 3f)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("12", fontSize = 12.sp)
            Text("18.5", fontSize = 12.sp)
            Text("25", fontSize = 12.sp)
            Text("40", fontSize = 12.sp)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Bajo peso", fontSize = 12.sp, color = Color(0xFF42A5F5))
            Text("Normal", fontSize = 12.sp, color = Color(0xFF66BB6A))
            Text("Sobrepeso", fontSize = 12.sp, color = Color(0xFFEF5350))
        }
    }
}





