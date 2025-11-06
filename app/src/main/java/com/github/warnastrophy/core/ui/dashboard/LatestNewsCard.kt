package com.github.warnastrophy.core.ui.dashboard

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.github.warnastrophy.R
import com.github.warnastrophy.core.model.HazardsDataService
import com.github.warnastrophy.core.util.formatDate
import kotlin.rem

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

object LatestNewsCardColors {
  val BORDER_COLOR: Color = Color(0xFFBDBDBD) // Shadow Grey
  val HEADER_BACKGROUND_COLOR: Color = Color(0xFFFFEBEE) // Light Red
  val HEADER_TEXT_COLOR: Color = Color(0xFFD32F2F) // Dark Red
  val BODY_BACKGROUND_COLOR: Color = Color(0xFFF6F4F4) // Off White
  val WEATHER_TEXT_COLOR: Color = Color(0xFF616161) // Dark Grey

  val IMAGE_TEXT_COLOR: Color = Color(0xFF9E9E9E) // Grey
  val READ_ARTICLE_TEXT_COLOR: Color = Color(0xFF8A2301) // Orange
}

@Composable
fun LatestNewsCard(hazardsService: HazardsDataService) {
  val fetcherState = hazardsService.fetcherState.collectAsState()
  val hazards = fetcherState.value.hazards
  var currentIndex by remember { mutableStateOf(0) }
  val context = LocalContext.current

  if (hazards.isEmpty()) return

  val currentHazard = hazards[currentIndex]

  Column(
      modifier =
          Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .border(
                  width = 1.dp,
                  color = LatestNewsCardColors.BORDER_COLOR.copy(alpha = 0.4f),
                  shape = RoundedCornerShape(12.dp))) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .testTag(LatestNewsTestTags.HEADER_ROW)
                    .background(LatestNewsCardColors.HEADER_BACKGROUND_COLOR)
                    .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
              Text(
                  text = "Latest news",
                  modifier = Modifier.testTag(LatestNewsTestTags.HEADER_TITLE),
                  color = LatestNewsCardColors.HEADER_TEXT_COLOR,
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp)

              Text(
                  text = formatDate(currentHazard.date ?: ""),
                  modifier = Modifier.testTag(LatestNewsTestTags.HEADER_TIMESTAMP),
                  color = LatestNewsCardColors.WEATHER_TEXT_COLOR,
                  fontSize = 12.sp)
            }

        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .background(LatestNewsCardColors.BODY_BACKGROUND_COLOR)
                    .padding(3.dp)) {
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                  verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                          currentIndex = (currentIndex - 1 + hazards.size) % hazards.size
                        },
                        modifier =
                            Modifier.fillMaxHeight()
                                .width(10.dp)
                                .testTag(LatestNewsTestTags.LEFT_BUTTON)
                                .clip(RoundedCornerShape(12.dp)),
                        contentPadding = PaddingValues(0.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = LatestNewsCardColors.BODY_BACKGROUND_COLOR)) {
                          Text("<", fontSize = 14.sp, color = Color.Black)
                        }

                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                          text = currentHazard.description ?: "",
                          color = Color.Black,
                          fontWeight = FontWeight.SemiBold,
                          fontSize = 16.sp,
                          maxLines = 1,
                          modifier = Modifier.testTag(LatestNewsTestTags.HEADLINE),
                          overflow = TextOverflow.Ellipsis)

                      Spacer(modifier = Modifier.height(4.dp))

                      Text(
                          text = currentHazard.severityText ?: "",
                          color = Color.DarkGray,
                          fontSize = 13.sp,
                          lineHeight = 16.sp,
                          modifier = Modifier.testTag(LatestNewsTestTags.BODY),
                          maxLines = 2,
                          overflow = TextOverflow.Ellipsis)

                      Spacer(modifier = Modifier.height(8.dp))

                      Text(
                          text = "read",
                          color = LatestNewsCardColors.READ_ARTICLE_TEXT_COLOR,
                          fontSize = 16.sp,
                          textDecoration = TextDecoration.Underline,
                          modifier =
                              Modifier.clickable {
                                    val intent =
                                        Intent(
                                            Intent.ACTION_VIEW, Uri.parse(currentHazard.reportUrl))
                                    ContextCompat.startActivity(context, intent, null)
                                  }
                                  .testTag(LatestNewsTestTags.LINK))
                    }

                    Box(
                        modifier =
                            Modifier.size(80.dp)
                                .border(
                                    width = 1.dp,
                                    color = LatestNewsCardColors.BORDER_COLOR,
                                    shape = RoundedCornerShape(8.dp))
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .testTag(LatestNewsTestTags.IMAGE_BOX),
                        contentAlignment = Alignment.Center) {
                          val imageRes = getImageForEvent(currentHazard.type ?: "default")
                          androidx.compose.foundation.Image(
                              painter = androidx.compose.ui.res.painterResource(id = imageRes),
                              contentDescription = "Event Image",
                              contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                              modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)))
                        }

                    Button(
                        onClick = {
                          currentIndex = (currentIndex + 1 + hazards.size) % hazards.size
                        },
                        modifier =
                            Modifier.fillMaxHeight()
                                .width(10.dp)
                                .testTag(LatestNewsTestTags.RIGHT_BUTTON)
                                .clip(RoundedCornerShape(12.dp)),
                        contentPadding = PaddingValues(0.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = LatestNewsCardColors.BODY_BACKGROUND_COLOR)) {
                          Text(">", fontSize = 14.sp, color = Color.Black)
                        }
                  }
              Spacer(modifier = Modifier.height(12.dp))
            }
      }
}

fun getImageForEvent(eventType: String): Int {
  return when (eventType) {
    "EQ" -> R.drawable.eq
    "TC" -> R.drawable.tc
    "FL" -> R.drawable.fl
    "VO" -> R.drawable.vo
    "DR" -> R.drawable.dr
    "WF" -> R.drawable.wf
    else -> R.drawable.eq // Une image par défaut
  }
}
