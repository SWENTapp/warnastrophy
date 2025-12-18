package com.github.warnastrophy.core.ui.features.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.github.warnastrophy.R
import com.github.warnastrophy.core.model.Location
import com.github.warnastrophy.core.model.Location.Companion.toLatLng
import com.github.warnastrophy.core.ui.features.map.Colors.horizontalDividerColor
import com.google.maps.android.compose.CameraPositionState
import kotlinx.coroutines.launch

/** Color constants used in the map search bar UI components. */
object Colors {
  /** Color for horizontal dividers between dropdown items. */
  val horizontalDividerColor: Color = Color(0xFF3B3B3B)
}

/**
 * A search bar component for searching locations on the map.
 *
 * This composable provides a text input field with autocomplete suggestions from Nominatim. When a
 * suggestion is selected, the map camera animates to that location.
 *
 * @param modifier Modifier to be applied to the search bar container.
 * @param viewModel The [MapViewModel] that handles search logic and state.
 * @param cameraPositionState The camera state used to animate the map to selected locations.
 * @param focusManager The focus manager used to clear focus when a selection is made.
 */
@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    viewModel: MapViewModel,
    cameraPositionState: CameraPositionState,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
  val coroutineScope = rememberCoroutineScope()
  val focusRequester = remember { FocusRequester() }
  val interactionSource = remember { MutableInteractionSource() }

  var text by remember { mutableStateOf("") }
  var expanded by remember { mutableStateOf(false) }

  // Track focus state from interaction source for better device compatibility
  val isFocused by interactionSource.collectIsFocusedAsState()

  val uiState by viewModel.uiState.collectAsState()
  val suggestions = uiState.nominatimState

  // Update dropdown visibility based on focus, text content and available suggestions
  LaunchedEffect(suggestions, text, isFocused) {
    expanded = isFocused && text.isNotEmpty() && suggestions.isNotEmpty()
  }

  Box(modifier = modifier.fillMaxWidth(0.75f).testTag(MapScreenTestTags.SEARCH_BAR)) {
    Column {
      SearchTextField(
          text = text,
          onTextChange = { newText ->
            text = newText
            viewModel.searchLocations(newText)
          },
          focusRequester = focusRequester,
          interactionSource = interactionSource,
          onClearClick = {
            text = ""
            viewModel.searchLocations("")
          },
          onSearchAction = {
            expanded = false
            focusManager.clearFocus()
          },
          modifier = Modifier.fillMaxWidth())

      SuggestionsDropdown(
          expanded = expanded,
          suggestions = suggestions,
          modifier = Modifier.fillMaxWidth(0.75f).background(Color.White),
          onDismiss = {
            expanded = false
            focusManager.clearFocus()
          },
          onSelect = { loc ->
            val name = loc.name ?: ""
            text = name
            expanded = false
            focusManager.clearFocus()
            viewModel.setSelectedLocation(loc)
            coroutineScope.launch { defaultAnimate(cameraPositionState, toLatLng(loc)) }
          })
    }
  }
}

/**
 * Internal text field component for the search bar.
 *
 * Displays a search icon, text input field with placeholder, and a clear button when text is
 * present. The entire row is clickable to request focus, which improves compatibility on physical
 * Android devices.
 *
 * @param text Current text value of the search field.
 * @param onTextChange Callback invoked when the text changes.
 * @param focusRequester Focus requester used to programmatically request focus.
 * @param interactionSource Interaction source to track focus state reliably.
 * @param onClearClick Callback invoked when the clear button is clicked.
 * @param onSearchAction Callback invoked when the search IME action is triggered.
 * @param modifier Modifier to be applied to the text field container.
 */
@Composable
private fun SearchTextField(
    text: String,
    onTextChange: (String) -> Unit,
    focusRequester: FocusRequester,
    interactionSource: MutableInteractionSource,
    onClearClick: () -> Unit,
    onSearchAction: () -> Unit,
    modifier: Modifier = Modifier
) {
  Row(
      modifier =
          modifier
              .background(Color.White, RoundedCornerShape(16.dp))
              .clickable(
                  interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    // Request focus when the row is tapped to re-enable editing
                    focusRequester.requestFocus()
                  }
              .padding(horizontal = 8.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = stringResource(R.string.search_bar_icon),
            tint = Color.Black,
            modifier = Modifier.padding(start = 8.dp))

        Spacer(Modifier.width(12.dp))

        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            modifier =
                Modifier.weight(1f)
                    .focusRequester(focusRequester)
                    .testTag(MapScreenTestTags.SEARCH_BAR_TEXT_FIELD),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(color = Color.Black),
            cursorBrush = SolidColor(Color.Black),
            interactionSource = interactionSource,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearchAction() }),
            decorationBox = { innerTextField ->
              Box(contentAlignment = Alignment.CenterStart) {
                if (text.isEmpty()) {
                  Text(
                      stringResource(R.string.search_bar_search),
                      color = Color.Black.copy(alpha = 0.6f))
                }
                innerTextField()
              }
            })

        // Clear button shown only when there is text
        if (text.isNotEmpty()) {
          IconButton(
              onClick = {
                onClearClick()
                focusRequester.requestFocus()
              },
              modifier = Modifier.size(24.dp).testTag(MapScreenTestTags.SEARCH_BAR_CLEAR_BUTTON)) {
                Icon(
                    imageVector = Icons.Outlined.Clear,
                    contentDescription = stringResource(R.string.search_bar_clear),
                    tint = Color.Black.copy(alpha = 0.6f))
              }
        }
      }
}

/**
 * Dropdown menu displaying location suggestions.
 *
 * Shows a list of [Location] items returned from the search. Each item can be selected to navigate
 * the map to that location.
 *
 * @param expanded Whether the dropdown is currently visible.
 * @param suggestions List of location suggestions to display.
 * @param onDismiss Callback invoked when the dropdown should be dismissed.
 * @param onSelect Callback invoked when a location is selected.
 * @param modifier Modifier to be applied to the dropdown menu.
 */
@Composable
private fun SuggestionsDropdown(
    expanded: Boolean,
    suggestions: List<Location>,
    onDismiss: () -> Unit,
    onSelect: (Location) -> Unit,
    modifier: Modifier = Modifier
) {
  if (!expanded || suggestions.isEmpty()) return

  DropdownMenu(
      expanded = true,
      onDismissRequest = onDismiss,
      modifier = modifier.testTag(MapScreenTestTags.SEARCH_BAR_DROPDOWN),
      properties =
          PopupProperties(
              focusable = false, dismissOnClickOutside = true, dismissOnBackPress = true)) {
        suggestions.forEachIndexed { index, item ->
          val name = item.name
          if (name != null) {
            DropdownMenuItem(
                modifier = Modifier.testTag(MapScreenTestTags.SEARCH_BAR_DROPDOWN_ITEM),
                text = { Text(name, maxLines = 2) },
                onClick = { onSelect(item) })
            if (index < suggestions.lastIndex) {
              HorizontalDivider(thickness = 1.dp, color = horizontalDividerColor)
            }
          }
        }
      }
}
