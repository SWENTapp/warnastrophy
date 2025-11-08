package com.github.warnastrophy.core.ui.features.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.warnastrophy.core.data.repository.ContactRepositoryProvider
import com.github.warnastrophy.core.model.Contact
import com.github.warnastrophy.core.ui.components.Loading
import com.github.warnastrophy.core.ui.components.StandardDashboardButton
import com.github.warnastrophy.core.ui.components.StandardDashboardCard
import com.github.warnastrophy.core.ui.theme.MainAppTheme
import kotlinx.coroutines.launch

/**
 * Test tags for the Dashboard Emergency Contacts Card component.
 *
 * Use these constants to identify UI elements in automated tests.
 */
object DashboardEmergencyContactsTestTags {
  const val CARD = "dashboardEmergencyContactsCard"
  const val TITLE = "dashboardEmergencyContactsTitle"
  const val CONTACT_ITEM_PREFIX = "dashboardEmergencyContactItem_"
  const val NO_CONTACTS_TEXT = "dashboardEmergencyContactsNoContacts"
  const val MANAGE_BUTTON = "dashboardEmergencyContactsManageButton"
}

/**
 * Color scheme for the Dashboard Emergency Contacts Card.
 *
 * Defines the visual appearance with a light yellow background and gray text variants.
 */
object DashboardEmergencyContactsCardColors {
  fun getColors(colorScheme: ColorScheme): Colors {
    return Colors(
        backgroundColor =
            colorScheme.surface.copy(red = 1f, green = 1f, blue = 0.8f), // light yellow
        textColor = colorScheme.onSurface, // Dark Gray text
        subtitleColor = colorScheme.onSurfaceVariant // Medium Gray subtitle
        )
  }
}

data class Colors(val backgroundColor: Color, val textColor: Color, val subtitleColor: Color)

/**
 * Displays a dashboard card showing emergency contacts with management controls.
 *
 * This is a stateless composable that displays up to 2 emergency contacts in a card format. The
 * card includes a "Manage" button for navigating to contact management and handles three states:
 * loading, empty, and populated with contacts.
 *
 * @param contacts The list of emergency contacts to display. Only the first 2 contacts will be
 *   shown.
 * @param onManageContactsClick Callback invoked when the user clicks the "Manage" button.
 * @param modifier Modifier to be applied to the card container.
 * @param isLoading Whether the contacts are currently being loaded. When true, displays a loading
 *   indicator.
 * @see DashboardEmergencyContactsCardStateful Stateful version that automatically fetches contacts
 */
@Composable
fun DashboardEmergencyContactsCardStateless(
    contacts: List<Contact>,
    onManageContactsClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false
) {
  val colorScheme = MaterialTheme.colorScheme
  val colors = DashboardEmergencyContactsCardColors.getColors(colorScheme)
  StandardDashboardCard(
      modifier = modifier.testTag(DashboardEmergencyContactsTestTags.CARD),
      backgroundColor = colors.backgroundColor,
      minHeight = 120.dp,
      maxHeight = 150.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween) {
              // Header
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Contacts",
                        color = colors.textColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag(DashboardEmergencyContactsTestTags.TITLE),
                        fontSize = 16.sp)

                    Box(
                        modifier =
                            Modifier.testTag(DashboardEmergencyContactsTestTags.MANAGE_BUTTON)) {
                          StandardDashboardButton(label = "Manage", onClick = onManageContactsClick)
                        }
                  }

              // Content area
              when {
                isLoading -> {
                  Box(
                      modifier = Modifier.fillMaxWidth().padding(16.dp),
                      contentAlignment = Alignment.Center) {
                        Loading()
                      }
                }
                contacts.isEmpty() -> {
                  Text(
                      text = "No emergency contacts added",
                      color = colors.subtitleColor,
                      fontSize = 14.sp,
                      modifier =
                          Modifier.testTag(DashboardEmergencyContactsTestTags.NO_CONTACTS_TEXT))
                }
                else -> {
                  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    contacts.take(2).forEachIndexed { index, contact ->
                      ContactItem(
                          contact = contact,
                          modifier =
                              Modifier.testTag(
                                  "${DashboardEmergencyContactsTestTags.CONTACT_ITEM_PREFIX}$index"))
                    }
                  }
                }
              }
            }
      }
}

/**
 * Displays a single contact item showing the contact's full name and phone number.
 *
 * The format is "Full Name • Phone Number" with medium font weight.
 *
 * @param contact The contact information to display.
 * @param modifier Modifier to be applied to the text component.
 */
@Composable
private fun ContactItem(contact: Contact, modifier: Modifier = Modifier) {
  Text(
      text = "${contact.fullName}: ${contact.phoneNumber}",
      color = DashboardEmergencyContactsCardColors.getColors(MaterialTheme.colorScheme).textColor,
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium,
      modifier = modifier)
}

/**
 * Displays a dashboard card showing emergency contacts with automatic data fetching.
 *
 * This is a stateful composable that automatically fetches emergency contacts from the
 * [ContactRepositoryProvider] on composition. It manages its own loading state and delegates
 * rendering to the stateless version of this composable.
 *
 * The card will fetch contacts once when first composed using [LaunchedEffect]. If the fetch fails,
 * an empty contact list is displayed.
 *
 * @param onManageContactsClick Callback invoked when the user clicks the "Manage" button.
 * @param modifier Modifier to be applied to the card container.
 * @see DashboardEmergencyContactsCardStateful Stateless version that accepts contacts as a
 *   parameter
 */
@Composable
fun DashboardEmergencyContactsCardStateful(
    onManageContactsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
  var isLoading by remember { mutableStateOf(true) }
  val scope = rememberCoroutineScope()

  LaunchedEffect(Unit) {
    scope.launch {
      ContactRepositoryProvider.repository
          .getAllContacts()
          .onSuccess { contactList ->
            contacts = contactList
            isLoading = false
          }
          .onFailure {
            contacts = emptyList()
            isLoading = false
          }
    }
  }

  DashboardEmergencyContactsCardStateless(
      contacts = contacts,
      onManageContactsClick = onManageContactsClick,
      modifier = modifier,
      isLoading = isLoading)
}

@Preview(showBackground = true, name = "With Contacts")
@Composable
private fun DashboardEmergencyContactsCardPreview() {
  MainAppTheme {
    DashboardEmergencyContactsCardStateless(
        contacts =
            listOf(
                Contact(
                    id = "1",
                    fullName = "Jane Doe",
                    phoneNumber = "+1 555-123-4567",
                    relationship = "Mom"),
                Contact(
                    id = "2",
                    fullName = "John Smith",
                    phoneNumber = "+1 555-987-6543",
                    relationship = "Dad")),
        onManageContactsClick = {})
  }
}
