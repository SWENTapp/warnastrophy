/** Fait par anas sifi mohamed et Gemini comme assistant. */
package com.github.warnastrophy.core.data.service

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

/**
 * Tests unitaires pour la classe [SpeechToTextService].
 *
 * Ces tests se concentrent sur la logique de la fonction `parseConfirmation` pour s'assurer qu'elle
 * identifie correctement les réponses positives, négatives et non valides.
 */
@RunWith(MockitoJUnitRunner::class)
class SpeechToTextServiceTest {

  // Mock du contexte Android, nécessaire pour instancier le service.
  @Mock private lateinit var mockContext: Context

  private lateinit var speechToTextService: SpeechToTextService

  @Before
  fun setUp() {
    // Initialisation du service avec le contexte mocké avant chaque test.
    speechToTextService = SpeechToTextService(mockContext)
  }

  @Test
  fun `parseConfirmation avec 'yes' retourne true`() {
    assertEquals(true, speechToTextService.parseConfirmation("yes"))
  }

  @Test
  fun `parseConfirmation avec 'YES' en majuscules retourne true`() {
    assertEquals(true, speechToTextService.parseConfirmation("YES"))
  }

  @Test
  fun `parseConfirmation avec 'yeah' retourne true`() {
    assertEquals(true, speechToTextService.parseConfirmation("yeah"))
  }

  @Test
  fun `parseConfirmation avec ' Yeah ' avec espaces retourne true`() {
    assertEquals(true, speechToTextService.parseConfirmation(" Yeah "))
  }

  @Test
  fun `parseConfirmation avec 'no' retourne false`() {
    assertEquals(false, speechToTextService.parseConfirmation("no"))
  }

  @Test
  fun `parseConfirmation avec 'NO' en majuscules retourne false`() {
    assertEquals(false, speechToTextService.parseConfirmation("NO"))
  }

  @Test
  fun `parseConfirmation avec ' No ' avec espaces retourne false`() {
    assertEquals(false, speechToTextService.parseConfirmation(" No "))
  }

  @Test
  fun `parseConfirmation avec une chaîne aléatoire retourne null`() {
    assertNull(speechToTextService.parseConfirmation("hello world"))
  }

  @Test
  fun `parseConfirmation avec une chaîne vide retourne null`() {
    assertNull(speechToTextService.parseConfirmation(""))
  }

  @Test
  fun `parseConfirmation avec une valeur nulle retourne null`() {
    assertNull(speechToTextService.parseConfirmation(null))
  }
}
