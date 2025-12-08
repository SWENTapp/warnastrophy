package com.github.warnastrophy.core.ui.features.contact

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.warnastrophy.R
import com.github.warnastrophy.core.model.Contact
import com.github.warnastrophy.core.ui.theme.extendedColors

object ContactPopUpTestTags {
  const val ROOT_CARD = "contactPopUpRootCard"
  const val TITLE = "contactPopUpTitle"
  const val VIEW_ALL_BUTTON = "contactPopUpViewAllButton"
  const val CONTENT_CARD = "contactPopUpContentCard"
  const val CONTACT_LIST = "contactPopUpContactList"
  const val EMPTY_STATE = "contactPopUpEmptyState"
  const val ERROR_STATE = "contactPopUpErrorState"

  fun contactItem(contactId: String) = "contactPopUpItem_$contactId"
}

/**
 * A composable function that displays a list of emergency contacts in a dialog pop-up.
 *
 * This dialog fetches the contact list using the [ContactListViewModel]. It shows a title and a
 * forward arrow button that triggers the [onClick] lambda, which can be used to navigate to the
 * full contact list view. The main content area displays a list of contacts, an empty state
 * message, or an error message. Each contact in the list is clickable, triggering [onContactClick].
 * The pop-up can be dismissed via [onDismissRequest].
 *
 * @param userId The unique identifier for the user whose contacts are to be displayed.
 * @param onDismissRequest A lambda function to be invoked when the user requests to dismiss the
 *   dialog.
 * @param onClick A lambda function to be invoked when the forward arrow button is clicked,
 *   typically for navigation.
 * @param onContactClick A lambda function to be invoked when a specific contact item is clicked,
 *   passing the [Contact] object.
 * @param viewModel An instance of [ContactListViewModel] used to fetch and manage the contact list.
 */
@Composable
fun ContactPopUp(
    userId: String,
    onDismissRequest: () -> Unit,
    onClick: () -> Unit = {},
    onContactClick: (Contact) -> Unit = {},
    viewModel: ContactListViewModel = viewModel(factory = ContactListViewModelFactory(userId))
) {
  val uiState by viewModel.uiState.collectAsState()
  val colors = MaterialTheme.extendedColors.dashboardPopUp

  LaunchedEffect(Unit) { viewModel.refreshUIState() }

  Dialog(onDismissRequest = onDismissRequest) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier =
            Modifier.fillMaxWidth().fillMaxHeight(0.7f).testTag(ContactPopUpTestTags.ROOT_CARD),
        colors = CardDefaults.cardColors(containerColor = colors.primary)) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                  Text(
                      text = stringResource(R.string.emergency_contacts_title),
                      color = colors.secondary,
                      fontWeight = FontWeight.Bold,
                      fontSize = 20.sp,
                      modifier = Modifier.testTag(ContactPopUpTestTags.TITLE))
                  TextButton(
                      onClick = onClick,
                      modifier = Modifier.testTag(ContactPopUpTestTags.VIEW_ALL_BUTTON)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription =
                                stringResource(R.string.emergency_contacts_show_full_button),
                            tint = colors.secondary,
                            modifier = Modifier.size(16.dp))
                      }
                }

            Spacer(modifier = Modifier.height(6.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.secondary),
                modifier =
                    Modifier.fillMaxWidth().weight(1f).testTag(ContactPopUpTestTags.CONTENT_CARD),
            ) {
              if (uiState.contacts.isEmpty() && uiState.errorMsg == null) {
                EmptyState()
              } else if (uiState.errorMsg != null) {
                ErrorState(message = uiState.errorMsg!!)
              } else {
                ContactList(contacts = uiState.contacts, onClick = onContactClick)
              }
            }
          }
        }
  }
}

/**
 * A composable that displays a scrollable list of contacts.
 *
 * This function uses a `LazyColumn` to efficiently display a list of [Contact] objects. Each
 * contact is rendered using the [ContactItem] composable.
 *
 * @param contacts The list of [Contact] objects to be displayed.
 * @param onClick A lambda function to be invoked when a contact item is clicked, passing the
 *   corresponding [Contact] object.
 */
@Composable
private fun ContactList(contacts: List<Contact>, onClick: (Contact) -> Unit) {
  LazyColumn(modifier = Modifier.padding(8.dp).testTag(ContactPopUpTestTags.CONTACT_LIST)) {
    items(contacts) { contact ->
      ContactItem(contact = contact, onClick = { onClick(contact) })
      Spacer(modifier = Modifier.height(8.dp))
    }
  }
}

/**
 * A composable that displays a single contact item in a card layout.
 *
 * @param contact The [Contact] object containing the data to be displayed.
 * @param onClick A lambda function to be invoked when the contact item is clicked.
 */
@Composable
private fun ContactItem(contact: Contact, onClick: () -> Unit) {
  val colors = MaterialTheme.extendedColors.dashboardPopUp
  Card(
      modifier = Modifier.fillMaxWidth().testTag(ContactPopUpTestTags.contactItem(contact.id)),
      shape = RoundedCornerShape(12.dp),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      colors =
          CardDefaults.cardColors(
              containerColor = colors.secondary.compositeOver(MaterialTheme.colorScheme.onSurface)),
      onClick = onClick,
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = stringResource(R.string.emergency_contact_name),
            tint = colors.primary,
            modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = contact.fullName,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = colors.primary)
      }
      Spacer(modifier = Modifier.height(8.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Phone,
            contentDescription = stringResource(R.string.emergency_contact_phone_number),
            tint = colors.fieldText,
            modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${contact.relationship} - ${contact.phoneNumber}",
            fontSize = 16.sp,
            color = colors.fieldText)
      }
    }
  }
}

/**
 * A composable function that displays a message indicating that the emergency contact list is
 * empty.
 */
@Composable
private fun EmptyState() {
  Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(id = R.string.emergency_contacts_popup_empty),
            color = MaterialTheme.extendedColors.dashboardPopUp.fieldText,
            modifier = Modifier.testTag(ContactPopUpTestTags.EMPTY_STATE))
      }
}

/**
 * A composable function that displays a centered error message.
 *
 * @param message The specific error message string to be displayed.
 */
@Composable
private fun ErrorState(message: String) {
  Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.testTag(ContactPopUpTestTags.ERROR_STATE))
      }
}
