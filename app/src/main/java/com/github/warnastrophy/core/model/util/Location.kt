package com.github.warnastrophy.core.model.util

import com.google.android.gms.maps.model.LatLng

data class Location(val latitude: Double, val longitude: Double, val name: String? = null) {

  companion object {
    private const val earthRadiusKm = 6371.009

    fun toLatLng(location: Location): LatLng {
      return LatLng(location.latitude, location.longitude)
    }
  }
}

fun debugPrint(msg: String) {
  val file = java.io.File("debug.txt")
  file.appendText(msg + "\n")
}
