package com.github.warnastrophy.core.ui.features.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.github.warnastrophy.core.model.Hazard
import com.github.warnastrophy.core.util.BaseAndroidComposeTest
import com.github.warnastrophy.core.util.formatDate
import com.google.android.gms.maps.model.LatLng
import junit.framework.TestCase
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
  fun hazardsHaveCorrectIcons() {
    val hazard: MutableState<Hazard?> = mutableStateOf(null)

    composeTestRule.setContent {
      hazard.value?.let {
        HazardMarker(
            it,
            markerIconProvider = { _, _, _, _ -> null },
            polygonContent = {},
            markerInfoWindowContent = { _, _, _, _, content -> Box { content() } },
            iconContent = { icon, tint -> icon(tint) })
      }
    }

    hazardToTag.forEach { (haz, expectedTag) ->
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

  @Test
  fun formatSeveritySnippet_hidesZeroAndNull() {
    // severity == null -> no snippet
    val hazardNullSeverity =
        Hazard(
            id = 1,
            type = "FL",
            description = "Flood",
            country = null,
            date = null,
            severity = null,
            severityUnit = "ha",
            articleUrl = null,
            alertLevel = null,
            bbox = null,
            affectedZone = null,
            centroid = null)

    // severity == 0.0 -> no snippet (non-meaningful)
    val hazardZeroSeverity = hazardNullSeverity.copy(id = 2, severity = 0.0)

    // severity > 0 with unit
    val hazardPositiveWithUnit =
        hazardNullSeverity.copy(id = 3, severity = 1234.0, severityUnit = "ha")

    // severity > 0 without unit
    val hazardPositiveNoUnit = hazardNullSeverity.copy(id = 4, severity = 12.5, severityUnit = null)

    // Null + 0.0 should give null snippet
    TestCase.assertNull(formatSeveritySnippet(hazardNullSeverity))
    TestCase.assertNull(formatSeveritySnippet(hazardZeroSeverity))

    // Positive with unit
    val snippetWithUnit = formatSeveritySnippet(hazardPositiveWithUnit)
    TestCase.assertEquals("1234 ha", snippetWithUnit)

    // Positive without unit (keeps decimal with one digit)
    val snippetNoUnit = formatSeveritySnippet(hazardPositiveNoUnit)
    TestCase.assertEquals("12.5", snippetNoUnit)
  }

  @Test
  fun hazardInfoWindowContent_showsMeaningfulData_andArticleHint() {
    val hazard =
        Hazard(
            id = 10,
            type = "FR",
            description = "Wildfire in Region X",
            country = "CountryY",
            date = "2025-07-15",
            severity = 13590.0,
            severityUnit = "ha",
            articleUrl = "https://example.com/news/fire",
            alertLevel = 3.0,
            bbox = null,
            affectedZone = null,
            centroid = null)

    val snippet = formatSeveritySnippet(hazard)
    composeTestRule.setContent {
      HazardInfoWindowContent(
          hazard = hazard,
          title = hazard.description,
          snippet = snippet,
      )
    }

    // Description / title
    composeTestRule.onNodeWithText("Wildfire in Region X").assertIsDisplayed()

    // Severity text (from snippet) -- "13590 ha"
    composeTestRule.onNodeWithText("13590 ha").assertIsDisplayed()

    // Optional severityText: not set here, so we don't assert it

    // Date (formatted). We don't assert exact format, just that some date-like text exists.
    // If you know the exact output of formatDate("2025-07-15"), you can assert that instead.
    val formattedDate = formatDate("2025-07-15")
    composeTestRule.onNodeWithText(formattedDate).assertIsDisplayed()

    // Hint for article URL
    composeTestRule
        .onNodeWithText("Tap this bubble to open the full news article")
        .assertIsDisplayed()
  }

  @Test
  fun hazardMarker_doesNotShowZeroSeveritySnippet() {
    val hazard =
        Hazard(
            id = 42,
            type = "FL",
            description = "Minor flood",
            country = null,
            date = null,
            severity = 0.0, // non-meaningful → snippet must be hidden
            severityUnit = "ha",
            severityText = null,
            articleUrl = "https://example.com/flood",
            alertLevel = null,
            bbox = null,
            affectedZone = null,
            centroid = null)

    composeTestRule.setContent {
      HazardMarker(
          hazard = hazard,
          markerIconProvider = { _, _, _, _ -> null },
          polygonContent = {},
          markerInfoWindowContent = { _, _, _, _, content -> Box { content() } },
          iconContent = { _, _ -> })
    }

    // Title is visible
    composeTestRule.onNodeWithText("Minor flood").assertIsDisplayed()
    // "0.0" must NOT appear anywhere
    composeTestRule.onAllNodesWithText("0.0").assertCountEquals(0)
  }

  @Test
  fun hazardMarker_passesIconAndTintToIconContent() {
    val hazard =
        Hazard(
            id = 1,
            type = "XX",
            description = "Test hazard",
            country = null,
            date = null,
            severity = 5.0,
            severityUnit = "units",
            articleUrl = null,
            alertLevel = null,
            bbox = null,
            affectedZone = null,
            centroid = null)

    // capture what iconContent receives
    var receivedIcon: MapIcon? = null
    var receivedTint: Color? = null

    composeTestRule.setContent {
      HazardMarker(
          hazard = hazard,
          markerIconProvider = { _, _, _, _ -> null },
          polygonContent = {},
          markerInfoWindowContent = { _, _, _, _, content -> Box { content() } },
          iconContent = { icon, tint ->
            receivedIcon = icon
            receivedTint = tint
            icon(tint)
          })
    }

    composeTestRule.waitForIdle()

    // Icon is Unknown for type "XX"
    TestCase.assertEquals(MapIcon.Unknown, receivedIcon)
    // Tint color should be UNKNOWN (gray) since alertLevel is null
    TestCase.assertEquals(SeverityColors.UNKNOWN, receivedTint)

    // Icon composable is rendered with a Tint semantics; we use the Unknown tag
    val node = composeTestRule.onNodeWithTag(MapIcon.Unknown.tag)
    node.assertIsDisplayed()
    val tintColor = node.fetchSemanticsNode().config.getOrNull(Tint)
    TestCase.assertNotNull(tintColor)
  }

  @Test
  fun hazardInfoWindowContent_showsDescriptionSeverityDateAndHint() {
    val hazard =
        Hazard(
            id = 99,
            type = "FR",
            description = "Wildfire in Region X",
            country = "CountryY",
            date = "2025-07-15",
            severity = 13590.0,
            severityUnit = "ha",
            articleUrl = "https://example.com/news/fire",
            alertLevel = 3.0,
            bbox = null,
            affectedZone = null,
            centroid = null)

    val snippet = formatSeveritySnippet(hazard)

    composeTestRule.setContent {
      HazardInfoWindowContent(
          hazard = hazard,
          title = hazard.description,
          snippet = snippet,
      )
    }

    // Title
    composeTestRule.onNodeWithText("Wildfire in Region X").assertIsDisplayed()

    // Severity snippet
    composeTestRule.onNodeWithText("13590 ha").assertIsDisplayed()

    // Date (formatted)
    val formattedDate = formatDate("2025-07-15")
    composeTestRule.onNodeWithText(formattedDate).assertIsDisplayed()

    // Article hint
    composeTestRule
        .onNodeWithText("Tap this bubble to open the full news article")
        .assertIsDisplayed()
  }

  @Test
  fun hazardMarker_polygonHiddenWhenClickingOutside() {
    // Create a test polygon geometry
    val polygonGeoJson =
        """
        {
          "type": "Polygon",
          "coordinates": [[
            [10.0, 10.0],
            [10.0, 11.0],
            [11.0, 11.0],
            [11.0, 10.0],
            [10.0, 10.0]
          ]]
        }
        """
            .trimIndent()

    val affectedZone =
        com.github.warnastrophy.core.util.GeometryParser.convertRawGeoJsonGeometryToJTS(
            polygonGeoJson)

    val hazard =
        Hazard(
            id = 456,
            type = "EQ",
            description = "Test earthquake",
            country = null,
            date = null,
            severity = 6.5,
            severityUnit = "M",
            articleUrl = null,
            alertLevel = 3.0,
            bbox = null,
            centroid = null,
            affectedZone = affectedZone)

    // Track state with external control (simulating Map's selectedMarkerId)
    var selectedMarkerId by mutableStateOf<Int?>(null)
    var polygonCoordsReceived: List<LatLng>? = null

    composeTestRule.setContent {
      // Reset polygon coords on each recomposition
      polygonCoordsReceived = null
      HazardMarker(
          hazard = hazard,
          selectedMarkerId = selectedMarkerId,
          onMarkerSelected = { selectedMarkerId = it },
          markerIconProvider = { _, _, _, _ -> null },
          polygonContent = { coords -> polygonCoordsReceived = coords },
          markerInfoWindowContent = { _, _, _, _, content -> Box { content() } },
          iconContent = { _, _ -> })
    }

    composeTestRule.waitForIdle()

    // Initially, polygon should not be shown (polygonContent not called)
    TestCase.assertNull("Polygon should not be shown initially", polygonCoordsReceived)

    // Simulate marker selection
    selectedMarkerId = hazard.id

    composeTestRule.waitForIdle()

    // After selection, polygon should be shown (polygonContent called with coords)
    TestCase.assertNotNull(
        "Polygon should be shown after marker is selected", polygonCoordsReceived)
    TestCase.assertTrue("Polygon should have coordinates", polygonCoordsReceived!!.size > 1)

    // Simulate clicking outside (clearing selection)
    selectedMarkerId = null

    composeTestRule.waitForIdle()

    // After clicking outside, polygon should be hidden again
    TestCase.assertNull("Polygon should be hidden after clicking outside", polygonCoordsReceived)
  }

  @Test
  fun getSeverityColor_returnsLowForAlertLevel1() {
    val hazard = hazardBasedOnType("FL").copy(alertLevel = 1.0)
    TestCase.assertEquals(SeverityColors.LOW, getSeverityColor(hazard))
  }

  @Test
  fun getSeverityColor_returnsMediumForAlertLevel2() {
    val hazard = hazardBasedOnType("FL").copy(alertLevel = 2.0)
    TestCase.assertEquals(SeverityColors.MEDIUM, getSeverityColor(hazard))
  }

  @Test
  fun getSeverityColor_returnsHighForAlertLevel3() {
    val hazard = hazardBasedOnType("FL").copy(alertLevel = 3.0)
    TestCase.assertEquals(SeverityColors.HIGH, getSeverityColor(hazard))
  }

  @Test
  fun getSeverityColor_returnsUnknownForNullAlertLevel() {
    val hazard = hazardBasedOnType("FL").copy(alertLevel = null)
    TestCase.assertEquals(SeverityColors.UNKNOWN, getSeverityColor(hazard))
  }

  @Test
  fun getSeverityColor_returnsUnknownForOtherAlertLevels() {
    val hazard = hazardBasedOnType("FL").copy(alertLevel = 4.0)
    TestCase.assertEquals(SeverityColors.UNKNOWN, getSeverityColor(hazard))

    val hazard2 = hazardBasedOnType("FL").copy(alertLevel = 0.5)
    TestCase.assertEquals(SeverityColors.UNKNOWN, getSeverityColor(hazard2))
  }

  @Test
  fun hazardNewsImage_displaysFallbackWhenUrlIsNull() {
    val hazard = hazardBasedOnType("FL").copy(articleUrl = null, description = "Test flood")

    composeTestRule.setContent { HazardNewsImage(hazard = hazard) }

    composeTestRule.waitForIdle()
    // The image should be displayed (fallback)
    composeTestRule.onNodeWithContentDescription("Test flood").assertIsDisplayed()
  }

  @Test
  fun hazardNewsImage_displaysFallbackWhenUrlIsBlank() {
    val hazard = hazardBasedOnType("FL").copy(articleUrl = "   ", description = "Test flood blank")

    composeTestRule.setContent { HazardNewsImage(hazard = hazard) }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithContentDescription("Test flood blank").assertIsDisplayed()
  }

  @Test
  fun hazardNewsImage_displaysFallbackWhenUrlIsNotImageFormat() {
    val hazard =
        hazardBasedOnType("FL")
            .copy(articleUrl = "https://example.com/news/article", description = "Non-image URL")

    composeTestRule.setContent { HazardNewsImage(hazard = hazard) }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithContentDescription("Non-image URL").assertIsDisplayed()
  }

  @Test
  fun hazardNewsImage_usesDefaultDescriptionWhenDescriptionIsNull() {
    val hazard = hazardBasedOnType("FL").copy(articleUrl = null, description = null)

    composeTestRule.setContent { HazardNewsImage(hazard = hazard) }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithContentDescription("Hazard image").assertIsDisplayed()
  }

  @Test
  fun mapIcon_unknownUsesDefaultWarningIcon() {
    composeTestRule.setContent { MapIcon.Unknown.invoke(tint = Color.Red) }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(MapIcon.Unknown.tag).assertIsDisplayed()

    // Verify tint is set in semantics
    val node = composeTestRule.onNodeWithTag(MapIcon.Unknown.tag)
    val tintColor = node.fetchSemanticsNode().config.getOrNull(Tint)
    TestCase.assertEquals(Color.Red, tintColor)
  }

  @Test
  fun mapIcon_allIconsHaveCorrectTags() {
    TestCase.assertEquals("map_icon_tsunami", MapIcon.Tsunami.tag)
    TestCase.assertEquals("map_icon_drought", MapIcon.Drought.tag)
    TestCase.assertEquals("map_icon_earthquake", MapIcon.Earthquake.tag)
    TestCase.assertEquals("map_icon_fire", MapIcon.Fire.tag)
    TestCase.assertEquals("map_icon_flood", MapIcon.Flood.tag)
    TestCase.assertEquals("map_icon_cyclone", MapIcon.Cyclone.tag)
    TestCase.assertEquals("map_icon_volcano", MapIcon.Volcano.tag)
    TestCase.assertEquals("map_icon_unknown", MapIcon.Unknown.tag)
  }

  @Test
  fun mapIcon_allKnownIconsHaveResIds() {
    TestCase.assertNotNull(MapIcon.Tsunami.resId)
    TestCase.assertNotNull(MapIcon.Drought.resId)
    TestCase.assertNotNull(MapIcon.Earthquake.resId)
    TestCase.assertNotNull(MapIcon.Fire.resId)
    TestCase.assertNotNull(MapIcon.Flood.resId)
    TestCase.assertNotNull(MapIcon.Cyclone.resId)
    TestCase.assertNotNull(MapIcon.Volcano.resId)
    TestCase.assertNull(MapIcon.Unknown.resId)
  }

  @Test
  fun hazardInfoWindowContent_hidesArticleHintWhenNoUrl() {
    val hazard =
        Hazard(
            id = 100,
            type = "FL",
            description = "Flood without article",
            country = null,
            date = null,
            severity = 50.0,
            severityUnit = "km",
            articleUrl = null,
            alertLevel = 1.0,
            bbox = null,
            affectedZone = null,
            centroid = null)

    composeTestRule.setContent {
      HazardInfoWindowContent(
          hazard = hazard, title = hazard.description, snippet = formatSeveritySnippet(hazard))
    }

    composeTestRule.onNodeWithText("Flood without article").assertIsDisplayed()
    composeTestRule.onNodeWithText("50 km").assertIsDisplayed()
    // Article hint should NOT be present
    composeTestRule
        .onAllNodesWithText("Tap this bubble to open the full news article")
        .assertCountEquals(0)
  }

  @Test
  fun hazardInfoWindowContent_showsSeverityText() {
    val hazard =
        Hazard(
            id = 101,
            type = "EQ",
            description = "Earthquake with severity text",
            country = null,
            date = null,
            severity = 7.2,
            severityUnit = "M",
            severityText = "Magnitude 7.2 - Very strong",
            articleUrl = "https://example.com",
            alertLevel = 3.0,
            bbox = null,
            affectedZone = null,
            centroid = null)

    composeTestRule.setContent {
      HazardInfoWindowContent(
          hazard = hazard, title = hazard.description, snippet = formatSeveritySnippet(hazard))
    }

    composeTestRule.onNodeWithText("Magnitude 7.2 - Very strong").assertIsDisplayed()
  }

  @Test
  fun hazardInfoWindowContent_usesDefaultTitleWhenDescriptionNull() {
    val hazard =
        Hazard(
            id = 102,
            type = "FL",
            description = null,
            country = null,
            date = null,
            severity = null,
            severityUnit = null,
            articleUrl = null,
            alertLevel = null,
            bbox = null,
            affectedZone = null,
            centroid = null)

    composeTestRule.setContent {
      HazardInfoWindowContent(hazard = hazard, title = null, snippet = null)
    }

    // Should show default "Hazard" title
    composeTestRule.onNodeWithText("Hazard").assertIsDisplayed()
  }

  @Test
  fun hazardMarker_invokesOnInfoWindowClickCallback() {
    var onInfoWindowClickCalled = false
    val hazard = hazardBasedOnType("FL").copy(articleUrl = "https://example.com/article")

    composeTestRule.setContent {
      HazardMarker(
          hazard = hazard,
          onInfoWindowClick = { onInfoWindowClickCalled = true },
          markerIconProvider = { _, _, _, _ -> null }, // Avoid Google Maps initialization
          polygonContent = {},
          markerInfoWindowContent = { _, _, _, onInfoWindowClick, content ->
            onInfoWindowClick() // Simulate info window click - this calls our injected callback
            Box { content() }
          },
          iconContent = { _, _ -> })
    }

    composeTestRule.waitForIdle()
    TestCase.assertTrue("onInfoWindowClick should have been called", onInfoWindowClickCalled)
  }

  @Test
  fun hazardMarker_invokesOnMarkerClickCallback() {
    var markerClickId: Int? = null
    val hazard = hazardBasedOnType("FL").copy(id = 999)

    composeTestRule.setContent {
      HazardMarker(
          hazard = hazard,
          onMarkerSelected = { markerClickId = it },
          markerIconProvider = { _, _, _, _ -> null },
          polygonContent = {},
          markerInfoWindowContent = { _, _, onMarkerClick, _, content ->
            onMarkerClick() // Simulate marker click
            Box { content() }
          },
          iconContent = { _, _ -> })
    }

    composeTestRule.waitForIdle()
    TestCase.assertEquals(999, markerClickId)
  }

  @Test
  fun formatSeveritySnippet_handlesWholeNumbers() {
    val hazard = hazardBasedOnType("FL").copy(severity = 100.0, severityUnit = "km")
    TestCase.assertEquals("100 km", formatSeveritySnippet(hazard))
  }

  @Test
  fun formatSeveritySnippet_handlesDecimalNumbers() {
    val hazard = hazardBasedOnType("FL").copy(severity = 5.67, severityUnit = "M")
    TestCase.assertEquals("5.7 M", formatSeveritySnippet(hazard))
  }

  @Test
  fun formatSeveritySnippet_handlesEmptyUnit() {
    val hazard = hazardBasedOnType("FL").copy(severity = 42.0, severityUnit = "")
    TestCase.assertEquals("42", formatSeveritySnippet(hazard))
  }

  @Test
  fun formatSeveritySnippet_trimsUnitWhitespace() {
    val hazard = hazardBasedOnType("FL").copy(severity = 10.0, severityUnit = "  ha  ")
    TestCase.assertEquals("10 ha", formatSeveritySnippet(hazard))
  }
}
