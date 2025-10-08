package com.github.warnastrophy.core.ui.repository

import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class HazardsRepositoryTests {

    private lateinit var repository: HazardsRepository

    @Before
    fun setup() {
        repository = spyk(HazardsRepository())
    }

    @Test
    fun `test buildUrlWorldHazards returns correct url`() {
        val url = repository.run {
            javaClass.getDeclaredMethod("buildUrlWorldHazards", HazardType::class.java)
                .apply { isAccessible = true }
                .invoke(this, HazardType.Earthquakes) as String
        }
        assertTrue(url.contains("eventtypes=EQ"))
    }

    @Test
    fun `test buildUrlCountryHazards returns correct url`() {
        val url = repository.run {
            javaClass.getDeclaredMethod(
                "buildUrlCountryHazards",
                List::class.java, String::class.java, String::class.java, String::class.java, List::class.java
            ).apply { isAccessible = true }
                .invoke(this, listOf("EQ", "FL"), "United States", "2024-01-01", "2024-06-01", listOf("green", "red")) as String
        }
        assertTrue(url.contains("country=United%20States"))
        assertTrue(url.contains("eventlist=EQ;FL"))
        assertTrue(url.contains("fromDate=2024-01-01"))
    }

    @Test
    fun `test parseHazard returns null if not current`() {
        val json = JSONObject("""
            {
                "id": "1",
                "properties": {
                    "iscurrent": false,
                    "eventtype": "EQ",
                    "country": "France",
                    "fromdate": "2024-01-01",
                    "severitydata": {"severity": "5.0", "severityunit": "Mw"},
                    "url": {"report": "http://example.com"},
                    "alertscore": 2
                },
                "geometry": {
                    "type": "Point",
                    "coordinates": [2.0, 48.0]
                }
            }
        """.trimIndent())
        val hazard = repository.run {
            javaClass.getDeclaredMethod("parseHazard", JSONObject::class.java)
                .apply { isAccessible = true }
                .invoke(this, json) as Hazard?
        }
        assertNull(hazard)
    }

    @Test
    fun `test parseHazard returns Hazard if current`() {
        val json = JSONObject("""
            {
                "id": "2",
                "properties": {
                    "iscurrent": true,
                    "eventtype": "EQ",
                    "country": "France",
                    "fromdate": "2024-01-01",
                    "severitydata": {"severity": "5.0", "severityunit": "Mw"},
                    "url": {"report": "http://example.com"},
                    "alertscore": 2
                },
                "geometry": {
                    "type": "Point",
                    "coordinates": [2.0, 48.0]
                }
            }
        """.trimIndent())
        val hazard = repository.run {
            javaClass.getDeclaredMethod("parseHazard", JSONObject::class.java)
                .apply { isAccessible = true }
                .invoke(this, json) as Hazard?
        }
        assertNotNull(hazard)
        assertEquals("2", hazard?.id)
        assertEquals(HazardType.Earthquakes, hazard?.type)
        assertEquals("France", hazard?.country)
        assertEquals("5.0", hazard?.severity)
        assertEquals("Mw", hazard?.severityUnit)
        assertEquals("http://example.com", hazard?.reportUrl)
        assertEquals(2, hazard?.alertLevel)
        assertEquals(48.0, hazard?.coordinates?.first()?.latitude)
        assertEquals(2.0, hazard?.coordinates?.first()?.longitude)
    }

    @Test
    fun `test getCountryHazards returns hazards list`() = runBlocking {
        val fakeResponse = """
            {
                "features": [
                    {
                        "id": "3",
                        "properties": {
                            "iscurrent": true,
                            "eventtype": "FL",
                            "country": "France",
                            "fromdate": "2024-01-01",
                            "severitydata": {"severity": "high", "severityunit": "m"},
                            "url": {"report": "http://example.com"},
                            "alertscore": 3
                        },
                        "geometry": {
                            "type": "Point",
                            "coordinates": [2.0, 48.0]
                        }
                    }
                ]
            }
        """.trimIndent()
        coEvery { repository["httpGet"](any<String>()) } returns fakeResponse

        val hazards = repository.getCountryHazards(
            eventTypes = listOf("FL"),
            country = "France",
            fromDate = "2024-01-01",
            toDate = "2024-06-01",
            alertLevels = listOf("3")
        )
        assertEquals(1, hazards.size)
        assertEquals("3", hazards[0].id)
        assertEquals(HazardType.Floods, hazards[0].type)
    }

    @Test
    fun `test getAllWorldHazards returns hazards for all types`() = runBlocking {
        val fakeResponse = """
            {
                "features": [
                    {
                        "id": "4",
                        "properties": {
                            "iscurrent": true,
                            "eventtype": "VO",
                            "country": "Indonesia",
                            "fromdate": "2024-01-01",
                            "severitydata": {"severity": "medium", "severityunit": "VEI"},
                            "url": {"report": "http://example.com"},
                            "alertscore": 4
                        },
                        "geometry": {
                            "type": "Point",
                            "coordinates": [120.0, -3.0]
                        }
                    }
                ]
            }
        """.trimIndent()
        coEvery { repository["httpGet"](any<String>()) } returns fakeResponse

        val hazards = repository.getAllWorldHazards()
        assertEquals(4, hazards.size) // 4 types, 1 hazard per type
        assertTrue(hazards.all { it.type != null })
    }
}
