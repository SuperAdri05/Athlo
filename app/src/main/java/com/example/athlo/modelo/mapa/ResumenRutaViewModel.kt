package com.example.athlo.modelo.mapa

import androidx.lifecycle.ViewModel

class ResumenRutaViewModel : ViewModel() {
    var resumen: RegistroRuta? = null
    var puntosRuta: List<PuntoRuta> = emptyList()
}
