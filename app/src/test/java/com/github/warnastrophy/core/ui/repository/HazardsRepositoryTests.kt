package com.github.warnastrophy.core.ui.repository

import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class HazardsRepositoryIntegrationTest {

    @Test
    fun `getAreaHazards retourne les bons floods en Italie`() = runBlocking {
        val repo = HazardsRepository()
        val geometry = "POLYGON((6.0 45.8,6.0 47.8,10.5 47.8,10.5 45.8,6.0 45.8))"
        // Ajoute le paramètre days=360
        val url = "https://www.gdacs.org/gdacsapi/api/Events/geteventlist/eventsbyarea?geometryArea=$geometry&days=360"
        var response: List<Hazard> = emptyList()
        GlobalScope.launch { response = repo.getAreaHazards(geometry) }

        print("Response: $response")
        // Vérifie qu'il y a au moins 2 événements
        assertTrue(response.size >= 2)

        // Vérifie les champs principaux du premier événement
        val flood1 = response[0]
        assertEquals("Italy", flood1.country)
        assertEquals("0.0", flood1.severity)
        assertEquals(1, flood1.alertLevel)
        assertNotNull(flood1.reportUrl)
        assertNotNull(flood1.coordinates)
        assertEquals(1, flood1.coordinates!!.size)
        assertEquals(45.6427259, flood1.coordinates!![0].latitude, 1e-6)
        assertEquals(13.0546698, flood1.coordinates!![0].longitude, 1e-6)
    }
}
