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
  fun `getAreaHazards with world polygon returns non empty list`() = runBlocking {
    val repo = HazardsRepository()
    val locationPolygon: String = HazardRepositoryProvider.WORLD_POLYGON
    // "POLYGON((-124.848974 49.384358,-124.848974 24.396308,-66.93457 24.396308," +
    //  "-66.93457 49.384358,-124.848974 49.384358))"
    // Polygone simplifié des USA (format WKT ou GeoJSON selon l'API attendue)
    val hazards: List<Hazard> = repo.getAreaHazards(locationPolygon, days = "3")
    assertTrue(hazards.isNotEmpty())
  }

  @Test
  fun `parseHazard retourne deux JSON hazards`() {
    // val testLogger = TestLogger()
    val repo = spyk(HazardsRepository())

    val hazardJson1 =
        JSONObject(
            """
            {
                "type":"Feature",
                "geometry":{"type":"Point","coordinates":[-115.2,25.9]},
                "properties":{
                    "eventid":1001222,
                    "eventtype":"TC",
                    "country":"Mexico",
                    "fromdate":"2025-10-04T21:00:00",
                    "severitydata":{"severity":175.9248,"severityunit":"km/h", "severitytext":"very bad"},
                    
                    "url":{"report":"https://www.gdacs.org/report.aspx?eventid=1001222&episodeid=24&eventtype=TC"},
                    "alertscore":1,
                    "iscurrent":true
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
                "properties":{
                    "eventid":1001225,
                    "eventtype":"TC",
                    "country":"Mexico",
                    "fromdate":"2025-10-09T15:00:00",
                    "severitydata":{"severity":92.592,"severityunit":"km/h", "severitytext":"very bad"},
                    "url":{"report":"https://www.gdacs.org/report.aspx?eventid=1001225&episodeid=5&eventtype=TC"},
                    "alertscore":1,
                    "iscurrent":true
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
