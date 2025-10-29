package com.github.warnastrophy.core.util

import android.util.Log
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.io.WKTReader

object WktParser {
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
}
