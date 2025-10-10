package com.github.warnastrophy.core.model.util

import com.google.android.gms.maps.model.LatLng

data class Location(val latitude: Double, val longitude: Double) {
  companion object {
    fun toLatLng(location: Location): LatLng {
      return LatLng(location.latitude, location.longitude)
    }
  }
}
