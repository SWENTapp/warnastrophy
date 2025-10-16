package com.github.warnastrophy.core.util

import android.util.Log
import com.github.warnastrophy.core.model.util.Location
import java.util.Locale
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.io.WKTReader

object WktParserUtil {
  private const val TAG = "WKTParserUtil"
  private val wktReader = WKTReader()

  /**
   * Parses a WKT string and returns the JTS Geometry object directly. This object is used by the
   * HazardChecker to perform spatial analysis (e.g., the Point-in-Polygon check via
   * Geometry.contains()).
   *
   * @param wktString The Well-Known Text string (e.g., "POLYGON ((...))").
   * @return The JTS Geometry object, or null if parsing fails.
   */
  fun parseWktToJtsGeometry(wktString: String): Geometry? {
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
   * Build a WKT POLYGON from a list of Location (latitude, longitude). Returns null when there are
   * not enough points to form a polygon.
   */
  fun polygonWktFromLocations(locations: List<Location>?): String? {
    if (locations == null) return null
    if (locations.size < 3) return null
    val ring =
        locations
            .map { String.format(Locale.US, "%f %f", it.longitude, it.latitude) }
            .toMutableList()
    if (ring.first() != ring.last()) ring.add(ring.first())
    return "POLYGON((${ring.joinToString(",")}))"
  }
}
