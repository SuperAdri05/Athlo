package com.example.athlo.vista.entreno

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.athlo.modelo.entreno.EjercicioDisponible

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaInformacionEjercicio(
    ejercicio: EjercicioDisponible,
    navController: NavHostController
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = ejercicio.nombre) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Imagen animada del ejercicio (GIF)
            AsyncImage(
                model = ejercicio.foto,
                contentDescription = "GIF del ejercicio",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            // Nombre y grupo muscular
            Text(
                text = ejercicio.nombre,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Músculo: ${ejercicio.musculo}",
                style = MaterialTheme.typography.bodyMedium
            )

            // Descripción
            Text(
                text = ejercicio.descripcion,
                style = MaterialTheme.typography.bodyLarge
            )

            // Botón para ver video (opcional)
            if (ejercicio.video.isNotBlank()) {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ejercicio.video))
                        context.startActivity(intent)
                    }
                ) {
                    Text("Ver video completo")
                }
            }
        }
    }
}
