package com.example.athlo.vista

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.athlo.R
import com.example.athlo.controlador.InicioController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PantallaInicio(navController: NavController) {
    val context = LocalContext.current
    val usuario = FirebaseAuth.getInstance().currentUser
    val avatarNombre = usuario?.photoUrl?.toString() ?: "avatar_gato"

    val avatarResId = when (avatarNombre) {
        "avatar_perro" -> R.drawable.avatar_perro
        "avatar_mono" -> R.drawable.avatar_mono
        "avatar_conejo" -> R.drawable.avatar_conejo
        "avatar_oso" -> R.drawable.avatar_oso
        "avatar_zorro" -> R.drawable.avatar_zorro
        else -> R.drawable.avatar_gato
    }

    var metaDiaria by remember { mutableIntStateOf(2000) }
    var consumidasHoy by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        InicioController.escucharMetaDiaria(context) { metaDiaria = it }
        withContext(Dispatchers.IO) {
            val kcal = InicioController.obtenerCaloriasConsumidasHoy(context)
            withContext(Dispatchers.Main) {
                consumidasHoy = kcal
            }
        }
    }

    val progreso = (consumidasHoy.toFloat() / metaDiaria).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopEnd),
            horizontalArrangement = Arrangement.End
        ) {
            Image(
                painter = painterResource(id = avatarResId),
                contentDescription = "Avatar del usuario",
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .clickable { navController.navigate("perfil") }
            )
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "¡Bienvenido!",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(32.dp))

            CaloriasFuego(progreso = progreso)

            Text(
                text = "$consumidasHoy / $metaDiaria kcal",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            // Mensaje animado de felicitación al alcanzar la meta
            androidx.compose.animation.AnimatedVisibility(visible = progreso >= 1f) {
                Text(
                    text = "¡Felicidades, has completado tu meta diaria!",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
                    ),
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
        // Botón flotante de acceso al chat IA
        androidx.compose.material3.FloatingActionButton(
            onClick = { navController.navigate("chat_ia") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Chat,
                contentDescription = "Chat IA",
                tint = Color.White
            )
        }

    }
}


@Composable
fun CaloriasFuego(progreso: Float) {
    val progresoAnimado by animateFloatAsState(targetValue = progreso, label = "ProgresoCalorias")
    val colorUnificado = if (isSystemInDarkTheme())
        Color(0xFF4FC3F7) // Azul clarito para modo oscuro
    else
        Color(0xFF13445D) // Azul más oscuro para modo claro

    Box(
        modifier = Modifier.size(360.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(260.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_logo),
                    contentDescription = "Logo relleno",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    colorFilter = ColorFilter.tint(colorUnificado)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(1f - progresoAnimado)
                        .background(MaterialTheme.colorScheme.background)
                )
            }

            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(alpha = 0.15f),
                colorFilter = ColorFilter.tint(colorUnificado)
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 20f
            val halfStroke = strokeWidth / 2
            val arcSize = size.copy(
                width = size.minDimension - strokeWidth,
                height = size.minDimension - strokeWidth
            )
            val arcOffset = Offset(halfStroke, halfStroke)

            drawArc(
                color = colorUnificado.copy(alpha = 0.2f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = arcOffset,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )
            drawArc(
                color = colorUnificado,
                startAngle = -90f,
                sweepAngle = progresoAnimado * 360f,
                useCenter = false,
                topLeft = arcOffset,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}


