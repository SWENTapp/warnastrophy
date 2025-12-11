package com.github.warnastrophy.core.ui.features.dashboard

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.warnastrophy.R
import com.github.warnastrophy.core.data.service.FetcherState
import com.github.warnastrophy.core.data.service.HazardsDataService
import com.github.warnastrophy.core.model.Hazard
import com.github.warnastrophy.core.ui.theme.ExtendedColorScheme
import com.github.warnastrophy.core.ui.theme.extendedColors
import com.github.warnastrophy.core.util.formatDate
import com.github.warnastrophy.core.util.openWebPage

object LatestNewsTestTags {
  const val HEADER_ROW = "latestNewsHeader"
  const val HEADER_TITLE = "latestNewsHeaderTitle"
  const val HEADER_TIMESTAMP = "latestNewsHeaderTimestamp"
  const val HEADLINE = "latestNewsHeadline"
  const val BODY = "latestNewsBody"
  const val IMAGE_BOX = "latestNewsImage"

  const val RIGHT_BUTTON = "latestNewsCardRightButton"

  const val LEFT_BUTTON = "latestNewsCardLeftButton"

  const val LINK = "latestNewsLink"
}

/**
 * Displays a card with the latest news related to hazards.
 *
 * @param hazardsService An instance of `HazardsDataService` used to retrieve hazard data and manage
 *   their state.
 *
 * Features:
 * - Displays information about the current hazard, including its description, severity, and date.
 * - Allows navigation between hazards using left and right buttons.
 * - Includes a clickable link to read more information about the hazard.
 * - Displays an image associated with the type of hazard.
 *
 * @see HazardsDataService
 */
@Composable
fun LatestNewsCard(
    hazardsService: HazardsDataService,
    modifier: Modifier = Modifier,
    openWebPage: (context: Context, url: String?) -> Unit = ::openWebPage // For testing
) {
  val fetcherState by hazardsService.fetcherState.collectAsState()
  var currentIndex by remember { mutableIntStateOf(0) }
  currentIndex = currentIndex.coerceIn(0, (fetcherState.hazards.size - 1).coerceAtLeast(0))

  val currentHazard = fetcherState.hazards.getOrNull(currentIndex) ?: Hazard()
  val extendedColors = MaterialTheme.extendedColors

  Column(
      modifier =
          modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .border(
                  width = 1.dp,
                  color = extendedColors.newsCard.border.copy(alpha = 0.4f),
                  shape = RoundedCornerShape(12.dp))) {
        CardHeader(currentHazard.date, extendedColors)
        CardBody(
            fetcherState = fetcherState,
            currentHazard = currentHazard,
            currentIndex = currentIndex,
            onIndexChange = { newIndex -> currentIndex = newIndex },
            extendedColors = extendedColors,
            openWebPage = openWebPage)
      }
}

@Composable
private fun CardHeader(date: String?, extendedColors: ExtendedColorScheme) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .testTag(LatestNewsTestTags.HEADER_ROW)
              .background(extendedColors.newsCard.headerBackground)
              .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 5.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(id = R.string.latest_news),
            modifier = Modifier.testTag(LatestNewsTestTags.HEADER_TITLE),
            color = extendedColors.newsCard.headerText,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp)

        Text(
            text = formatDate(date ?: ""),
            modifier = Modifier.testTag(LatestNewsTestTags.HEADER_TIMESTAMP),
            color = extendedColors.newsCard.weatherText,
            fontSize = 12.sp)
      }
}

