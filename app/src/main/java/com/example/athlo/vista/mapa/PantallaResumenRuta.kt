package com.example.athlo.vista.mapa

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.athlo.R
import com.example.athlo.modelo.mapa.PuntoRuta
import com.example.athlo.modelo.mapa.ResumenRutaViewModel
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.iconSize
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.utils.BitmapUtils

@SuppressLint("SimpleDateFormat")
@Composable
fun PantallaResumenRuta(navController: NavController, viewModel: ResumenRutaViewModel) {
    val resumen = viewModel.resumen
    val puntos = viewModel.puntosRuta
    val context = LocalContext.current

    if (resumen == null || puntos.isEmpty()) {
        Toast.makeText(context, "⚠️ El entrenamiento debe durar al menos 10 segundos para generar un resumen.", Toast.LENGTH_LONG).show()
        navController.popBackStack()
        return
    }

    val duracionMin = resumen.duracionSegundos / 60
    val duracionSeg = resumen.duracionSegundos % 60
    val distanciaKm = resumen.distanciaMetros / 1000f
    val velocidadMedia = if (resumen.duracionSegundos > 0) {
        (distanciaKm / (resumen.duracionSegundos / 3600f))
    } else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Encabezado con icono y logo
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_logo),
                contentDescription = "Logo Athlo",
                modifier = Modifier
                    .height(100.dp)
                    .padding(end = 12.dp)
            )
        }

        // Mapa
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .padding(horizontal = 8.dp)
                .shadow(8.dp, shape = RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
        ) {
            MapaConRuta(puntos)

        }
        Text(
            text = "📊 Resultados",
            fontSize = 36.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.headlineSmall
        )

        // Contenido centrado en pantalla
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "📅 ${java.text.SimpleDateFormat("dd MMMM, yyyy").format(java.util.Date(resumen.fecha))}",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 1.dp)

                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("📏 Distancia", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                            Text("${"%.2f".format(distanciaKm)} km", fontSize = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                        VerticalDivider(
                            modifier = Modifier
                                .height(64.dp)
                                .width(1.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("⏱️ Tiempo", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                            Text("${duracionMin}m ${duracionSeg}s", fontSize = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("🚀 Vel. media", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                            Text("${"%.2f".format(velocidadMedia)} km/h", fontSize = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                        VerticalDivider(
                            modifier = Modifier
                                .height(64.dp)
                                .width(1.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("🔥 Calorías", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                            Text("${resumen.caloriasEstimadas} kcal", fontSize = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 1.dp)

                }
            }
        }
    }
}



@Composable
fun MapaConRuta(puntos: List<PuntoRuta>) {
    val context = LocalContext.current

    AndroidView(
        factory = {
            MapView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    400
                )

                getMapAsync { map ->
                    map.setStyle("https://api.maptiler.com/maps/019644e0-aa3e-76fa-9d07-f2a7846f6f9a/style.json?key=Q9rFfJJB0EDEU0CRyJie") {

                        val startIcon = BitmapUtils.getBitmapFromDrawable(
                            androidx.core.content.res.ResourcesCompat.getDrawable(
                                context.resources, R.drawable.ic_start, null
                            )!!
                        )
                        if (startIcon != null) map.style?.addImage("marker-green", startIcon)

                        val finishIcon = BitmapUtils.getBitmapFromDrawable(
                            androidx.core.content.res.ResourcesCompat.getDrawable(
                                context.resources, R.drawable.ic_finish, null
                            )!!
                        )
                        if (finishIcon != null) map.style?.addImage("marker-red", finishIcon)

                        if (puntos.isNotEmpty()) {
                            val posInicial = LatLng(puntos.first().latitud, puntos.first().longitud)
                            map.moveCamera(
                                org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(posInicial, 15.0)
                            )
                        }

                        val coordinates = JSONArray()
                        puntos.forEach {
                            coordinates.put(JSONArray().put(it.longitud).put(it.latitud))
                        }

                        val geoJsonRuta = JSONObject().apply {
                            put("type", "FeatureCollection")
                            put("features", JSONArray().put(JSONObject().apply {
                                put("type", "Feature")
                                put("geometry", JSONObject().apply {
                                    put("type", "LineString")
                                    put("coordinates", coordinates)
                                })
                            }))
                        }

                        val sourceRuta = GeoJsonSource("ruta", geoJsonRuta.toString())
                        map.style?.addSource(sourceRuta)

                        val layerRuta = LineLayer("capa_ruta", "ruta").withProperties(
                            lineColor("#3F51B5"),
                            lineWidth(5f)
                        )
                        map.style?.addLayer(layerRuta)

                        val inicioSource = GeoJsonSource("inicio", JSONObject().apply {
                            put("type", "Feature")
                            put("geometry", JSONObject().apply {
                                put("type", "Point")
                                put("coordinates", JSONArray().put(puntos.first().longitud).put(puntos.first().latitud))
                            })
                        }.toString())
                        map.style?.addSource(inicioSource)

                        val inicioLayer = SymbolLayer("inicio_layer", "inicio")
                            .withProperties(iconImage("marker-green"), iconSize(0.07f))
                        map.style?.addLayer(inicioLayer)

                        val finSource = GeoJsonSource("fin", JSONObject().apply {
                            put("type", "Feature")
                            put("geometry", JSONObject().apply {
                                put("type", "Point")
                                put("coordinates", JSONArray().put(puntos.last().longitud).put(puntos.last().latitud))
                            })
                        }.toString())
                        map.style?.addSource(finSource)

                        val finLayer = SymbolLayer("fin_layer", "fin")
                            .withProperties(iconImage("marker-red"), iconSize(0.07f))
                        map.style?.addLayer(finLayer)
                    }
                }
            }
        },
        modifier = Modifier.fillMaxWidth().height(300.dp)
    )
}
