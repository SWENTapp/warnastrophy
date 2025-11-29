package com.github.warnastrophy.core.ui.onboard


sealed class OnboardingModel(
    val title: String,
    val description: String
) {
    data object FirstPage: OnboardingModel(
        title = "Automatic Disaster Alert",
        description = "We track your location in the background, if you enter a high-risk area, " +
                "and we detect inactivity (no movement), " +
                "we automatically trigger an alert"
    )

    data object SecondPage: OnboardingModel(
        title = "Manual High-Risk Safety",
        description = "Use this for activities like climbing or solo hiking. " +
                "If we detect a fall followed " +
                "by prolonged stillness, we send your location to emergency service."
    )

    data object ThirdPage: OnboardingModel(
        title = "Ready for Protection?",
        description = "To monitor your location in the background and send emergency texts," +
                " we need location and SMS permission. We use these data only for your safety."
    )
}