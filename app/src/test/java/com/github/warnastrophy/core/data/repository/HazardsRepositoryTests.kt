package com.github.warnastrophy.core.data.repository

import com.github.warnastrophy.core.model.Hazard
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
class HazardsRepositoryIntegrationTest {

  @Before
  fun setUp() {
    ShadowLog.stream = System.out
  }

  @Test
  fun `getAreaHazards with US polygon returns non empty list`() = runBlocking {
    val repo = HazardsRepository()
    val locationPolygon: String =
        "POLYGON((-124.848974 49.384358,-124.848974 24.396308,-66.93457 24.396308," +
            "-66.93457 49.384358,-124.848974 49.384358))"
    // Polygone simplifié des USA (format WKT ou GeoJSON selon l'API attendue)
    val hazards: List<Hazard> = repo.getAreaHazards(locationPolygon, days = "30")
    assertTrue(hazards.isNotEmpty())
  }

  @Test
  fun `parseHazard retourne un Hazard valide pour deux JSON hazards`() {
    // val testLogger = TestLogger()
    val repo = spyk(HazardsRepository())

    val hazardJson1 =
        JSONObject(
            """
            {
              "type": "Feature",
              "bbox": [
                -116.076,
                31.325,
                -116.076,
                31.325
              ],
              "geometry": {
                "type": "Point",
                "coordinates": [
                  -116.076,
                  31.325
                ]
              },
              "properties": {
                "eventtype": "EQ",
                "eventid": 1503804,
                "episodeid": 1664543,
                "eventname": "",
                "glide": "",
                "name": "Earthquake in Mexico",
                "description": "Earthquake in Mexico",
                "htmldescription": "Green M 4.6 Earthquake in Mexico at: 06 Oct 2025 16:53:27.",
                "icon": "https://www.gdacs.org/images/gdacs_icons/maps/Green/EQ.png",
                "iconoverall": "https://www.gdacs.org/images/gdacs_icons/maps/Green/EQ.png",
                "url": {
                  "geometry": "https://www.gdacs.org/gdacsapi/api/polygons/getgeometry?eventtype=EQ&eventid=1503804&episodeid=1664543",
                  "report": "https://www.gdacs.org/report.aspx?eventid=1503804&episodeid=1664543&eventtype=EQ",
                  "details": "https://www.gdacs.org/gdacsapi/api/events/geteventdata?eventtype=EQ&eventid=1503804"
                },
                "alertlevel": "Green",
                "alertscore": 1,
                "iscurrent": "false",
                "fromdate": "2025-10-06T16:53:27",
                "country": "Mexico",
                "severitydata": {
                  "severity": 4.6,
                  "severitytext": "Magnitude 4.6M, Depth:1km",
                  "severityunit": "M"
                 }
              }
            }
        """)

    val hazardJson2 =
        JSONObject(
            """
                {
                  "type": "Feature",
                  "bbox": [
                    -59.5,
                    33.2,
                    -59.5,
                    33.2
                  ],
                  "geometry": {
                    "type": "Point",
                    "coordinates": [
                      -59.5,
                      33.2
                    ]
                  },
                  "properties": {
                    "eventtype": "TC",
                    "eventid": 1001216,
                    "episodeid": 24,
                    "eventname": "IMELDA-25",
                    "glide": "",
                    "name": "Tropical Cyclone IMELDA-25",
                    "description": "Tropical Cyclone IMELDA-25",
                    "htmldescription": "Green Tropical Cyclone IMELDA-25 in Bermuda, Bahamas, Cuba from: 26 Sep 2025  to: 02 Oct 2025 .",
                    "icon": "https://www.gdacs.org/images/gdacs_icons/maps/Green/TC.png",
                    "iconoverall": "https://www.gdacs.org/images/gdacs_icons/maps/Green/TC.png",
                    "url": {
                      "geometry": "https://www.gdacs.org/gdacsapi/api/polygons/getgeometry?eventtype=TC&eventid=1001216&episodeid=24",
                      "report": "https://www.gdacs.org/report.aspx?eventid=1001216&episodeid=24&eventtype=TC",
                      "details": "https://www.gdacs.org/gdacsapi/api/events/geteventdata?eventtype=TC&eventid=1001216"
                    },
                    "alertlevel": "Green",
                    "country": "Bermuda, Bahamas, Cuba",
                    "alertscore": 1,
                    "iscurrent": "false",
                    "fromdate": "2025-09-26T21:00:00",
                    "severitydata": {
                      "severity": 157.4064,
                      "severitytext": "Hurricane/Typhoon > 74 mph (maximum wind speed of 157 km/h)",
                      "severityunit": "km/h"
                    }
                  }
                }
        """)

    val method =
        HazardsRepository::class.java.getDeclaredMethod("parseHazard", JSONObject::class.java)
    method.isAccessible = true

    val hazard1 = method.invoke(repo, hazardJson1) as Hazard
    val hazard2 = method.invoke(repo, hazardJson2) as Hazard

    assertEquals(1503804, hazard1.id)
    assertEquals("EQ", hazard1.type)
    assertEquals("Mexico", hazard1.country)
    assertEquals(4.6, hazard1.severity!!, 0.001)

    assertNotNull(hazard1.affectedZone)
    assertNotNull(hazard1.description)
    assertTrue(hazard1.articleUrl?.isNotBlank() ?: false)
    assertNotNull(hazard1.bbox)

    assertEquals(1001216, hazard2.id)
    assertEquals("TC", hazard2.type)
    assertEquals("Bermuda, Bahamas, Cuba", hazard2.country)
    assertEquals(157.4064, hazard2.severity!!, 0.001)

    assertTrue(hazard1.articleUrl?.isNotBlank() ?: false)
    assertNotNull(hazard2.affectedZone)
    assertNotNull(hazard2.description)
    assertNotNull(hazard2.bbox)
  }
}
