import com.github.warnastrophy.core.ui.repository.HazardsRepository
import com.github.warnastrophy.core.ui.repository.Hazard
import io.mockk.spyk
import kotlinx.coroutines.runBlocking

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class HazardsRepositoryIntegrationTest {

    @Test
    fun `getAreaHazards with US polygon returns non empty list`() = runBlocking {
        val repo = HazardsRepository()
         val locationPolygon: String =
            "POLYGON((-124.848974 49.384358,-124.848974 24.396308,-66.93457 24.396308," +
                    "-66.93457 49.384358,-124.848974 49.384358))"
        // Polygone simplifié des USA (format WKT ou GeoJSON selon l'API attendue)
        var hazards: List<Hazard> = repo.getAreaHazards(locationPolygon, days = "30")
        assertTrue(hazards.isNotEmpty())
    }

    @Test
    fun `parseHazard retourne un Hazard valide pour deux JSON hazards`() {
        val repo = spyk(HazardsRepository())

        val hazardJson1 = JSONObject("""
            {
                "type":"Feature",
                "geometry":{"type":"Point","coordinates":[-115.2,25.9]},
                "properties":{
                    "eventid":1001222,
                    "eventtype":"TC",
                    "country":"Mexico",
                    "fromdate":"2025-10-04T21:00:00",
                    "severitydata":{"severity":175.9248,"severityunit":"km/h"},
                    "url":{"report":"https://www.gdacs.org/report.aspx?eventid=1001222&episodeid=24&eventtype=TC"},
                    "alertscore":1,
                    "iscurrent":true
                }
            }
        """)

        val hazardJson2 = JSONObject("""
            {
                "type":"Feature",
                "geometry":{"type":"Point","coordinates":[-104.9,18.3]},
                "properties":{
                    "eventid":1001225,
                    "eventtype":"TC",
                    "country":"Mexico",
                    "fromdate":"2025-10-09T15:00:00",
                    "severitydata":{"severity":92.592,"severityunit":"km/h"},
                    "url":{"report":"https://www.gdacs.org/report.aspx?eventid=1001225&episodeid=5&eventtype=TC"},
                    "alertscore":1,
                    "iscurrent":true
                }
            }
        """)

        val method = HazardsRepository::class.java.getDeclaredMethod("parseHazard", JSONObject::class.java)
        method.isAccessible = true

        val hazard1 = method.invoke(repo, hazardJson1) as Hazard
        val hazard2 = method.invoke(repo, hazardJson2) as Hazard

        assertEquals(1001222, hazard1.id)
        assertEquals("TC", hazard1.type)
        assertEquals("Mexico", hazard1.country)
        assertEquals(175.9248, hazard1.severity!!, 0.001)

        assertEquals(1001225, hazard2.id)
        assertEquals("TC", hazard2.type)
        assertEquals("Mexico", hazard2.country)
        assertEquals(92.592, hazard2.severity!!, 0.001)
    }
}
