package com.github.warnastrophy.core.ui.features.contact

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.warnastrophy.R
import com.github.warnastrophy.core.model.Contact
import com.github.warnastrophy.core.ui.theme.extendedColors

/**
 * A composable function that displays a list of emergency contacts in a dialog pop-up.
 *
 * This dialog fetches the contact list using the [ContactListViewModel]. It shows a title and an
 * "Add" button that triggers the [onAddContactClick] lambda. The main content area displays a list
 * of contacts or a message if the list is empty. The pop-up can be dismissed via
 * [onDismissRequest].
 *
 * @param userId The unique identifier for the user whose contacts are to be displayed.
 * @param onDismissRequest A lambda function to be invoked when the user requests to dismiss the
 *   dialog.
 * @param onAddContactClick A lambda function to be invoked when the "Add" button is clicked.
 * @param viewModel An instance of [ContactListViewModel] used to fetch and manage the contact list.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ContactPopUp(
    userId: String,
    onDismissRequest: () -> Unit,
    onClick: () -> Unit = {},
    onContactClick: (Contact) -> Unit = {},
    viewModel: ContactListViewModel = viewModel(factory = ContactListViewModelFactory(userId))
) {
  val uiState by viewModel.uiState.collectAsState()
  val colors = MaterialTheme.extendedColors.healthCardPopUp

  LaunchedEffect(Unit) { viewModel.refreshUIState() }

  Dialog(onDismissRequest = onDismissRequest) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.7f),
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
                      fontSize = 20.sp)
                  TextButton(onClick = onClick) {
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
                modifier = Modifier.fillMaxWidth().weight(1f),
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

@Composable
private fun ContactList(contacts: List<Contact>, onClick: (Contact) -> Unit) {
  LazyColumn(modifier = Modifier.padding(8.dp)) {
    items(contacts) { contact ->
      ContactItem(contact = contact, onClick = { onClick(contact) })
      Spacer(modifier = Modifier.height(8.dp))
    }
  }
}

@Composable
private fun ContactItem(contact: Contact, onClick: () -> Unit) {
  val colors = MaterialTheme.extendedColors.healthCardPopUp
  Card(
      modifier = Modifier.fillMaxWidth(),
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

@Composable
private fun EmptyState() {
  Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.emergency_contact_empty_list),
            color = MaterialTheme.extendedColors.healthCardPopUp.fieldText)
      }
}

@Composable
private fun ErrorState(message: String) {
  Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
      }
}
