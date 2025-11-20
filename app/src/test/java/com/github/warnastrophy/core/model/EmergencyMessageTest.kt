package com.github.warnastrophy.core.model

import com.github.warnastrophy.core.domain.model.EmergencyMessage
import com.github.warnastrophy.core.domain.model.Location
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.junit.Assert.*
import org.junit.Test

class EmergencyMessageTest {
  private val location = Location(11.0, 22.0)

  @Test
  fun secondary_constructor_should_use_default_text_and_current_timestamp() {
    val creationTime = Instant.now()

    val message = EmergencyMessage(location = location)

    assertEquals(EmergencyMessage.DEFAULT_TEXT, message.text)
    assertEquals(location, message.location)
    assertNull(message.additionalInfo)

    // Check timestamp recent
    val secondsDifference = ChronoUnit.SECONDS.between(message.timestamp, creationTime)
    assertTrue("Timestamp should be very close to the time of creation", secondsDifference <= 1)
  }

  @Test
  fun secondary_constructor_with_custom_text_should_set_all_properties_correctly() {
    val customText = "Help! Fire in building."
    val additionalInfo = "I'm on the third floor."
    val creationTime = Instant.now()

    val message = EmergencyMessage(customText, location, additionalInfo)

    assertEquals(customText, message.text)
    assertEquals(location, message.location)
    assertEquals(additionalInfo, message.additionalInfo)

    // Check timestamp recent
    val secondsDifference = ChronoUnit.SECONDS.between(message.timestamp, creationTime)
    assertTrue("Timestamp should be very close to the time of creation", secondsDifference <= 1)
  }

  @Test
  fun primary_constructor_should_set_all_properties_exactly_as_provided() {
    val customText = "Help! Fire in building."
    val specificTimestamp = Instant.parse("2024-01-01T00:00:00Z")
    val additionalInfo = "Apartment 12B"

    val message = EmergencyMessage(customText, specificTimestamp, location, additionalInfo)

    assertEquals(customText, message.text)
    assertEquals(specificTimestamp, message.timestamp) // Check for the exact timestamp
    assertEquals(location, message.location)
    assertEquals(additionalInfo, message.additionalInfo)
  }

  @Test
  fun toString_should_format_the_message_correctly_with_all_details() {
    val timestamp = Instant.parse("2023-10-27T10:30:00Z")
    val message =
        EmergencyMessage(
            text = "Engine room is on fire",
            timestamp = timestamp,
            location = Location(48.8584, 2.2945),
            additionalInfo = "Sector 7G")
    val expectedString =
        """
                🚨 EMERGENCY MESSAGE 🚨
    
                Engine room is on fire
    
                Time: October 27, 2023 at 12:30
    
                Location:
                - Latitude: 48.8584
                - Longitude: 2.2945
                Map: https://www.google.com/maps?q=48.8584,2.2945
    
                Additional information:
                Sector 7G
            """
            .trimIndent()

    assertEquals(expectedString, message.toStringMessage())
  }

  @Test
  fun toString_should_format_correctly_when_additionalInfo_is_null() {
    val timestamp = Instant.parse("2023-10-27T10:30:00Z")
    val message =
        EmergencyMessage(
            text = "Medical assistance required",
            timestamp = timestamp,
            location = Location(48.8584, 2.2945),
            additionalInfo = null // Explicitly null
            )
    val expectedString =
        """
                🚨 EMERGENCY MESSAGE 🚨

                Medical assistance required

                Time: October 27, 2023 at 12:30

                Location:
                - Latitude: 48.8584
                - Longitude: 2.2945
                Map: https://www.google.com/maps?q=48.8584,2.2945
            """
            .trimIndent()

    assertEquals(expectedString, message.toStringMessage())
  }
}
