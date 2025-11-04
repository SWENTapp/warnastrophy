package com.github.warnastrophy.core.util

import android.util.Log
import java.text.ParseException
import org.locationtech.jts.geom.Geometry
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
      val jtsGeometry = jtsReader.read(geoJsonGeometryString)
      return jtsGeometry
    } catch (e: ParseException) {
      // Handle parsing errors, e.g., malformed GeoJSON syntax
      System.err.println("JTS Parsing Error: ${e.message}")
      return null
    } catch (e: Exception) {
      // Handle other exceptions
      System.err.println("Unexpected Error: ${e.message}")
      return null
    }
  }
}