@Composable
private fun CardBody(
    fetcherState: FetcherState,
    currentHazard: Hazard,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
    extendedColors: ExtendedColorScheme,
    openWebPage: (context: Context, url: String?) -> Unit
) {
  Column(
      modifier =
          Modifier.fillMaxWidth()
              .background(extendedColors.newsCard.bodyBackground.copy(alpha = 0.8f))
              .padding(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
              val hasHazards = fetcherState.hazards.isNotEmpty()
              val hazardCount = fetcherState.hazards.size

              if (hasHazards) {
                NavigationButton(
                    testTag = LatestNewsTestTags.LEFT_BUTTON,
                    content = "<",
                    extendedColors = extendedColors,
                    onClick = { onIndexChange((currentIndex - 1 + hazardCount) % hazardCount) })
              }

              HazardInfo(
                  fetcherState = fetcherState,
                  currentHazard = currentHazard,
                  extendedColors = extendedColors,
                  openWebPage = openWebPage)

              HazardImage(currentHazard.type)

              if (hasHazards) {
                NavigationButton(
                    testTag = LatestNewsTestTags.RIGHT_BUTTON,
                    content = ">",
                    extendedColors = extendedColors,
                    onClick = { onIndexChange((currentIndex + 1) % hazardCount) })
              } else {
                Spacer(modifier = Modifier.width(2.dp))
              }
            }
        Spacer(modifier = Modifier.height(12.dp))
      }
}

@Composable
private fun RowScope.HazardInfo(
    fetcherState: FetcherState,
    currentHazard: Hazard,
    extendedColors: ExtendedColorScheme,
    openWebPage: (context: Context, url: String?) -> Unit
) {
  val context = LocalContext.current
  Column(modifier = Modifier.weight(1f)) {
    Text(
        text =
            when {
              currentHazard.description != null -> currentHazard.description
              fetcherState.isLoading -> stringResource(id = R.string.loading)
              else -> stringResource(id = R.string.no_news_yet)
            },
        color = Color.Black,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        maxLines = 1,
        modifier =
            Modifier.testTag(LatestNewsTestTags.HEADLINE).align(Alignment.CenterHorizontally),
        overflow = TextOverflow.Ellipsis)

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = currentHazard.severityText ?: "",
        color = Color.DarkGray,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        modifier = Modifier.testTag(LatestNewsTestTags.BODY).height(32.dp),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis)

    Spacer(modifier = Modifier.height(8.dp))

    if (fetcherState.hazards.isNotEmpty() && currentHazard.articleUrl != null) {
      Text(
          text = "read",
          color = extendedColors.newsCard.readArticleText,
          fontSize = 16.sp,
          textDecoration = TextDecoration.Underline,
          modifier =
              Modifier.clickable { openWebPage(context, currentHazard.articleUrl) }
                  .testTag(LatestNewsTestTags.LINK))
    }
  }
}

@Composable
private fun NavigationButton(
    testTag: String,
    content: String,
    extendedColors: ExtendedColorScheme,
    onClick: () -> Unit
) {
  Button(
      onClick = onClick,
      modifier =
          Modifier.fillMaxHeight().width(10.dp).testTag(testTag).clip(RoundedCornerShape(12.dp)),
      contentPadding = PaddingValues(0.dp),
      colors =
          ButtonDefaults.buttonColors(
              containerColor = extendedColors.newsCard.bodyBackground.copy(alpha = 0.8f))) {
        Text(content, fontSize = 14.sp, color = Color.Black)
      }
}

@Composable
private fun HazardImage(eventType: String?) {
  Box(
      modifier =
          Modifier.size(80.dp)
              .border(
                  width = 1.dp,
                  color = MaterialTheme.extendedColors.newsCard.border,
                  shape = RoundedCornerShape(8.dp))
              .background(Color.White, RoundedCornerShape(8.dp))
              .testTag(LatestNewsTestTags.IMAGE_BOX),
      contentAlignment = Alignment.Center) {
        val imageRes = getImageForEvent(eventType ?: "default")
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = "Event Image",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)))
      }
}

/**
 * Returns the image resource identifier corresponding to the event type.
 *
 * @param eventType The event type as a string (for example, "EQ", "TC", etc.).
 * @return The resource identifier of the image associated with that event type. If the type is not
 *   recognized, a default image is returned.
 */
fun getImageForEvent(eventType: String): Int {
  return when (eventType) {
    "EQ" -> R.drawable.eq
    "TC" -> R.drawable.tc
    "FL" -> R.drawable.fl
    "VO" -> R.drawable.vo
    "DR" -> R.drawable.dr
    "WF" -> R.drawable.wf
    else -> R.drawable.de
  }
}
