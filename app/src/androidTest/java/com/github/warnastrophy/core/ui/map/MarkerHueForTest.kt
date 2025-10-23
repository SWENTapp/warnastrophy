package com.github.warnastrophy.core.ui.map

import com.google.android.gms.maps.model.BitmapDescriptorFactory
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkerHueForTest {

  /** Given the map type "FL", verify that the `markerHueFor` function returns the hue for green. */
  @Test
  fun hue_for_FL_is_green() {
    assertEquals(BitmapDescriptorFactory.HUE_GREEN, markerHueFor("FL"), 0f)
  }

  /**
   * Given the map type "DR", verify that the `markerHueFor` function returns the hue for orange.
   */
  @Test
  fun hue_for_DR_is_orange() {
    assertEquals(BitmapDescriptorFactory.HUE_ORANGE, markerHueFor("DR"), 0f)
  }

  /** Given the map type "WC", verify that the `markerHueFor` function returns the hue for blue. */
  @Test
  fun hue_for_WC_is_blue() {
    assertEquals(BitmapDescriptorFactory.HUE_BLUE, markerHueFor("WC"), 0f)
  }

  /** Given the map type "EQ", verify that the `markerHueFor` function returns the hue for red. */
  @Test
  fun hue_for_EQ_is_red() {
    assertEquals(BitmapDescriptorFactory.HUE_RED, markerHueFor("EQ"), 0f)
  }

  /**
   * Given the map type "TC", verify that the `markerHueFor` function returns the hue for yellow.
   */
  @Test
  fun hue_for_TC_is_yellow() {
    assertEquals(BitmapDescriptorFactory.HUE_YELLOW, markerHueFor("TC"), 0f)
  }

  /**
   * Given a null or unknown map type, verify that the `markerHueFor` function returns the default
   * hue (AZURE).
   */
  @Test
  fun hue_for_unknown_or_null_is_azure() {
    assertEquals(BitmapDescriptorFactory.HUE_AZURE, markerHueFor(null), 0f)
    assertEquals(BitmapDescriptorFactory.HUE_AZURE, markerHueFor("UNKNOWN"), 0f)
  }

  /**
   * Given a set of map types, verify that the `markerHueFor` function returns the expected hues.
   */
  @Test
  fun markerHueFor_mapsTypes_toExpectedHues() {
    assertEquals(BitmapDescriptorFactory.HUE_GREEN, markerHueFor("FL"))
    assertEquals(BitmapDescriptorFactory.HUE_ORANGE, markerHueFor("DR"))
    assertEquals(BitmapDescriptorFactory.HUE_BLUE, markerHueFor("WC"))
    assertEquals(BitmapDescriptorFactory.HUE_RED, markerHueFor("EQ"))
    assertEquals(BitmapDescriptorFactory.HUE_YELLOW, markerHueFor("TC"))

    // Unknown / null → default (AZURE)
    assertEquals(BitmapDescriptorFactory.HUE_AZURE, markerHueFor("XYZ"))
    assertEquals(BitmapDescriptorFactory.HUE_AZURE, markerHueFor(null))
  }
}
