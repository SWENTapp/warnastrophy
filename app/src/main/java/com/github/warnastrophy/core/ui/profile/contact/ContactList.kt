package com.github.warnastrophy.core.ui.profile.contact

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.warnastrophy.core.ui.viewModel.Contact

// Create a list of mock contacts to display
val mockContacts =
    listOf(
        Contact("1", "Alice Johnson", "+1234567890", "Family"),
        Contact("2", "Dr. Robert Smith", "+9876543210", "Doctor"),
        Contact("3", "Chloé Dupont", "+41791234567", "Friend"),
        Contact("4", "Emergency Services", "911", "Critical"),
        Contact("5", "Michael Brown", "+447700900000", "Colleague"),
        Contact("6", "Grandma Sue", "+15551234567", "Family"),
        Contact("7", "Mr. Chen", "+8613800000000", "Neighbor"),
        Contact("8", "Security Guard Bob", "+18005551212", "Work"),
        Contact("9", "Zack Taylor", "+12341234123", "Friend"),
        Contact("10", "Yara Habib", "+971501112222", "Family"),
    )

@Composable
fun ContactItem(contact: Contact, onContactClick: () -> Unit) {
  Card(
      modifier =
          Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp).clickable {
            onContactClick
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
    contacts: List<Contact> = mockContacts,
    onContactClick: () -> Unit = {},
    onAddButtonClick: () -> Unit = {},
) {
  Scaffold(
      floatingActionButton = {
        FloatingActionButton(onClick = { onAddButtonClick() }) {
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
          LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            items(contacts, key = { it.id }) { contact ->
              ContactItem(contact = contact, onContactClick = { onContactClick() })
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
  MaterialTheme {
    ContactListScreen(
        contacts = mockContacts, // Use the mock data here
    )
  }
}
