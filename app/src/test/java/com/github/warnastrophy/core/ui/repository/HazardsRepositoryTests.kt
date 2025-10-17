package com.github.warnastrophy.core.model.repository

import com.github.warnastrophy.core.model.util.Hazard
import com.github.warnastrophy.core.model.util.HazardsRepository
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
    val hazards: List<Hazard> = repo.getAreaHazards(locationPolygon, days = "30")
    assertTrue(hazards.isNotEmpty())
  }

  @Test
  fun `parseHazard retourne un Hazard valide pour deux JSON hazards`() {
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
                    "severitydata":{"severity":175.9248,"severityunit":"km/h"},
                    "htmldescription": "Green M 5.6 Earthquake in Indonesia at: 16 Oct 2025 17:02:58.",
                    "url":{
                        "geometry":"",
                        "report":"https://www.gdacs.org/report.aspx?eventid=1001222&episodeid=24&eventtype=TC"
                    },
                    "alertscore":1,
                    "iscurrent":true
                }
            }
        """)

    val hazardJson2 =
        JSONObject(
            """
            {
                "type":"Feature",
                "geometry": {
            "type": "Point",
                    "coordinates": [
                        139.0855,
                        -2.1142
                    ]
                },
                "properties":{
                    "eventid":1001225,
                    "eventtype":"TC",
                    "country":"Mexico",
                    "fromdate":"2025-10-09T15:00:00",
                    "severitydata":{"severity":92.592,"severityunit":"km/h"},
                    "htmldescription": "Green M 5.1 Earthquake in Indonesia at: 16 Oct 2025 17:02:58.",
                    "url":{
                        "geometry":"",
                        "report":"https://www.gdacs.org/report.aspx?eventid=1001225&episodeid=5&eventtype=TC"
                    },
                    "alertscore":1,
                    "iscurrent":true
                }
            }
        """)

    val method =
        HazardsRepository::class
            .java
            .getDeclaredMethod("parseHazard", JSONObject::class.java, JSONObject::class.java)
    method.isAccessible = true

    val geometry =
        JSONObject(
            """
                   {
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "bbox": [139.0855, -2.1142, 139.0855, -2.1142],
      "geometry": {
        "type": "Point",
        "coordinates": [139.0855, -2.1142]
      },
      "properties": {
        "eventtype": "EQ",
        "eventid": 1505491,
        "episodeid": 1666479,
        "eventname": "",
        "glide": "",
        "name": "Earthquake in Indonesia",
        "description": "Earthquake in Indonesia",
        "htmldescription": "Green M 5.1 Earthquake in Indonesia at: 16 Oct 2025 17:02:58.",
        "icon": "https://www.gdacs.org/images/gdacs_icons/maps/Green/EQ.png",
        "iconoverall": "https://www.gdacs.org/images/gdacs_icons/maps/Green/EQ.png",
        "url": {
          "geometry": "https://www.gdacs.org/gdacsapi/api/polygons/getgeometry?eventtype=EQ&eventid=1505491&episodeid=1666479",
          "report": "https://www.gdacs.org/report.aspx?eventid=1505491&episodeid=1666479&eventtype=EQ",
          "details": "https://www.gdacs.org/gdacsapi/api/events/geteventdata?eventtype=EQ&eventid=1505491"
        },
        "alertlevel": "Green",
        "alertscore": 1,
        "episodealertlevel": "Green",
        "episodealertscore": 0,
        "istemporary": "false",
        "iscurrent": "true",
        "country": "Indonesia",
        "fromdate": "2025-10-16T17:02:58",
        "todate": "2025-10-16T17:02:58",
        "datemodified": "2025-10-16T17:43:02",
        "iso3": "IDN",
        "source": "NEIC",
        "sourceid": "us6000rhke",
        "polygonlabel": "Centroid",
        "Class": "Point_Centroid",
        "affectedcountries": [
          {
            "iso2": "ID",
            "iso3": "IDN",
            "countryname": "Indonesia"
          }
        ],
        "severitydata": {
          "severity": 5.1,
          "severitytext": "Magnitude 5.1M, Depth:10km",
          "severityunit": "M"
        }
      }
    },
    {
      "type": "Feature",
      "bbox": [138.926121, -2.24854, 139.244875, -1.958303],
      "geometry": {
        "type": "MultiPolygon",
        "coordinates": [
          [
            [
              [139.120661, -2.245641],
              [139.192182, -2.211982],
              [139.238931, -2.162212],
              [139.244875, -2.112442],
              [139.236504, -2.070968],
              [139.220972, -2.037788],
              [139.195642, -2.004608],
              [139.13725, -1.967425],
              [139.087481, -1.958303],
              [139.010861, -1.971429],
              [138.958509, -2.012903],
              [138.927224, -2.079263],
              [138.926121, -2.137327],
              [138.93585, -2.178802],
              [138.946467, -2.198212],
              [138.983288, -2.228571],
              [139.062596, -2.24854],
              [139.120661, -2.245641]
            ]
          ]
        ]
      },
      "properties": {
        "eventtype": "EQ",
        "eventid": 1505491,
        "episodeid": 1666479,
        "eventname": "",
        "glide": "",
        "name": "Earthquake in Indonesia",
        "description": "Earthquake in Indonesia",
        "htmldescription": "Green M 5.1 Earthquake in Indonesia at: 16 Oct 2025 17:02:58.",
        "icon": "https://www.gdacs.org/images/gdacs_icons/maps/Green/EQ.png",
        "iconoverall": "https://www.gdacs.org/images/gdacs_icons/maps/Green/EQ.png",
        "url": {
          "geometry": "https://www.gdacs.org/gdacsapi/api/polygons/getgeometry?eventtype=EQ&eventid=1505491&episodeid=1666479",
          "report": "https://www.gdacs.org/report.aspx?eventid=1505491&episodeid=1666479&eventtype=EQ",
          "details": "https://www.gdacs.org/gdacsapi/api/events/geteventdata?eventtype=EQ&eventid=1505491"
        },
        "alertlevel": "Green",
        "alertscore": 1,
        "episodealertlevel": "Green",
        "episodealertscore": 0,
        "istemporary": "false",
        "iscurrent": "true",
        "country": "Indonesia",
        "fromdate": "2025-10-16T17:02:58",
        "todate": "2025-10-16T17:02:58",
        "datemodified": "2025-10-16T17:43:02",
        "iso3": "IDN",
        "source": "NEIC",
        "sourceid": "us6000rhke",
        "polygondate": "2025-10-16T19:25:02",
        "polygonlabel": "Intensity 0",
        "affectedcountries": [
          {
            "iso2": "ID",
            "iso3": "IDN",
            "countryname": "Indonesia"
          }
        ],
        "severitydata": {
          "severity": 5.1,
          "severitytext": "Magnitude 5.1M, Depth:10km",
          "severityunit": "M"
        },
        "Class": "Poly_SMPInt_0",
        "iconeventlink": "https://www.gdacs.org/images/gdacs_icons/maps/Green/EQ.png",
        "iconitemlink": "https://www.gdacs.org/images/gdacs_icons/maps/Green/EQ.png"
      }
    },
    {
      "type": "Feature",
      "bbox": [138.926121, -2.24854, 139.244875, -1.958303],
      "geometry": {
        "type": "Polygon",
        "coordinates": [
          [
            [139.062596, -2.24854],
            [139.120661, -2.245641],
            [139.192182, -2.211982],
            [139.238931, -2.162212],
            [139.244875, -2.112442],
            [139.236504, -2.070968],
            [139.220972, -2.037788],
            [139.195642, -2.004608],
            [139.13725, -1.967425],
            [139.087481, -1.958303],
            [139.010861, -1.971429],
            [138.958509, -2.012903],
            [138.927224, -2.079263],
            [138.926121, -2.137327],
            [138.93585, -2.178802],
            [138.946467, -2.198212],
            [138.983288, -2.228571],
            [139.062596, -2.24854]
          ],
          [
            [139.079186, -2.220727],
            [139.046006, -2.21647],
            [139.021121, -2.20165],
            [138.986589, -2.162212],
            [138.974764, -2.120737],
            [138.982305, -2.070968],
            [139.004531, -2.035138],
            [139.037711, -2.011153],
            [139.070891, -2.004151],
            [139.112366, -2.010702],
            [139.15384, -2.034119],
            [139.186681, -2.070968],
            [139.199025, -2.112442],
            [139.191075, -2.153917],
            [139.169258, -2.187097],
            [139.131196, -2.211982],
            [139.079186, -2.220727]
          ]
        ]
      },
      "properties": {
        "eventtype": "EQ",
        "eventid": 1505491,
        "episodeid": 1666479,
        "eventname": "",
        "glide": "",
        "name": "Earthquake in Indonesia",
        "description": "Earthquake in Indonesia",
        "htmldescription": "Green M 5.1 Earthquake in Indonesia at: 16 Oct 2025 17:02:58.",
        "icon": "https://www.gdacs.org/images/gdacs_icons/maps/Green/EQ.png",
        "iconoverall": "https://www.gdacs.org/images/gdacs_icons/maps/Green/EQ.png",
        "url": {
          "geometry": "https://www.gdacs.org/gdacsapi/api/polygons/getgeometry?eventtype=EQ&eventid=1505491&episodeid=1666479",
          "report": "https://www.gdacs.org/report.aspx?eventid=1505491&episodeid=1666479&eventtype=EQ",
          "details": "https://www.gdacs.org/gdacsapi/api/events/geteventdata?eventtype=EQ&eventid=1505491"
        },
        "alertlevel": "Green",
        "alertscore": 1,
        "episodealertlevel": "Green",
        "episodealertscore": 0,
        "istemporary": "false",
        "iscurrent": "true",
        "country": "Indonesia",
        "fromdate": "2025-10-16T17:02:58",
        "todate": "2025-10-16T17:02:58",
        "datemodified": "2025-10-16T17:43:02",
        "iso3": "IDN",
        "source": "NEIC",
        "sourceid": "us6000rhke",
        "polygondate": "2025-10-16T19:25:02",
        "polygonlabel": "Intensity 4",
        "affectedcountries": [
          {
            "iso2": "ID",
            "iso3": "IDN",
            "countryname": "Indonesia"
          }
        ],
        "severitydata": {
          "severity": 5.1,
          "severitytext": "Magnitude 5.1M, Depth:10km",
          "severityunit": "M"
        },
        "Class": "Poly_SMPInt_4",
        "iconeventlink": "https://www.gdacs.org/images/gdacs_icons/maps/Green/EQ.png",
        "iconitemlink": "https://www.gdacs.org/images/gdacs_icons/maps/Green/EQ.png"
      }
    },
    {
      "type": "Feature",
      "bbox": [138.974764, -2.220727, 139.199025, -2.004151],
      "geometry": {
        "type": "Polygon",
        "coordinates": [
          [
            [139.079186, -2.220727],
            [139.131196, -2.211982],
            [139.169258, -2.187097],
            [139.191075, -2.153917],
            [139.199025, -2.112442],
            [139.186681, -2.070968],
            [139.15384, -2.034119],
            [139.112366, -2.010702],
            [139.070891, -2.004151],
            [139.037711, -2.011153],
            [139.004531, -2.035138],
            [138.982305, -2.070968],
            [138.974764, -2.120737],
            [138.986589, -2.162212],
            [139.021121, -2.20165],
            [139.046006, -2.21647],
            [139.079186, -2.220727]
          ],
          [
            [139.095776, -2.170788],
            [139.054301, -2.161741],
            [139.029416, -2.133494],
            [139.022587, -2.112442],
            [139.037711, -2.079147],
            [139.070891, -2.056377],
            [139.104071, -2.058981],
            [139.140655, -2.095853],
            [139.143469, -2.120737],
            [139.138116, -2.137327],
            [139.120661, -2.156985],
            [139.095776, -2.170788]
          ]
        ]
      },
      "properties": {
        "eventtype": "EQ",
        "eventid": 1505491,
        "episodeid": 1666479,
        "eventname": "",
        "glide": "",
        "name": "Earthquake in Indonesia",
        "description": "Earthquake in Indonesia",
        "htmldescription": "Green M 5.1 Earthquake in Indonesia at: 16 Oct 2025 17:02:58.",
        "icon": "https://www.gdacs.org/images/gdacs_icons/maps/Green/EQ.png",
        "iconoverall": "https://www.gdacs.org/images/gdacs_icons/maps/Green/EQ.png",
        "url": {
          "geometry": "https://www.gdacs.org/gdacsapi/api/polygons/getgeometry?eventtype=EQ&eventid=1505491&episodeid=1666479",
          "report": "https://www.gdacs.org/report.aspx?eventid=1505491&episodeid=1666479&eventtype=EQ",
          "details": "https://www.gdacs.org/gdacsapi/api/events/geteventdata?eventtype=EQ&eventid=1505491"
        },
        "alertlevel": "Green",
        "alertscore": 1,
        "episodealertlevel": "Green",
        "episodealertscore": 0,
        "istemporary": "false",
        "iscurrent": "true",
        "country": "Indonesia",
        "fromdate": "2025-10-16T17:02:58",
        "todate": "2025-10-16T17:02:58",
        "datemodified": "2025-10-16T17:43:02",
        "iso3": "IDN",
        "source": "NEIC",
        "sourceid": "us6000rhke",
        "polygondate": "2025-10-16T19:25:02",
        "polygonlabel": "Intensity 4.5",
        "affectedcountries": [
          {
            "iso2": "ID",
            "iso3": "IDN",
            "countryname": "Indonesia"
          }
        ],
        "severitydata": {
          "severity": 5.1,
          "severitytext": "Magnitude 5.1M, Depth:10km",
          "severityunit": "M"
        },
        "Class": "Poly_SMPInt_4.5",
        "iconeventlink": "https://www.gdacs.org/images/gdacs_icons/maps/Green/EQ.png",
        "iconitemlink": "https://www.gdacs.org/images/gdacs_icons/maps/Green/EQ.png"
      }
    },
    {
      "type": "Feature",
      "bbox": [139.022587, -2.170788, 139.143469, -2.056377],
      "geometry": {
        "type": "MultiPolygon",
        "coordinates": [
          [
            [
              [139.095776, -2.170788],
              [139.120661, -2.156985],
              [139.138116, -2.137327],
              [139.143469, -2.120737],
              [139.140655, -2.095853],
              [139.104071, -2.058981],
              [139.070891, -2.056377],
              [139.037711, -2.079147],
              [139.022587, -2.112442],
              [139.029416, -2.133494],
              [139.054301, -2.161741],
              [139.095776, -2.170788]
            ]
          ]
        ]
      },
      "properties": {
        "eventtype": "EQ",
        "eventid": 1505491,
        "episodeid": 1666479,
        "eventname": "",
        "glide": "",
        "name": "Earthquake in Indonesia",
        "description": "Earthquake in Indonesia",
        "htmldescription": "Green M 5.1 Earthquake in Indonesia at: 16 Oct 2025 17:02:58.",
        "icon": "https://www.gdacs.org/images/gdacs_icons/maps/Green/EQ.png",
        "iconoverall": "https://www.gdacs.org/images/gdacs_icons/maps/Green/EQ.png",
        "url": {
          "geometry": "https://www.gdacs.org/gdacsapi/api/polygons/getgeometry?eventtype=EQ&eventid=1505491&episodeid=1666479",
          "report": "https://www.gdacs.org/report.aspx?eventid=1505491&episodeid=1666479&eventtype=EQ",
          "details": "https://www.gdacs.org/gdacsapi/api/events/geteventdata?eventtype=EQ&eventid=1505491"
        },
        "alertlevel": "Green",
        "alertscore": 1,
        "episodealertlevel": "Green",
        "episodealertscore": 0,
        "istemporary": "false",
        "iscurrent": "true",
        "country": "Indonesia",
        "fromdate": "2025-10-16T17:02:58",
        "todate": "2025-10-16T17:02:58",
        "datemodified": "2025-10-16T17:43:02",
        "iso3": "IDN",
        "source": "NEIC",
        "sourceid": "us6000rhke",
        "polygondate": "2025-10-16T19:25:02",
        "polygonlabel": "Intensity 5",
        "affectedcountries": [
          {
            "iso2": "ID",
            "iso3": "IDN",
            "countryname": "Indonesia"
          }
        ],
        "severitydata": {
          "severity": 5.1,
          "severitytext": "Magnitude 5.1M, Depth:10km",
          "severityunit": "M"
        },
        "Class": "Poly_SMPInt_5",
        "iconeventlink": "https://www.gdacs.org/images/gdacs_icons/maps/Green/EQ.png",
        "iconitemlink": "https://www.gdacs.org/images/gdacs_icons/maps/Green/EQ.png"
      }
    },
    {
      "type": "Feature",
      "bbox": [138.184, -3.015, 139.987, -1.213],
      "geometry": {
        "type": "Polygon",
        "coordinates": [
          [
            [139.987, -2.114],
            [139.986, -2.083],
            [139.985, -2.051],
            [139.982, -2.02],
            [139.978, -1.989],
            [139.973, -1.958],
            [139.967, -1.927],
            [139.96, -1.896],
            [139.952, -1.866],
            [139.943, -1.836],
            [139.933, -1.806],
            [139.921, -1.777],
            [139.909, -1.748],
            [139.896, -1.719],
            [139.881, -1.691],
            [139.866, -1.664],
            [139.85, -1.637],
            [139.833, -1.61],
            [139.815, -1.585],
            [139.796, -1.56],
            [139.776, -1.535],
            [139.755, -1.511],
            [139.734, -1.488],
            [139.712, -1.466],
            [139.689, -1.445],
            [139.665, -1.424],
            [139.641, -1.404],
            [139.615, -1.385],
            [139.59, -1.367],
            [139.563, -1.35],
            [139.536, -1.334],
            [139.509, -1.319],
            [139.481, -1.304],
            [139.452, -1.291],
            [139.423, -1.279],
            [139.394, -1.268],
            [139.364, -1.257],
            [139.334, -1.248],
            [139.304, -1.24],
            [139.273, -1.233],
            [139.242, -1.227],
            [139.211, -1.222],
            [139.18, -1.218],
            [139.148, -1.215],
            [139.117, -1.214],
            [139.086, -1.213],
            [139.054, -1.214],
            [139.023, -1.215],
            [138.991, -1.218],
            [138.96, -1.222],
            [138.929, -1.227],
            [138.898, -1.233],
            [138.867, -1.24],
            [138.837, -1.248],
            [138.807, -1.257],
            [138.777, -1.268],
            [138.748, -1.279],
            [138.719, -1.291],
            [138.69, -1.304],
            [138.662, -1.319],
            [138.635, -1.334],
            [138.608, -1.35],
            [138.581, -1.367],
            [138.556, -1.385],
            [138.53, -1.404],
            [138.506, -1.424],
            [138.482, -1.445],
            [138.459, -1.466],
            [138.437, -1.488],
            [138.416, -1.511],
            [138.395, -1.535],
            [138.375, -1.56],
            [138.356, -1.585],
            [138.338, -1.61],
            [138.321, -1.637],
            [138.305, -1.664],
            [138.29, -1.691],
            [138.275, -1.719],
            [138.262, -1.748],
            [138.25, -1.777],
            [138.238, -1.806],
            [138.228, -1.836],
            [138.219, -1.866],
            [138.211, -1.896],
            [138.204, -1.927],
            [138.198, -1.958],
            [138.193, -1.989],
            [138.189, -2.02],
            [138.186, -2.051],
            [138.185, -2.083],
            [138.184, -2.114],
            [138.185, -2.146],
            [138.186, -2.177],
            [138.189, -2.208],
            [138.193, -2.24],
            [138.198, -2.271],
            [138.204, -2.302],
            [138.211, -2.332],
            [138.219, -2.363],
            [138.228, -2.393],
            [138.238, -2.422],
            [138.25, -2.452],
            [138.262, -2.481],
            [138.275, -2.509],
            [138.29, -2.537],
            [138.305, -2.565],
            [138.321, -2.592],
            [138.338, -2.618],
            [138.356, -2.644],
            [138.375, -2.669],
            [138.395, -2.693],
            [138.416, -2.717],
            [138.437, -2.74],
            [138.459, -2.762],
            [138.482, -2.784],
            [138.506, -2.804],
            [138.53, -2.824],
            [138.556, -2.843],
            [138.581, -2.861],
            [138.608, -2.878],
            [138.635, -2.894],
            [138.662, -2.91],
            [138.69, -2.924],
            [138.719, -2.937],
            [138.748, -2.95],
            [138.777, -2.961],
            [138.807, -2.971],
            [138.837, -2.98],
            [138.867, -2.988],
            [138.898, -2.995],
            [138.929, -3.001],
            [138.96, -3.006],
            [138.991, -3.01],
            [139.023, -3.013],
            [139.054, -3.015],
            [139.086, -3.015],
            [139.117, -3.015],
            [139.148, -3.013],
            [139.18, -3.01],
            [139.211, -3.006],
            [139.242, -3.001],
            [139.273, -2.995],
            [139.304, -2.988],
            [139.334, -2.98],
            [139.364, -2.971],
            [139.394, -2.961],
            [139.423, -2.95],
            [139.452, -2.937],
            [139.481, -2.924],
            [139.509, -2.91],
            [139.536, -2.894],
            [139.563, -2.878],
            [139.59, -2.861],
            [139.615, -2.843],
            [139.641, -2.824],
            [139.665, -2.804],
            [139.689, -2.784],
            [139.712, -2.762],
            [139.734, -2.74],
            [139.755, -2.717],
            [139.776, -2.693],
            [139.796, -2.669],
            [139.815, -2.644],
            [139.833, -2.618],
            [139.85, -2.592],
            [139.866, -2.565],
            [139.881, -2.537],
            [139.896, -2.509],
            [139.909, -2.481],
            [139.921, -2.452],
            [139.933, -2.422],
            [139.943, -2.393],
            [139.952, -2.363],
            [139.96, -2.332],
            [139.967, -2.302],
            [139.973, -2.271],
            [139.978, -2.24],
            [139.982, -2.208],
            [139.985, -2.177],
            [139.986, -2.146],
            [139.987, -2.114]
          ]
        ]
      },
      "properties": {
        "eventtype": "EQ",
        "eventid": 1505491,
        "episodeid": 1666479,
        "eventname": "",
        "glide": "",
        "name": "Earthquake in Indonesia",
        "description": "Earthquake in Indonesia",
        "htmldescription": "Green M 5.1 Earthquake in Indonesia at: 16 Oct 2025 17:02:58.",
        "icon": "https://www.gdacs.org/images/gdacs_icons/maps/Green/EQ.png",
        "iconoverall": "https://www.gdacs.org/images/gdacs_icons/maps/Green/EQ.png",
        "url": {
          "geometry": "https://www.gdacs.org/gdacsapi/api/polygons/getgeometry?eventtype=EQ&eventid=1505491&episodeid=1666479",
          "report": "https://www.gdacs.org/report.aspx?eventid=1505491&episodeid=1666479&eventtype=EQ",
          "details": "https://www.gdacs.org/gdacsapi/api/events/geteventdata?eventtype=EQ&eventid=1505491"
        },
        "alertlevel": "Green",
        "alertscore": 1,
        "episodealertlevel": "Green",
        "episodealertscore": 0,
        "istemporary": "false",
        "iscurrent": "true",
        "country": "Indonesia",
        "fromdate": "2025-10-16T17:02:58",
        "todate": "2025-10-16T17:02:58",
        "datemodified": "2025-10-16T17:43:02",
        "iso3": "IDN",
        "source": "NEIC",
        "sourceid": "us6000rhke",
        "polygondate": "2025-10-16T19:25:02",
        "polygonlabel": "100km",
        "affectedcountries": [
          {
            "iso2": "ID",
            "iso3": "IDN",
            "countryname": "Indonesia"
          }
        ],
        "severitydata": {
          "severity": 5.1,
          "severitytext": "Magnitude 5.1M, Depth:10km",
          "severityunit": "M"
        },
        "Class": "Poly_Circle",
        "iconeventlink": "https://www.gdacs.org/images/gdacs_icons/maps/Green/EQ.png",
        "iconitemlink": "https://www.gdacs.org/images/gdacs_icons/maps/Green/EQ.png"
      }
    }
  ],
  "bbox": null
}""")

    val hazard1 = method.invoke(repo, hazardJson1, geometry) as Hazard
    val hazard2 = method.invoke(repo, hazardJson2, geometry) as Hazard

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
