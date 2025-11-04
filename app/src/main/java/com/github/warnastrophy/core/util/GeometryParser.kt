package com.github.warnastrophy.core.util

import android.util.Log
import com.github.warnastrophy.core.model.Location
import java.text.ParseException
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.io.WKTReader
import org.locationtech.jts.io.geojson.GeoJsonReader

object GeometryParser {
  private const val TAG = "WKTParserUtil"

  /**
   * Parses a WKT string and returns the JTS Geometry object directly. This object is used by the
   * HazardChecker to perform spatial analysis (e.g., the Point-in-Polygon check via
   * Geometry.contains()).
   *
   * @param wktString The Well-Known Text string (e.g., "POLYGON ((...))").
   * @return The JTS Geometry object, or null if parsing fails.
   */
  fun parseWktToJtsGeometry(wktString: String): Geometry? {
    val wktReader = WKTReader()
    return try {
      // The read() method parses the WKT into the appropriate JTS Geometry subtype
      wktReader.read(wktString)
    } catch (e: Exception) {
      // Log the error if the WKT string is malformed
      Log.e(TAG, "Failed to parse WKT string to JTS Geometry.", e)
      null
    }
  }

  /**
   * Converts a raw GeoJSON Geometry string (MultiPolygon in this case) directly into a JTS Geometry
   * object.
   * * @param geoJsonGeometryString The JSON string starting with {"type": "MultiPolygon", ...}
   *
   * @return The resulting JTS Geometry object, or null if parsing fails.
   */
  fun convertRawGeoJsonGeometryToJTS(geoJsonGeometryString: String): Geometry? {
    return try {
      val jtsReader = GeoJsonReader()
      jtsReader.read(geoJsonGeometryString)
    } catch (e: ParseException) {
      System.err.println("JTS Parsing Error: ${e.message}")
      null
    } catch (e: Exception) {
      // Handle other exceptions
      System.err.println("Unexpected Error: ${e.message}")
      null
    }
  }

  /**
   * Transforms a JTS Geometry object into a List of LatLng objects. The output depends on the
   * Geometry type:
   * - Point: Returns a list with a single LatLng.
   * - Polygon: Returns a list of the vertices of the exterior ring.
   * - Other/Multi-Geometries: Returns null. This method is used to get centroid of hazard
   *
   * @param jtsGeometry The JTS Geometry object to transform.
   * @return A list of LatLng objects, or an empty list if conversion is not supported/possible.
   */
  fun jtsGeometryToLatLngList(jtsGeometry: Geometry): List<Location>? {
    if (jtsGeometry.isEmpty) {
      return null
    }
    val coordinates: Array<Coordinate> =
        when (jtsGeometry.geometryType) {
          // We decide to cover only these 2 types of geometry because we observed that on API, the
          // centroid is always a Point
          "Point" -> arrayOf((jtsGeometry as Point).coordinate)
          "Polygon" -> {
            // A Polygon's boundary is the exterior ring. We only convert the exterior.
            (jtsGeometry as Polygon).exteriorRing.coordinates
          }
          else -> {
            System.err.println(
                "Unsupported Geometry type for direct LatLng list conversion: ${jtsGeometry.geometryType}")
            return null
          }
        }
    return coordinates.map { coordinates ->
      Location(latitude = coordinates.y, longitude = coordinates.x)
    }
  }
}
