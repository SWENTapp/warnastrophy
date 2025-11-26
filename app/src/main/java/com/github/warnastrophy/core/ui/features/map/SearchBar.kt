package com.github.warnastrophy.core.ui.features.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.github.warnastrophy.R
import com.github.warnastrophy.core.model.Location
import com.github.warnastrophy.core.model.Location.Companion.toLatLng
import com.github.warnastrophy.core.ui.features.map.Colors.horizontalDividerColor
import com.google.maps.android.compose.CameraPositionState
import kotlinx.coroutines.launch

object Colors {
  val horizontalDividerColor: Color = Color(0xFF3B3B3B)
}

@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    viewModel: MapViewModel,
    cameraPositionState: CameraPositionState,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
  val coroutineScope = rememberCoroutineScope()
  val focusRequester = remember { FocusRequester() }

  var text by remember { mutableStateOf("") }
  var expanded by remember { mutableStateOf(false) }
  var isTextFocused by remember { mutableStateOf(false) }

  val uiState by viewModel.uiState.collectAsState()
  val suggestions = uiState.nominatimState

  LaunchedEffect(suggestions, text, isTextFocused) {
    expanded = isTextFocused && text.isNotEmpty() && suggestions.isNotEmpty()
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
          onFocusChanged = { focused ->
            isTextFocused = focused
            if (!focused) expanded = false
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

@Composable
private fun SearchTextField(
    text: String,
    onTextChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
  Row(
      modifier =
          modifier
              .background(Color.White, RoundedCornerShape(16.dp))
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
                Modifier.fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { state -> onFocusChanged(state.isFocused) }
                    .testTag(MapScreenTestTags.SEARCH_BAR_TEXT_FIELD),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(color = Color.Black),
            decorationBox = { innerTextField ->
              if (text.isEmpty()) {
                Text(
                    stringResource(R.string.search_bar_search),
                    color = Color.Black.copy(alpha = 0.6f))
              }
              innerTextField()
            })
      }
}

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
            if (index < suggestions.size - 1) {
              HorizontalDivider(thickness = 1.dp, color = horizontalDividerColor)
            }
          }
        }
      }
}
