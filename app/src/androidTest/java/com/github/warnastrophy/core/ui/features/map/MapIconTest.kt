package com.github.warnastrophy.core.ui.features.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
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
        HazardMarker(it, emptyMap(), markerContent = { _, _, _, content -> Box { content() } })
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

    // Must contain an entry for the hazard type, otherwise computeSeverityTint() throws
    val severities = mapOf("FL" to (0.0 to 10.0))

    composeTestRule.setContent {
      HazardMarker(
          hazard = hazard,
          severities = severities,
          markerContent = { _, title, snippet, _ ->
            HazardInfoWindowContent(
                hazard = hazard,
                title = title,
                snippet = snippet,
            )
          })
    }

    // Title is visible
    composeTestRule.onNodeWithText("Minor flood").assertIsDisplayed()
    // "0.0" must NOT appear anywhere
    composeTestRule.onAllNodesWithText("0.0").assertCountEquals(0)
  }

  @Test
  fun hazardMarker_passesLocationTitleSnippetAndTintToMarkerContent() {
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

    val severities = mapOf("XX" to (1.0 to 9.0))

    // capture what markerContent receives
    var receivedTitle: String? = null
    var receivedSnippet: String? = null

    composeTestRule.setContent {
      HazardMarker(
          hazard = hazard,
          severities = severities,
          markerContent = { _, title, snippet, content ->
            receivedTitle = title
            receivedSnippet = snippet
            Box { content() }
          })
    }

    composeTestRule.waitForIdle()

    // 2) Title and snippet are passed
    TestCase.assertEquals("Test hazard", receivedTitle)
    TestCase.assertEquals("5 units", receivedSnippet)

    // 3) Icon composable is rendered with a Tint semantics; we use the Unknown tag
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
}
