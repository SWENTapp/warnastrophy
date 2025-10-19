package com.github.warnastrophy.core.ui.profile.contact

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.warnastrophy.core.model.Contact
import com.github.warnastrophy.core.ui.viewModel.ContactListViewModel

object ContactListScreenTestTags {
    const val ADD_CONTACT_BUTTON =  "addButton"
    const val CONTACT_LIST = "contactList"
    fun getTestTagForContactItem(contact: Contact) : String = "contactItem${contact.id}"
}

@Composable
fun ContactItem(contact: Contact, onContactClick: () -> Unit) {
    Card(
        modifier =
            Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp)
                .testTag(ContactListScreenTestTags.getTestTagForContactItem(contact = contact))
                .clickable {
                    onContactClick()
                } // Handle click on the whole item
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                // Contact Name
                Text(text = contact.fullName, style = MaterialTheme.typography.titleMedium)
                // Relationship
                Text(
                    text = "Relationship: ${contact.relationship}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Phone Icon and Number
            Row {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Phone",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp).padding(end = 4.dp))
                Text(text = contact.phoneNumber, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactListScreen(
    contactListViewModel: ContactListViewModel = viewModel(),
    // contacts: List<Contact> = mockContacts,
    onContactClick: (Contact) -> Unit = {},
    onAddButtonClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val uiState by contactListViewModel.uiState.collectAsState()
    val contacts = uiState.contacts

    // Fetch Contacts when the screen is recomposed
    LaunchedEffect(Unit) { contactListViewModel.refreshUIState() }

    // Show error message if fetching Contacts fails
    LaunchedEffect(uiState.errorMsg) {
        uiState.errorMsg?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            contactListViewModel.clearErrorMsg()
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddButtonClick() },
                modifier = Modifier.testTag(ContactListScreenTestTags.ADD_CONTACT_BUTTON)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Contact")
            }
        }) { paddingValues ->
        if (contacts.isEmpty()) {
            // Display a message if the list is empty
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No contacts found. Tap '+' to add one.")
            }
        } else {
            // Display the list of contacts
            LazyColumn(
                modifier =
                    Modifier.fillMaxSize().padding(paddingValues)
                        .testTag(
                ContactListScreenTestTags.CONTACT_LIST)
            ) {
                items(contacts, key = { it.id }) { contact ->
                    ContactItem(contact = contact, onContactClick = { onContactClick(contact) })
                    HorizontalDivider(
                        Modifier,
                        DividerDefaults.Thickness,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ContactListScreenPreview() {
    // Assuming you have a MainAppTheme or just use the system default
    MaterialTheme { ContactListScreen() }
}
