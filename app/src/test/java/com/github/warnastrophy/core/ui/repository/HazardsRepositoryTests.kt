import android.util.Log
import com.github.warnastrophy.core.ui.repository.HazardsRepository
import kotlinx.coroutines.runBlocking

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.lang.Thread.sleep

class HazardsRepositoryIntegrationTest {

    @Test
    fun `getAreaHazards with US polygon returns non empty list`() = runBlocking {
        val repo = HazardsRepository()
        // Polygone simplifié des USA (format WKT ou GeoJSON selon l'API attendue)
        val usPolygon = "POLYGON((-125 24, -66 24, -66 49, -125 49, -125 24))"
        val hazards = repo.getAreaHazards(usPolygon)
        sleep(2000) // Attendre 2 secondes pour s'assurer que la requête est terminée
        Log.d("Test", "Hazards: $hazards")
        assertTrue(hazards.isNotEmpty())
    }
}