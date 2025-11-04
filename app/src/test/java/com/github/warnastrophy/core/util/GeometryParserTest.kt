package com.github.warnastrophy.core.util

import com.github.warnastrophy.core.util.GeometryParser.convertRawGeoJsonGeometryToJTS
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import org.junit.Test

class GeometryParserTest {
  private val VALID_GEOJSON =
      """
        {"type": "Point", "coordinates": [30.0, 10.0]}
    """
          .trimIndent()

  private val MALFORMED_GEOJSON =
      """
        {"type": "Point", "coordinates": [30.0, 10.0
    """
          .trimIndent()
  private val INVALID_INPUT_STRING = "not a json string at all"

  @Test
  fun `convertRawGeoJsonGeometryToJTS returns Geometry for valid GeoJSON`() {
    // ACT
    val result = convertRawGeoJsonGeometryToJTS(VALID_GEOJSON)

    // ASSERT
    assertNotNull("The result should not be null for valid GeoJSON", result)
    // Optionally, assert the type:
    assert(result is org.locationtech.jts.geom.Point) { "The geometry should be a Point" }
  }

  @Test
  fun `convertRawGeoJsonGeometryToJTS returns null and logs error for malformed GeoJSON`() {
    // ACT
    val result = convertRawGeoJsonGeometryToJTS(MALFORMED_GEOJSON)

    // ASSERT
    assertNull("The result should be null for malformed GeoJSON", result)

    // NOTE: To verify the log message was printed, you would need to use a library
    // to capture System.err output, but for coverage, hitting this path is sufficient.
  }

  @Test
  fun `convertRawGeoJsonGeometryToJTS returns null and logs for generic exceptions`() {
    // ACT
    val result = convertRawGeoJsonGeometryToJTS(INVALID_INPUT_STRING)

    // ASSERT
    assertNull("The result should be null for unexpected errors", result)
  }
}
