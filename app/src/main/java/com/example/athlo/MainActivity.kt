package com.example.athlo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.athlo.ui.theme.AthloTheme
import com.example.athlo.vista.PantallaPrincipal
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Inicializar MapLibre (sin clave, usando servidor público)
        MapLibre.getInstance(
            applicationContext,
            "",
            WellKnownTileServer.MapLibre
        )

        // ✅ Pedir permisos de ubicación si no se han concedido
        requestLocationPermission()

        enableEdgeToEdge()
        setContent {
            AthloTheme {
                PantallaPrincipal()
            }
        }
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AthloTheme {
        PantallaPrincipal()
    }
}
