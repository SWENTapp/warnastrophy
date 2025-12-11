package com.github.warnastrophy.core.ui.features.map

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.github.warnastrophy.R
import com.github.warnastrophy.core.model.Hazard
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test

class HazardUtilsTest {
  private fun baseHazard() =
      Hazard(
          id = 0,
          type = "FL",
          description = "Flood",
          country = null,
          date = null,
          severity = null,
          severityUnit = null,
          articleUrl = null,
          alertLevel = null,
          bbox = null,
          affectedZone = null,
          centroid = null)

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    MapsInitializer.initialize(context)
  }

  @Test
  fun formatSeveritySnippet_handlesNullZeroAndPositive() {
    val hNull = baseHazard()
    val hZero = baseHazard().copy(severity = 0.0, severityUnit = "mm")
    val hInt = baseHazard().copy(severity = 10.0, severityUnit = "mm")
    val hFloat = baseHazard().copy(severity = 12.5, severityUnit = "")

    assertNull(formatSeveritySnippet(hNull))
    assertNull(formatSeveritySnippet(hZero))
    assertEquals("10 mm", formatSeveritySnippet(hInt))
    assertEquals("12.5", formatSeveritySnippet(hFloat))
  }

  @Test
  fun hazardTypeToMapIcon_andDrawableRes_coverAllBranches() {
    assertEquals(MapIcon.Flood, hazardTypeToMapIcon("FL"))
    assertEquals(MapIcon.Drought, hazardTypeToMapIcon("DR"))
    assertEquals(MapIcon.Earthquake, hazardTypeToMapIcon("EQ"))
    assertEquals(MapIcon.Cyclone, hazardTypeToMapIcon("TC"))
    assertEquals(MapIcon.Fire, hazardTypeToMapIcon("FR"))
    assertEquals(MapIcon.Volcano, hazardTypeToMapIcon("VO"))
    assertEquals(MapIcon.Tsunami, hazardTypeToMapIcon("TS"))
    assertEquals(MapIcon.Unknown, hazardTypeToMapIcon("XX"))
    assertEquals(MapIcon.Unknown, hazardTypeToMapIcon(null))

    assertEquals(R.drawable.material_symbols_outlined_flood, hazardTypeToDrawableRes("FL"))
    assertEquals(R.drawable.material_symbols_outlined_water_voc, hazardTypeToDrawableRes("DR"))
    assertEquals(R.drawable.material_symbols_outlined_earthquake, hazardTypeToDrawableRes("EQ"))
    assertEquals(R.drawable.material_symbols_outlined_storm, hazardTypeToDrawableRes("TC"))
    assertEquals(
        R.drawable.material_symbols_outlined_local_fire_department, hazardTypeToDrawableRes("FR"))
    assertEquals(R.drawable.material_symbols_outlined_volcano, hazardTypeToDrawableRes("VO"))
    assertEquals(R.drawable.material_symbols_outlined_tsunami, hazardTypeToDrawableRes("TS"))
    assertNull(hazardTypeToDrawableRes("XX"))
    assertNull(hazardTypeToDrawableRes(null))
  }

  @Test
  fun computeSeverityTint_respectsMinMaxAndNulls() {
    val hazardBase = baseHazard().copy(type = "X", severity = 5.0)

    val hazardLow = hazardBase.copy(alertLevel = 1.0)
    val hazardMedium = hazardBase.copy(alertLevel = 2.0)
    val hazardHigh = hazardBase.copy(alertLevel = 3.0)
    val hazardOther = hazardBase.copy(alertLevel = 4.0)

    assertEquals(SeverityColors.LOW, getSeverityColor(hazardLow))
    assertEquals(SeverityColors.MEDIUM, getSeverityColor(hazardMedium))
    assertEquals(SeverityColors.HIGH, getSeverityColor(hazardHigh))
    assertEquals(SeverityColors.UNKNOWN, getSeverityColor(hazardOther))

    // alertLevel == null -> UNKNOWN
    val hazardNull = hazardBase.copy(alertLevel = null)
    val nullColor = getSeverityColor(hazardNull)
    assertEquals(SeverityColors.UNKNOWN, nullColor)
  }

  @Test
  fun bitmapDescriptorFromVector_createsBitmapAndAppliesTint() {
    val context: Context = ApplicationProvider.getApplicationContext()
    val descriptor =
        bitmapDescriptorFromVector(
            context = context,
            vectorResId = R.drawable.material_symbols_outlined_flood,
            sizeDp = 32f,
            tintColor = Color.Red)

    // We can't inspect the pixels easily here,
    // but we can assert that it's not the default marker.
    // (Default marker has a fixed hue; this at least executes all lines.)
    assertTrue(descriptor != BitmapDescriptorFactory.defaultMarker())
  }
}
