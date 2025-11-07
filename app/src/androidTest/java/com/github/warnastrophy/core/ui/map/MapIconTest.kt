package com.github.warnastrophy.core.ui.map

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import com.github.warnastrophy.core.model.Hazard
import com.github.warnastrophy.core.ui.util.BaseAndroidComposeTest
import junit.framework.TestCase.assertNotNull
import org.junit.Test

class MapIconTest : BaseAndroidComposeTest() {
  val tsunami = hazardBasedOnType("TS")
  val drought = hazardBasedOnType("DR")
  val earthquake = hazardBasedOnType("EQ")
  val fire = hazardBasedOnType("FR")
  val flood = hazardBasedOnType("FL")
  val cyclone = hazardBasedOnType("TC")
  val volcano = hazardBasedOnType("VO")
  val randomOther = hazardBasedOnType("XY")

  val hazardToTag: Map<Hazard, String> =
      mapOf(
          tsunami to MapIcon.Tsunami.tag,
          drought to MapIcon.Drought.tag,
          earthquake to MapIcon.Earthquake.tag,
          fire to MapIcon.Fire.tag,
          flood to MapIcon.Flood.tag,
          cyclone to MapIcon.Cyclone.tag,
          volcano to MapIcon.Volcano.tag,
          randomOther to MapIcon.Unknown.tag)

  @Test
  fun iconsDisplay() {
    composeTestRule.setContent {
      Column {
        MapIcon.Tsunami.invoke()
        MapIcon.Drought.invoke()
        MapIcon.Earthquake.invoke()
        MapIcon.Fire.invoke()
        MapIcon.Flood.invoke()
        MapIcon.Cyclone.invoke()
        MapIcon.Volcano.invoke()
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(MapIcon.Tsunami.tag).assertIsDisplayed()
  }

  @Test
  fun intensityIsCoherent() {
    val lowHazard =
        Hazard(
            id = 0,
            type = "XX",
            description = null,
            country = null,
            date = null,
            severity = 1.0,
            severityUnit = "unit",
            articleUrl = null,
            alertLevel = null,
            bbox = null,
            affectedZone = null,
            centroid = null)

    val highHazard = lowHazard.copy(severity = 10.0)

    val hazard = mutableStateOf<Hazard?>(null)
    val severities = mapOf("XX" to Pair(lowHazard.severity!!, highHazard.severity!!))

    composeTestRule.setContent {
      hazard.value?.let {
        HazardMarker(it, severities, markerContent = { _, _, _, content -> Box { content() } })
      }
    }

    hazard.value = lowHazard
    composeTestRule.waitForIdle()
    val lowNode = composeTestRule.onNodeWithTag(MapIcon.Unknown.tag)
    lowNode.assertIsDisplayed()
    val lowNodeIntensity = lowNode.fetchSemanticsNode().config.getOrNull(Tint)
    assertNotNull(lowNodeIntensity)

    hazard.value = highHazard
    composeTestRule.waitForIdle()
    val highNode = composeTestRule.onNodeWithTag(MapIcon.Unknown.tag)
    highNode.assertIsDisplayed()
    val highNodeIntensity = highNode.fetchSemanticsNode().config.getOrNull(Tint)
    assertNotNull(highNodeIntensity)

    val lowGrayscale =
        (lowNodeIntensity!!.red + lowNodeIntensity.green + lowNodeIntensity.blue) / 3f
    val highGrayscale =
        (highNodeIntensity!!.red + highNodeIntensity.green + highNodeIntensity.blue) / 3f
    assert(highGrayscale < lowGrayscale)
  }

  @Test
  fun hazardsHaveCorrectIcons() {
    val hazard: MutableState<Hazard?> = mutableStateOf(null)

    composeTestRule.setContent {
      hazard.value?.let {
        HazardMarker(it, emptyMap(), markerContent = { _, _, _, content -> Box { content() } })
      }
    }

    hazardToTag.forEach { (haz, expectedTag) ->
      Log.d("MapIconTest", "Testing hazard type ${haz.type} expecting tag $expectedTag")
      hazard.value = haz
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithContentDescription(expectedTag).assertIsDisplayed()
    }
  }

  private fun hazardBasedOnType(type: String?): Hazard {
    return Hazard(
        id = 0,
        type = type,
        description = null,
        country = null,
        date = null,
        severity = null,
        severityUnit = null,
        articleUrl = null,
        bbox = null,
        affectedZone = null,
        alertLevel = null,
        centroid = null)
  }
}
