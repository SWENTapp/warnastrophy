package com.github.warnastrophy.core.ui.util

import com.github.warnastrophy.core.domain.model.Hazard

val hazards =
    listOf(
        Hazard(
            id = 1,
            description = "Tropical cyclone just hit Jamaica",
            severityText =
                "A tropical cyclone has made landfall in Jamaica causing significant damage.",
            articleUrl = "https://example.com/cyclone.jpg",
            date = "2024-06-15T10:00:00Z"),
        Hazard(
            id = 2,
            description = "Floods in Bangladesh",
            severityText = "Severe floods have affected thousands of people in Bangladesh.",
            articleUrl = "https://example.com/floods.jpg",
            date = "2024-06-15T10:00:00Z"),
        Hazard(
            id = 2,
            description = "Floods in Bangladesh",
            severityText = "Severe floods have affected thousands of people in Bangladesh.",
            articleUrl = "https://example.com/floods.jpg",
            date = "2024-06-15T10:00:00Z"))
