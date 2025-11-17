package com.github.warnastrophy.core.domain.model

import java.time.Instant

/**
 * Represents an emergency message containing location, time, and contextual info.
 *
 * @property text The main content of the emergency message.
 * @property timestamp The exact time the message was generated.
 * @property location The geographical location associated with the message.
 * @property additionalInfo Optional extra information to include in the message.
 */
data class EmergencyMessage(
    val text: String,
    val timestamp: Instant,
    val location: Location,
    val additionalInfo: String? = null,
) {
  /**
   * Secondary constructor to create an EmergencyMessage with the current timestamp. This simplifies
   * object creation by automatically setting the time.
   */
  constructor(
      text: String = DEFAULT_TEXT,
      location: Location,
      additionalInfo: String? = null
  ) : this(
      text = text, timestamp = Instant.now(), location = location, additionalInfo = additionalInfo)

  /**
   * Provides a user-friendly, multi-line string representation of the emergency message. Ideal for
   * display in UI components, logs, or notifications.
   */
  fun toStringMessage(): String {
    val additionalInfoText =
        if (!additionalInfo.isNullOrBlank()) {
          "- Additional Info: $additionalInfo"
        } else {
          ""
        }

    return """
            $text
            
            - Time: $timestamp
            - Location: ${location.latitude}, ${location.longitude}
            $additionalInfoText
        """
        .trimIndent()
  }

  companion object {
    /** The default text used for emergency messages when none is provided. */
    const val DEFAULT_TEXT = "EMERGENCY: I need immediate assistance! Call the emergency services!"
  }
}
