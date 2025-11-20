import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import com.github.warnastrophy.core.domain.model.EmergencyMessage
import com.github.warnastrophy.core.domain.model.Location
import com.github.warnastrophy.core.domain.model.SmsManagerSender
import java.time.Instant
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class SmsServiceTests {
  @Mock private lateinit var mockContext: Context

  @Mock private lateinit var mockSmsManager: SmsManager
  private lateinit var smsManagerSender: SmsManagerSender
  private val phoneNumber: String = "0123456789"
  private val message =
      EmergencyMessage(
          text = "Engine room is on fire",
          timestamp = Instant.parse("2023-10-27T10:30:00Z"),
          location = Location(48.8584, 2.2945),
          additionalInfo = "Sector 7G")
  val expectedString =
      buildString {
            appendLine("🚨 EMERGENCY MESSAGE 🚨")
            appendLine()
            appendLine("Engine room is on fire")
            appendLine()
            appendLine("Time: October 27, 2023 at 12:30")
            appendLine()
            appendLine("Location:")
            appendLine("- Latitude: 48.8584")
            appendLine("- Longitude: 2.2945")
            appendLine("Map: https://www.google.com/maps?q=48.8584,2.2945")
            appendLine()
            appendLine("Additional information:")
            appendLine("Sector 7G")
          }
          .trimEnd()

  @Before
  fun setUp() {
    MockitoAnnotations.openMocks(this)
  }

  @Test
  @Config(sdk = [Build.VERSION_CODES.S]) // API 31
  fun sendSms_callsSendTextMessage_API_above_S() {
    Mockito.`when`(mockContext.getSystemService(SmsManager::class.java)).thenReturn(mockSmsManager)

    smsManagerSender = SmsManagerSender(mockContext)

    smsManagerSender.sendSms(phoneNumber, message)

    Mockito.verify(mockSmsManager).sendTextMessage(phoneNumber, null, expectedString, null, null)
  }

  @Test
  @Suppress("DEPRECATION")
  @Config(sdk = [Build.VERSION_CODES.R]) // API 30
  fun sendSms_callsSendTextMessage_API_below_S() {
    val mockedStaticSmsManager = Mockito.mockStatic(SmsManager::class.java)
    mockedStaticSmsManager.`when`<SmsManager> { SmsManager.getDefault() }.thenReturn(mockSmsManager)

    smsManagerSender = SmsManagerSender(mockContext)

    smsManagerSender.sendSms(phoneNumber, message)

    Mockito.verify(mockSmsManager).sendTextMessage(phoneNumber, null, expectedString, null, null)

    mockedStaticSmsManager.close()
  }
}
