package com.github.warnastrophy.core.ui.profile.contact

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// Utility function for basic phone number format validation
fun isValidPhoneNumber(phone: String): Boolean {
  // Regex for basic validation: optional '+' at start, followed by 10-15 digits
  return phone.matches(Regex("^\\+?[0-9]{10,15}\$"))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactScreen(
    // Optional: Add a callback to handle the save action and pass the contact data
    onContactSaved: (fullName: String, phone: String, relationship: String) -> Unit = { _, _, _ -> }
) {
  // 1. State variables to hold the user input
  var fullName by remember { mutableStateOf("") }
  var phoneNumber by remember { mutableStateOf("") }
  var relationship by remember { mutableStateOf("") }

  // 2. State variables for error messages (Form Validation)
  var fullNameError by remember { mutableStateOf(false) }
  var phoneError by remember { mutableStateOf(false) }

  Column(
      modifier = Modifier.fillMaxSize().padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Add Contact Form",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp))

        // --- Input Field: Full Name ---
        OutlinedTextField(
            value = fullName,
            onValueChange = {
              fullName = it
              fullNameError = false // Clear error on change
            },
            label = { Text("Full Name") },
            isError = fullNameError,
            supportingText = {
              if (fullNameError) {
                Text("Full Name is required")
              }
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))

        // --- Input Field: Phone Number ---
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = {
              phoneNumber = it
              phoneError = false // Clear error on change
            },
            label = { Text("Phone number") },
            // Note: Use KeyboardOptions to hint at numeric input
            // keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = phoneError,
            supportingText = {
              if (phoneError) {
                Text("Valid phone number is required (e.g., 10-15 digits)")
              }
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))

        // --- Input Field: Relationship ---
        OutlinedTextField(
            value = relationship,
            onValueChange = { relationship = it },
            label = { Text("Relationship (e.g., family, friend, doctor, etc.)") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp))

        // --- Save Button with Validation ---
        Button(
            onClick = {
              // Reset errors before validation
              fullNameError = false
              phoneError = false

              // 3. Form Validation Logic
              var isValid = true

              // Full Name Validation
              if (fullName.isBlank()) {
                fullNameError = true
                isValid = false
              }

              // Phone Number Validation (Required and Formatting)
              if (phoneNumber.isBlank() || !isValidPhoneNumber(phoneNumber)) {
                phoneError = true
                isValid = false
              }

              if (isValid) {
                // All validation passed, call the save callback
                onContactSaved(fullName, phoneNumber, relationship)
                // TODO: Add navigate back here
                println("Contact Saved: $fullName, $phoneNumber, $relationship")
              }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)) {
              Text("Save Contact")
            }
      }
}

@Preview(showBackground = true)
@Composable
fun AddContactScreenPreview() {
  // Assuming you have a MainAppTheme or just use the system default
  MaterialTheme { AddContactScreen() }
}
