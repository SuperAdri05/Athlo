package com.example.athlo.vista

// Imports necesarios para navegación, UI y modelo
import android.content.Intent
import android.content.SharedPreferences
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.athlo.controlador.MapaController
import com.example.athlo.controlador.SesionController
import com.example.athlo.modelo.entreno.Entrenamiento
import com.example.athlo.modelo.entreno.EntrenoViewModel
import com.example.athlo.modelo.entreno.limpiarEstadoEntreno
import com.example.athlo.modelo.entreno.restaurarEntrenoSiExiste
import com.example.athlo.modelo.mapa.ResumenRutaViewModel
import com.example.athlo.servicio.EntrenoService
import com.example.athlo.ui.theme.AthloTheme
import com.example.athlo.vista.entreno.HistorialResumenesEntreno
import com.example.athlo.vista.entreno.PantallaAsignarEjercicios
import com.example.athlo.vista.entreno.PantallaCrearEntreno
import com.example.athlo.vista.entreno.PantallaDetalleEntreno
import com.example.athlo.vista.entreno.PantallaEjecutarEntreno
import com.example.athlo.vista.entreno.PantallaEntreno
import com.example.athlo.vista.entreno.PantallaResumenEntreno
import com.example.athlo.vista.mapa.PantallaHistorialRutas
import com.example.athlo.vista.mapa.PantallaMapa
import com.example.athlo.vista.mapa.PantallaResumenRuta
import com.example.athlo.vista.registro.PantallaLogin
import com.example.athlo.vista.registro.PantallaPerfil
import com.example.athlo.vista.registro.PantallaRegistro
import com.example.athlo.vista.registro.PantallaVerificacionCorreo
import kotlinx.coroutines.launch

// Representa cada pestaña de navegación inferior (ruta + icono)
sealed class NavItem(val route: String, val icon: ImageVector) {
    data object Mapa : NavItem("mapa", Icons.AutoMirrored.Filled.DirectionsRun)
    data object Inicio : NavItem("inicio", Icons.Default.Home)
    data object Entreno : NavItem("entreno", Icons.Default.FitnessCenter)
}

@Composable
fun PantallaPrincipal() {
    val entrenoViewModel: EntrenoViewModel = viewModel()
    val navController = rememberNavController()
    val context = LocalContext.current
    val items = listOf(NavItem.Mapa, NavItem.Inicio, NavItem.Entreno)
    val listaEntrenos = remember { mutableStateOf<List<Entrenamiento>>(emptyList()) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var startDestination by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        SesionController.obtenerRutaInicio(context) { ruta ->
            startDestination = ruta

            // Reanudar entreno GPS en segundo plano si existe
            val prefs: SharedPreferences = context.getSharedPreferences("entreno", 0)
            val entrenoId = prefs.getString("id", null)
            if (entrenoId != null &&
                    MapaController.estadoEntreno != MapaController.EstadoEntreno.Detenido
            ) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "📍 Tienes un seguimiento GPS activo",
                        actionLabel = "Vale"
                    )
                }
            }

            restaurarEntrenoSiExiste(context, entrenoViewModel) {
                if (
                    entrenoViewModel.entrenamientoEnCurso != null &&
                    entrenoViewModel.estaMinimizado &&
                    entrenoViewModel.resumenActual == null
                ) {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "📌 Tienes un entrenamiento activo minimizado",
                            actionLabel = "Vale"
                        )
                    }
                }
            }
        }
    }

    val rutasProtegidas = listOf("inicio", "mapa", "entreno", "perfil", "detalle_entreno", "crear_entreno", "asignar_ejercicios")
    val resumenViewModel: ResumenRutaViewModel = viewModel()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (currentRoute in rutasProtegidas) {
                NavigationBar {
                    items.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(imageVector = item.icon, contentDescription = item.route) },
                            alwaysShowLabel = false
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        if (startDestination != null) {
            NavHost(
                navController = navController,
                startDestination = startDestination!!,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("login") { PantallaLogin(navController) }
                composable("registro") { PantallaRegistro(navController) }
                composable("verificar") { PantallaVerificacionCorreo(navController) }
                composable("datos_iniciales") { PantallaDatosIniciales(navController) }
                composable(NavItem.Inicio.route) { PantallaInicio(navController) }
                composable(NavItem.Entreno.route) { PantallaEntreno(navController, listaEntrenos, entrenoViewModel) }
                composable("perfil") { PantallaPerfil(navController) }
                composable("detalle_entreno") {
                    PantallaDetalleEntreno(
                        viewModel = entrenoViewModel,
                        navController = navController,
                        onVolver = { navController.navigate("entreno") },
                        onIniciar = { navController.navigate("pantalla_ejecutar_entreno") },
                        onAgregarEjercicios = { navController.navigate("asignar_ejercicios") }
                    )
                }
                composable("pantalla_ejecutar_entreno") {
                    PantallaEjecutarEntreno(
                        viewModel = entrenoViewModel,
                        onVolver = { navController.popBackStack("entreno", inclusive = false) },
                        onIrResumen = { resumen ->
                            entrenoViewModel.resumenActual = resumen
                            navController.navigate("resumen_entreno")
                        }
                    )
                }
                composable("historial_rutas") { PantallaHistorialRutas(navController, resumenViewModel) }
                composable("resumen_ruta") { PantallaResumenRuta(navController, resumenViewModel) }
                composable(NavItem.Mapa.route) { PantallaMapa(navController, resumenViewModel) }
                composable("crear_entreno") {
                    PantallaCrearEntreno(
                        viewModel = entrenoViewModel,
                        onContinuar = { navController.navigate("asignar_ejercicios") },
                        onCancelar = { navController.popBackStack() }
                    )
                }
                composable("asignar_ejercicios") {
                    PantallaAsignarEjercicios(
                        viewModel = entrenoViewModel,
                        onBack = {
                            navController.popBackStack() },
                        onFinalizar = { nuevaLista ->
                            entrenoViewModel.entrenamientoActual =
                                entrenoViewModel.entrenamientoActual?.copy(ejercicios = nuevaLista)
                            entrenoViewModel.entrenamientoSeleccionado =
                                entrenoViewModel.entrenamientoActual
                            navController.navigate("detalle_entreno")
                        }
                    )
                }
                composable("historial_entrenos") {
                    HistorialResumenesEntreno(navController, entrenoViewModel)
                }

                composable("pantalla_resumen_entreno") {
                    entrenoViewModel.resumenActual?.let {
                        PantallaResumenEntreno(
                            resumen = it,
                            onCancelar = { navController.popBackStack() },
                            onGuardar = { navController.popBackStack() }
                        )
                    }
                }
                composable("chat_ia") {
                    PantallaChatIA(navController)
                }



                composable("resumen_entreno") {
                    val resumen = entrenoViewModel.resumenActual
                    if (resumen != null) {
                        PantallaResumenEntreno(
                            resumen = resumen,
                            onCancelar = { navController.popBackStack() },
                            onGuardar = {
                                entrenoViewModel.resetEntreno()
                                entrenoViewModel.entrenamientoEnCurso = null
                                entrenoViewModel.estaMinimizado = false
                                entrenoViewModel.resumenActual = null
                                limpiarEstadoEntreno(context)
                                context.stopService(Intent(context, EntrenoService::class.java))
                                navController.popBackStack("entreno", false)
                            }
                        )
                    } else {
                        Text("Cargando...")
                    }
                }
            }
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