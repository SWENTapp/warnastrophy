package com.github.warnastrophy.core.ui.map

import com.google.android.gms.maps.model.BitmapDescriptorFactory
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkerHueForTest {

  @Test
  fun hue_for_FL_is_green() {
    assertEquals(BitmapDescriptorFactory.HUE_GREEN, markerHueFor("FL"), 0f)
  }

  @Test
  fun hue_for_DR_is_orange() {
    assertEquals(BitmapDescriptorFactory.HUE_ORANGE, markerHueFor("DR"), 0f)
  }

  @Test
  fun hue_for_WC_is_blue() {
    assertEquals(BitmapDescriptorFactory.HUE_BLUE, markerHueFor("WC"), 0f)
  }

  @Test
  fun hue_for_EQ_is_red() {
    assertEquals(BitmapDescriptorFactory.HUE_RED, markerHueFor("EQ"), 0f)
  }

  @Test
  fun hue_for_TC_is_yellow() {
    assertEquals(BitmapDescriptorFactory.HUE_YELLOW, markerHueFor("TC"), 0f)
  }

  @Test
  fun hue_for_unknown_or_null_is_azure() {
    assertEquals(BitmapDescriptorFactory.HUE_AZURE, markerHueFor(null), 0f)
    assertEquals(BitmapDescriptorFactory.HUE_AZURE, markerHueFor("UNKNOWN"), 0f)
  }
}
