# Warnastrophy – Emergency and Safety Assistant (Android)

Warnastrophy is an Android application designed to assist users in dangerous or emergency
situations. The application prioritizes hands-free interaction so that users can confirm or cancel emergency actions when it may not be safe or possible to interact with the screen. It combines voice-based confirmation, location awareness, and secure data management to support emergency workflows while protecting sensitive personal information.

The project is built using Jetpack Compose and follows a modular service and repository
architecture. It supports both local and cloud-backed storage depending on the user’s 
authentication state and is designed to be testable through clear interfaces and mockable
components.

## Project purpose

In emergency situations, users may be stressed, injured, moving, or unable to focus on 
their device. Warnastrophy aims to reduce the number of manual interactions required to
take action. The app provides a structured emergency flow that can escalate to actions 
such as phone calls or alerts, while still giving the user a clear opportunity to confirm
or cancel using voice commands.

## Main features

### Authentication and onboarding

The application supports user authentication using Firebase Authentication. It integrates
Android Credential Manager to simplify sign-in flows on supported devices. Warnastrophy
also includes an onboarding process, and the app determines which screen to display at 
launch based on the user’s authentication state and whether onboarding has been completed.

### Hybrid and secure storage

Warnastrophy supports both local and cloud-based data storage. When a user is not authenticated,
repositories operate entirely in local mode. When the user is authenticated, repositories switch
to a hybrid mode using Firebase Firestore for cloud synchronization while maintaining local access
when appropriate. This design ensures both availability and data security.

### Danger mode and confirmation flow

The app includes a danger mode orchestrated through shared services. When a confirmation is 
required, a dedicated communication screen is displayed as an overlay. The application uses
text-to-speech to ask the user to confirm an action. Once speaking finishes, the app listens
for a voice response. The user can respond with short confirmations such as “yes” or “no”. 
The app then verbally confirms whether the alert was sent or canceled.

### Voice communication

Voice interaction is implemented through two main services:
- Speech-to-text using Android SpeechRecognizer for recognition and confirmation parsing.
- Text-to-speech using Android TextToSpeech with an utterance progress listener to track 
- speaking state.

Both services expose their state through StateFlow, allowing the UI to react to listening
and speaking status, recognized text, errors, and audio levels. These services are defined
through interfaces, which enables deterministic and reliable testing using mock implementations.

### Emergency actions (call and SMS)

Emergency actions such as phone calls are implemented as explicit services. 
Calling is permission-aware and uses the ACTION_CALL intent. SMS support can
be integrated through a similar abstraction. These services are designed to be
replaceable and testable, ensuring that emergency behavior can be validated 
without triggering real actions during tests.

### Location and mapping

The application includes map screens and a map preview used on the dashboard. 
Location access and GPS behavior are wrapped in service abstractions that can 
be mocked during testing. Reverse geocoding is provided through a Nominatim service 
and repository.

### Contacts and health information

Warnastrophy allows users to manage emergency contacts and store health card 
information. These features are fully integrated into the application flow and work
with both local and hybrid storage modes.

### End-to-end testing support

The project includes a complete end-to-end testing setup using Jetpack Compose UI 
tests. The test harness can render the full application composable and optionally 
replace the map with a fake component to improve stability. Voice confirmation flows
can be tested using mock speech-to-text and text-to-speech services.

## Architecture overview

The application follows a modular and test-friendly architecture:
- The UI is built with Jetpack Compose and Navigation Compose.
- State is managed using ViewModels and Kotlin StateFlow.
- Core behavior is implemented through service abstractions, including speech, 
- text-to-speech, GPS, sensors, and danger mode orchestration.
- Data access is handled via repositories with local and hybrid cloud implementations.
- Global initialization and shared services are coordinated through a central StateManagerService.

This structure keeps UI code reactive and lightweight, isolates Android framework 
dependencies inside services, and ensures that core logic remains easy to test.

## Technologies and tools

- Kotlin
- Jetpack Compose (Material 3)
- Navigation Compose
- Android ViewModel
- Kotlin Coroutines and StateFlow
- Android DataStore (Preferences)
- Firebase Authentication
- Firebase Firestore
- Android Credential Manager
- Android SpeechRecognizer (speech-to-text)
- Android TextToSpeech (text-to-speech)
- SensorManager (accelerometer and gyroscope)
- JUnit
- Jetpack Compose UI testing

## Running the app

### Prerequisites

- Android Studio (recent stable version recommended)
- Android SDK 33 or higher (or the version specified by the project)
- An emulator or physical device with microphone support
- Google speech recognition services available on the device
- A Firebase project configured for the application

### Installation and setup

1. Clone the repository:
   ```bash
   git clone https://github.com/SWENTapp/warnastrophy.git
   ```
2. Open the project in Android Studio.
3. Add your Firebase configuration file:
4. Place google-services.json in the app/ directory.
5. Generate the `debug.keystore` (this is needed for the authentication to work) with the following command: 
```bash
keytool -genkey -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "C=US, O=Android, CN=Android Debug"
```
6. Place the generated `debug.keystore` in the app/directory. 
   See [Securing a keystore](https://github.com/SWENTapp/warnastrophy/wiki/Setting-Up-and-Securing-a-Keystore-for-CI-CD) to add its fingerprints to Firebase.
7. In the project root, create a `keystore.properties` file (See [Securing a keystore](https://github.com/SWENTapp/warnastrophy/wiki/Setting-Up-and-Securing-a-Keystore-for-CI-CD))
8. Sync Gradle and build the project.
9. Run the application on an emulator or a physical device.

## Using the main features

### First-time usage

1. Launch the application.
2. Sign in or create an account.
3. Complete the onboarding process.
4. Add emergency contacts.
5. Optionally fill in health card information.

## Voice confirmation flow

### When the app requires a voice confirmation:

- The communication screen is displayed.
- The app speaks a confirmation request.
- After speaking ends, the app starts listening.
- The user responds with “yes” or “no”.
- The app verbally confirms the result and proceeds accordingly.

## Building and testing
### Run unit tests
./gradlew test

### Run instrumented and end-to-end tests
./gradlew connectedAndroidTest

End-to-end tests use an isolated DataStore and can replace real components, 
such as the map, with fake implementations to reduce flakiness.

## Design and documentation links

### Figma UI mockups and design system:
https://www.figma.com/team_invite/redeem/NSv7YeZKUNalLSsFQN0U6O

### Figma architecture diagrams:
https://www.figma.com/team_invite/redeem/NSv7YeZKUNalLSsFQN0U6O

### GitHub Wiki (technical documentation):
https://github.com/SWENTapp/warnastrophy/wiki

The GitHub Wiki contains deeper technical documentation, including architecture 
explanations, service contracts, and testing strategies.

## Contributing

Contributions are welcome. New features should follow the existing architectural 
patterns. Services should be defined through interfaces to remain mockable, and tests
should be added or updated when modifying core logic or user flows.

