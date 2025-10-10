import com.github.warnastrophy.core.model.util.Location
import com.github.warnastrophy.core.ui.repository.Hazard
import com.github.warnastrophy.core.ui.repository.HazardsRepository
import kotlin.jvm.java
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*

class HazardsRepositoryMockTest {
  val mockJson =
      """"
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature",
              "bbox": [17.576406, 40.727631, 17.576406, 40.727631],
              "geometry": {
                "type": "Point",
                "coordinates": [17.576406, 40.727631]
              },
              "properties": {
                "eventtype": "FL",
                "eventid": 1103510,
                "episodeid": 13,
                "eventname": "",
                "glide": "",
                "name": "Flood in Italy",
                "description": "Flood in Italy",
                "htmldescription": "Green Flood in Italy from: 21 Sep 2025 01 to: 04 Oct 2025 01.",
                "icon": "https://www.gdacs.org/images/gdacs_icons/maps/Green/FL.png",
                "iconoverall": "https://www.gdacs.org/images/gdacs_icons/maps/Green/FL.png",
                "url": {
                  "geometry": "https://www.gdacs.org/gdacsapi/api/polygons/getgeometry?eventtype=FL&eventid=1103510&episodeid=13",
                  "report": "https://www.gdacs.org/report.aspx?eventid=1103510&episodeid=13&eventtype=FL",
                  "details": "https://www.gdacs.org/gdacsapi/api/events/geteventdata?eventtype=FL&eventid=1103510"
                },
                "alertlevel": "Green",
                "alertscore": 1,
                "episodealertlevel": "Green",
                "episodealertscore": 0.5,
                "istemporary": "false",
                "iscurrent": "false",
                "country": "Italy",
                "fromdate": "2025-09-21T01:00:00",
                "todate": "2025-10-04T01:00:00",
                "datemodified": "2025-10-04T09:13:34",
                "iso3": "ITA",
                "source": "GLOFAS",
                "sourceid": "",
                "polygonlabel": "Centroid",
                "Class": "Point_Centroid",
                "affectedcountries": [
                  {
                    "iso2": "IT",
                    "iso3": "ITA",
                    "countryname": "Italy"
                  }
                ],
                "severitydata": {
                  "severity": 0.0,
                  "severitytext": "Magnitude 0 ",
                  "severityunit": ""
                }
              }
            },
            {
              "type": "Feature",
              "bbox": [13.0546698, 45.6427259, 13.0546698, 45.6427259],
              "geometry": {
                "type": "Point",
                "coordinates": [13.0546698, 45.6427259]
              },
              "properties": {
                "eventtype": "FL",
                "eventid": 1103443,
                "episodeid": 17,
                "eventname": "",
                "glide": "",
                "name": "Flood in Italy",
                "description": "Flood in Italy",
                "htmldescription": "Green Flood in Italy from: 15 Aug 2025 01 to: 12 Sep 2025 01.",
                "icon": "https://www.gdacs.org/images/gdacs_icons/maps/Green/FL.png",
                "iconoverall": "https://www.gdacs.org/images/gdacs_icons/maps/Green/FL.png",
                "url": {
                  "geometry": "https://www.gdacs.org/gdacsapi/api/polygons/getgeometry?eventtype=FL&eventid=1103443&episodeid=17",
                  "report": "https://www.gdacs.org/report.aspx?eventid=1103443&episodeid=17&eventtype=FL",
                  "details": "https://www.gdacs.org/gdacsapi/api/events/geteventdata?eventtype=FL&eventid=1103443"
                },
                "alertlevel": "Green",
                "alertscore": 1,
                "episodealertlevel": "Green",
                "episodealertscore": 0.5,
                "istemporary": "false",
                "iscurrent": "false",
                "country": "Italy",
                "fromdate": "2025-08-15T01:00:00",
                "todate": "2025-09-12T01:00:00",
                "datemodified": "2025-09-16T11:15:53",
                "iso3": "ITA",
                "source": "GLOFAS",
                "sourceid": "",
                "polygonlabel": "Centroid",
                "Class": "Point_Centroid",
                "affectedcountries": [
                  {
                    "iso2": "IT",
                    "iso3": "ITA",
                    "countryname": "Italy"
                  }
                ],
                "severitydata": {
                  "severity": 0.0,
                  "severitytext": "Magnitude 0 ",
                  "severityunit": ""
                }
              }
            }
          ],
          "bbox": null
        }
        """"

  @Test
  fun `getAreaHazards return mock json`() = runBlocking {
    val mockRepo = mock(HazardsRepository::class.java)
    val hazards =
        listOf(
            Hazard(
                id = "1103510",
                type = "FL",
                country = "Italy",
                date = "2025-09-21T01:00:00",
                severity = "0.0",
                severityUnit = "",
                reportUrl =
                    "https://www.gdacs.org/report.aspx?eventid=1103510&episodeid=13&eventtype=FL",
                alertLevel = 1,
                coordinates = listOf(Location(40.727631, 17.576406))),
            Hazard(
                id = "1103443",
                type = "FL",
                country = "Italy",
                date = "2025-08-15T01:00:00",
                severity = "0.0",
                severityUnit = "",
                reportUrl =
                    "https://www.gdacs.org/report.aspx?eventid=1103443&episodeid=17&eventtype=FL",
                alertLevel = 1,
                coordinates = listOf(Location(45.6427259, 13.0546698))))
    `when`(
            mockRepo.getAreaHazards(
                "POLYGON((6.0 45.8%2C6.0 47.8%2C10.5 47.8%2C10.5 45.8%2C6.0 45.8))"))
        .thenReturn(hazards)

    var response: List<Hazard> = emptyList()
    GlobalScope.launch { response = mockRepo.getAreaHazards("mocked_geometry") }

    // Attendre la coroutine
    Thread.sleep(500)

    assertEquals(2, response.size)
    assertEquals("Italy", response[0].country)
    assertEquals("0.0", response[0].severity)
    assertEquals(1, response[0].alertLevel)
    assertEquals(40.727631, response[0].coordinates!![0].latitude, 1e-6)
    assertEquals(17.576406, response[0].coordinates!![0].longitude, 1e-6)
  }
}
