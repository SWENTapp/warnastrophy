package com.github.warnastrophy.core.ui.profile.contact

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.warnastrophy.core.ui.viewModel.EditContactViewModel

@Composable
fun EditContactScreen(
    contactID: String,
    editContactViewModel: EditContactViewModel = viewModel(),
    onDone: () -> Unit = {}
) {
  // TODO: complete when local storage done
  LaunchedEffect(contactID) { editContactViewModel.loadContact(contactID) }

  val contactUIState by editContactViewModel.uiState.collectAsState()
  val errorMsg = contactUIState.errorMsg

  val context = LocalContext.current

  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
      editContactViewModel.clearErrorMsg()
    }
  }

  Column(
      modifier = Modifier.fillMaxSize().padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Add Contact Form",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp))

        // --- Input Field: Full Name ---
        OutlinedTextField(
            value = contactUIState.fullName,
            onValueChange = { editContactViewModel.setFullName(it) },
            label = { Text("Full Name") },
            isError = contactUIState.invalidFullNameMsg != null,
            supportingText = { contactUIState.invalidFullNameMsg?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))

        // --- Input Field: Phone Number ---
        OutlinedTextField(
            value = contactUIState.phoneNumber,
            onValueChange = { editContactViewModel.setPhoneNumber(it) },
            label = { Text("Phone number") },
            // Note: Use KeyboardOptions to hint at numeric input
            // keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = contactUIState.invalidPhoneNumberMsg != null,
            supportingText = { contactUIState.invalidPhoneNumberMsg?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))

        // --- Input Field: Relationship ---
        OutlinedTextField(
            value = contactUIState.relationship,
            onValueChange = { editContactViewModel.setRelationShip(it) },
            label = { Text("Relationship (e.g., family, friend, doctor, etc.)") },
            isError = contactUIState.invalidRelationshipMsg != null,
            supportingText = { contactUIState.invalidRelationshipMsg?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp))

        // --- Save Button with Validation ---
        Button(
            onClick = {
              if (editContactViewModel.editContact(contactID)) {
                onDone()
                // TODO: Add navigate back here

              }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)) {
              Text("Save Contact")
            }

        Button(
            onClick = {
              editContactViewModel.deleteContact(contactID)
              onDone()
            }) {
              Text("Delete", color = Color.White)
            }
      }
}
