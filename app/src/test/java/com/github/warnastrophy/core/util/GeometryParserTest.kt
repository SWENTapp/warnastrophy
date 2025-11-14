package com.github.warnastrophy.core.util

import com.github.warnastrophy.core.util.GeometryParser.convertRawGeoJsonGeometryToJTS
import com.github.warnastrophy.core.util.GeometryParser.jtsGeometryToLatLngList
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import org.junit.Assert.*
import org.junit.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory

class GeometryParserTest {
  private val geometryFactory = GeometryFactory()
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

    assertNotNull("The result should not be null for valid GeoJSON", result)

    assert(result is org.locationtech.jts.geom.Point) { "The geometry should be a Point" }
  }

  @Test
  fun `convertRawGeoJsonGeometryToJTS returns null and logs error for malformed GeoJSON`() {
    val result = convertRawGeoJsonGeometryToJTS(MALFORMED_GEOJSON)

    assertNull("The result should be null for malformed GeoJSON", result)
  }

  @Test
  fun `convertRawGeoJsonGeometryToJTS returns null and logs for generic exceptions`() {
    // ACT
    val result = convertRawGeoJsonGeometryToJTS(INVALID_INPUT_STRING)

    // ASSERT
    assertNull("The result should be null for unexpected errors", result)
  }

  @Test
  fun `conversion returns null for empty geometry`() {
    // An empty LineString (which is an empty geometry)
    val jtsEmpty = geometryFactory.createLineString(arrayOf())

    val result = jtsGeometryToLatLngList(jtsEmpty)

    assertNull("Result must be null for empty geometry", result)
  }

  @Test
  fun `conversion returns single Location for Point geometry`() {
    // ARRANGE: JTS Point (X=Longitude 10.5, Y=Latitude 20.9)
    val testLon = 10.5
    val testLat = 20.9
    val jtsPoint = geometryFactory.createPoint(Coordinate(testLon, testLat))

    val result = jtsGeometryToLatLngList(jtsPoint)

    assertNotNull("Result must not be null for valid Point", result)
    assertEquals("Result list must contain exactly one Location", 1, result!!.size)

    // Check coordinate mapping
    assertEquals("Latitude must match JTS Y", testLat, result[0].latitude, 0.0001)
    assertEquals("Longitude must match JTS X", testLon, result[0].longitude, 0.0001)
  }

  @Test
  fun `conversion extracts exterior ring Locations for Polygon`() {
    // ARRANGE: A simple closed square polygon (5 vertices)
    val coords =
        arrayOf(
            Coordinate(10.0, 40.0), // Lon, Lat
            Coordinate(20.0, 40.0),
            Coordinate(20.0, 50.0),
            Coordinate(10.0, 50.0),
            Coordinate(10.0, 40.0) // Closing coordinate
            )
    val shell = geometryFactory.createLinearRing(coords)
    val jtsPolygon = geometryFactory.createPolygon(shell, null)

    val result = jtsGeometryToLatLngList(jtsPolygon)

    assertNotNull("Result must not be null for valid Polygon", result)
    assertEquals("Result list must have 5 coordinates (including closing point)", 5, result!!.size)

    // Check first point (Lon 10.0, Lat 40.0)
    assertEquals(40.0, result.first().latitude, 0.0001)
    assertEquals(10.0, result.first().longitude, 0.0001)

    // Check last point (Lon 10.0, Lat 40.0)
    assertEquals(40.0, result.last().latitude, 0.0001)
    assertEquals(10.0, result.last().longitude, 0.0001)
  }

  @Test
  fun `conversion returns null and logs for unsupported LineString`() {
    // Create a LineString (explicitly excluded by the function's logic)
    val jtsLine =
        geometryFactory.createLineString(arrayOf(Coordinate(1.0, 1.0), Coordinate(2.0, 2.0)))

    val result = jtsGeometryToLatLngList(jtsLine)

    assertNull("Result must be null for unsupported geometry type", result)
  }

  @Test
  fun `jtsGeometryToLatLngList handles MultiPolygon by using first polygon exterior ring`() {
    // First polygon (square) - this exterior ring should be returned
    val coords1 =
        arrayOf(
            Coordinate(10.0, 40.0),
            Coordinate(20.0, 40.0),
            Coordinate(20.0, 50.0),
            Coordinate(10.0, 50.0),
            Coordinate(10.0, 40.0))
    val shell1 = geometryFactory.createLinearRing(coords1)
    val poly1 = geometryFactory.createPolygon(shell1, null)

    // Second polygon (different area) - should be ignored by the fallback
    val coords2 =
        arrayOf(
            Coordinate(30.0, 60.0),
            Coordinate(40.0, 60.0),
            Coordinate(40.0, 70.0),
            Coordinate(30.0, 70.0),
            Coordinate(30.0, 60.0))
    val shell2 = geometryFactory.createLinearRing(coords2)
    val poly2 = geometryFactory.createPolygon(shell2, null)

    val multi = geometryFactory.createMultiPolygon(arrayOf(poly1, poly2))

    val result = GeometryParser.jtsGeometryToLatLngList(multi)

    assertNotNull("Result must not be null for MultiPolygon fallback", result)
    assertEquals(
        "Result list must contain the exterior ring of the first polygon", 5, result!!.size)

    // Verify mapping (latitude = Y, longitude = X) for first/last (closing) coordinate
    assertEquals(40.0, result.first().latitude, 0.0001)
    assertEquals(10.0, result.first().longitude, 0.0001)
    assertEquals(40.0, result.last().latitude, 0.0001)
    assertEquals(10.0, result.last().longitude, 0.0001)
  }
}
