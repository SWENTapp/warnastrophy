package com.github.warnastrophy.core.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object LatestNewsTestTags {
  const val CARD = "latestNewsCard"
  const val HEADER_ROW = "latestNewsHeader"
  const val HEADER_TITLE = "latestNewsHeaderTitle"
  const val HEADER_TIMESTAMP = "latestNewsHeaderTimestamp"
  const val HEADLINE = "latestNewsHeadline"
  const val BODY = "latestNewsBody"
  const val IMAGE_BOX = "latestNewsImage"
}

object LatestNewsCardColors {
  val BORDER_COLOR: Color = Color(0xFFBDBDBD) // Shadow Grey
  val HEADER_BACKGROUND_COLOR: Color = Color(0xFFFFEBEE) // Light Red
  val HEADER_TEXT_COLOR: Color = Color(0xFFD32F2F) // Dark Red
  val BODY_BACKGROUND_COLOR: Color = Color(0xFFF6F4F4) // Off White
  val WEATHER_TEXT_COLOR: Color = Color(0xFF616161) // Dark Grey
  val IMAGE_TEXT_COLOR: Color = Color(0xFF9E9E9E) // Grey
}

/*
This Composable displays a card showing the latest news.
It features a header with the title and timestamp, followed by a news headline,
a brief description, and a placeholder for an image.
 */
@Composable
fun LatestNewsCard(modifier: Modifier = Modifier) {
  Column(
      modifier =
          Modifier.fillMaxWidth()
              .testTag(LatestNewsTestTags.CARD)
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
                  text = "2 min ago • Weather",
                  modifier = Modifier.testTag(LatestNewsTestTags.HEADER_TIMESTAMP),
                  color = LatestNewsCardColors.WEATHER_TEXT_COLOR,
                  fontSize = 12.sp)
            }

        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .background(LatestNewsCardColors.BODY_BACKGROUND_COLOR)
                    .padding(16.dp)) {
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                          text = "Tropical cyclone just hit Jamaica",
                          color = Color.Black,
                          fontWeight = FontWeight.SemiBold,
                          fontSize = 16.sp,
                          maxLines = 2,
                          modifier = Modifier.testTag(LatestNewsTestTags.HEADLINE),
                          overflow = TextOverflow.Ellipsis)

                      Spacer(modifier = Modifier.height(4.dp))

                      Text(
                          text = "Authorities report strong winds and heavy rainfall.",
                          color = Color.DarkGray,
                          fontSize = 13.sp,
                          lineHeight = 16.sp,
                          modifier = Modifier.testTag(LatestNewsTestTags.BODY),
                          maxLines = 3,
                          overflow = TextOverflow.Ellipsis)
                    }

                    Box(
                        modifier =
                            Modifier.size(64.dp)
                                .border(
                                    width = 1.dp,
                                    color = LatestNewsCardColors.BORDER_COLOR,
                                    shape = RoundedCornerShape(8.dp))
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .testTag(LatestNewsTestTags.IMAGE_BOX),
                        contentAlignment = Alignment.Center) {
                          Text(
                              text = "Image",
                              color = LatestNewsCardColors.IMAGE_TEXT_COLOR,
                              fontSize = 12.sp)
                        }
                  }
            }
      }
}
