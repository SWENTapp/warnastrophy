package com.github.warnastrophy.core.ui.util

import com.github.warnastrophy.core.util.WktParser
import junit.framework.TestCase.*
import org.junit.Test
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon

class WktParserTest {
  @Test
  fun parseWktToJtsGeometry_validPolygon_returnsPolygonGeometry() {
    // A simple polygon for a square zone
    val wkt = "POLYGON ((10 10, 10 30, 30 30, 30 10, 10 10))"

    val result: Geometry? = WktParser.parseWktToJtsGeometry(wkt)

    assertNotNull("The result should not be null for a valid POLYGON WKT.", result)
    // Assert the exact type
    assertTrue("The resulting geometry should be a Polygon.", result is Polygon)

    // Check geometry properties (e.g., number of coordinates)
    assertEquals("The polygon should have 5 coordinates.", 5, result?.coordinates?.size)
  }

  @Test
  fun parseWktToJtsGeometry_validPoint_returnsPointGeometry() {
    // A simple point coordinate
    val wkt = "POINT (50 60)"

    val result: Geometry? = WktParser.parseWktToJtsGeometry(wkt)

    assertNotNull("The result should not be null for a valid POINT WKT.", result)
    assertTrue("The resulting geometry should be a Point.", result is Point)

    // Verify the coordinates
    val point = result as Point
    assertEquals(50.0, point.x, 0.0001)
    assertEquals(60.0, point.y, 0.0001)
  }

  @Test
  fun parseWktToJtsGeometry_invalidType_returnsNull() {
    // Typo in the geometry type (POLIGON instead of POLYGON)
    val invalidTypeWkt = "POLIGON ((10 10, 10 30, 30 30, 30 10, 10 10))"

    val result: Geometry? = WktParser.parseWktToJtsGeometry(invalidTypeWkt)

    assertNull("WKT with an invalid geometry type should return null.", result)
  }
}
