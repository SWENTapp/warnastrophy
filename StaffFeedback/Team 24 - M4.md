# SwEnt M3 Team Grading Report

This M3 milestone is the culmination of your SwEnt journey, and it gives us the final opportunity to give you, as a team, formal feedback on how you performed in the project. By now, you should be capable of demonstrating a solid command of the Scrum methodology and collaborative teamwork, and be able to deliver a high-quality application that is ready for real users.
This feedback report is meant to complement the informal, ungraded feedback that you received from your coaches during the weekly meetings, over email, on Discord, etc.

You can find the evaluation criteria in the [M3 Deliverables](https://github.com/swent-epfl/public/blob/main/project/M3.md) document.
As mentioned before, the standards for M2 were elevated relative to M1, and this progression continued into M3: we now hold you to the highest professional standard in SwEnt.

---

## Blue Belt

You qualified for a Blue Belt 🥋🔵 and got a final team grade of 5.11/6 for M3. Excellent work! You're demonstrating advanced skills.

We looked at several aspects, grouped into seven categories for the team grade. Here is the breakdown of the points you earned:

| Metric                                   | **Points Earned**                | **Weight** | **Feedback**                               |
|------------------------------------------|----------------------------------|------------|--------------------------------------------|
| **Completeness**                         | 4.95 out of 6 | 20%        | [See Details](#completeness)               |
| **Functionality**                        | 4.97 out of 6 | 20%       | [See Details](#functionality)              |
| **User Experience**                      | 3.8 out of 6 | 5%       | [See Details](#user-experience)            |
| **Design and Maintainability**           | 5.6 out of 6 | 20% | [See Details](#design-and-maintainability) |
| **QA (Testing)**                         | 5.16 out of 6           | 20%        | [See Details](#qa-testing)                 |
| **Documentation**                        | 5.25 out of 6 | 10%       | [See Details](#documentation)              |
| **Autonomy**                             | 5.1 out of 6     | 5%         | [See Details](#autonomy)                   |
| **Final Team Grade**                     | **5.11 out of 6** |            |                                            |

In addition to the feedback you received from the coaches during the Sprints, you will find more detailed comments below.

---

## Completeness

We first evaluated the depth and complexity of the main epics in your app, along with their contribution to the app, the tangible value they provide to the user, and their alignment with the app’s goals.

We evaluated the extent to which your app meets the course requirements articulated at the start of the semester, and whether they are implemented effectively, they integrate seamlessly, and are indeed essential to the app.

We also looked at the robustness and completeness of the different features you implemented: are all the features finished and polished, are they secure and bug-free, and are they thoughtfully designed.

- Globally, it took us some time to make the app work. When installing it, some of the settings were not getting applied properly.

- Your main EPICs are implemented in the app and are coherent. Some could have been developped a bit more. For example, the hazards be displayed in a separated list where you could sort or filter them. In term of prioritization, a bigger focus on the UI would have made your app more usable in the end.

- The features are not all fully completed. There are some visual errors on the hazards (magnitude for floods, url retreival errors, ..) and unconsistent settings in the profile. Also, we had no feedback in the app on whether the background tracking was actually enabled

For this part, you received 4.95/6.

---

## Functionality

In this context, we assessed your app's ability to handle unexpected inputs provided by clueless or malicious users (including spamming buttons, entering wrong inputs, stopping a process mid-way, etc.); we wanted to see that your app handles all edge cases gracefully, has comprehensive error handling, and includes robust mechanisms for maintaining stability under stress.

We then evaluated the performance and reliability of the final product, i.e., the APK: we wanted to see that your APK is stable and the UI responds quickly and has seamless navigation.

Next we looked into your implementation of user authentication and multi-user support: does the app correctly manage users, can users personalize their accounts, does the app support session persistence, are multi-user interactions well supported, can accounts be used on another device, and is account information preserved when switching devices.

- The theme button behaves inconsistently, sometimes only partially working and causing UI jitter.

- It's not clear whether the buttons to give or remove permission in the settings are actually working and their visual state is not persistent .

- You can start the danger mode without having really selected a "mode".

- When deleting a contact, it stays in the page and prompts a "datastore error"

- The speech-to-text feature is a good idea, but it only works with the words "yes" and "no", which is quite limiting in the context of an emergency.


- It's unclear whether the account is really useful. Yes it synchronizes your information across devices, but a community system or real online value could have been good in the app.

For this part, you received 4.97/6.

---

## User Experience

For this part, we wanted to see how intuitive and user-friendly the app is for real users. Beyond having good usability, did you pay attention to streamlining the interactions, is it easy to figure out, can new users start making good use of the app quickly, are the interaction flows well thought out and refined.

- There is a lot going on in your app. You added an onboarding screen, which is really good, but once logged in, the user experience was not as easy to follow. We can add custom activities with different threshold, but the user doesn't have any idea what those metrics actually are. This could have been solved with more explanations or predefined categories that are already tuned.

- The dashboard is quite dense, maybe creating different screens would have made the app feel more complete (one with news, one with hazards, one with medical info, ...)

- Most of your work lies in the backend, and this could have been showcased more in the app -> explicitly say that the data is encrypted for example.

- The danger mode had sometimes weird behaviours and there is no button to dismiss the speech-to-test screen, forcing you to say "no" until the page closes.

- You don't use the appropriate input fields in your forms. Restrict directly the user to the right kind of content to make his easier.

- Don't use technichal terms like CALL_PHONE permission in your visible UI components


For this part, you received 3.8/6.

---

## Design and Maintainability

We evaluated whether your code is of high quality and employs best practices.
We expect the codebase to be polished, well documented, follow consistent conventions, be modular, and allow for easy modifications.
You should be able to employ advanced techniques by now, such as asynchronous functions (flows, coroutines), good resource management, and automated dependency injection (e.g., with Hilt).

We also assessed your overall app architecture and design, looking in particular at aspects surrounding robustness and scalability.
We looked at both the codebase and the documentation of the app (Wiki and architecture diagram), we expect your design to demonstrate thoughtful consideration for performance, maintainability, and future growth.

- You still have some sonar cloud issues opened. 

- Your repository is well structured, congrats!


For this part, you received 5.6/6.

---

## QA (Testing)

The first aspect we looked at here was your test suite, in terms of both quality and the final line coverage.
We expect the testing to be rigorous and to cover all components and edge cases, and they should validate every significant user journey.
Your end-to-end tests should be detailed and include error-handling scenarios.
The tests should be well-documented and easy to maintain.
Finally, your test suite should demonstrate advanced techniques, mock data for performance testing, and automated regression tests.


- Generally your code is well tested and you have an impressive coverage for an app that is based mostly on sensors.

- For the end2end test, you don't have the 4 required ones and they should adress more complex scenario than happy paths.

For this part, you received 5.16/6.

---

## Documentation

We looked at your README and GitHub Wiki to evaluate the quality and completeness of your app’s documentation. We expect the README and Wiki to be thorough and achieve professional-level clarity and completeness.
They should provide detailed descriptions of the app's architecture, implementation of the features, the development setup and guidelines for contributing.

We also assessed your use of Figma and the architecture diagram for effective UI design, organization, and app structure planning.
By this stage, we expect your Figma and Architecture diagram to be complete and up-to-date. The Figma should be consistent with the UI and the Architecture diagram should be comprehensive.

- The architecture diagram lacks some clarity, for instance the profile viewmodel is not linked to any repository.

- Some composable are missing clear documentaion.

- Congrats on your work for the README and the wiki, it's truly great!

For this part, you received 5.25/6.

---

## Autonomy

A primary goal of SwEnt is to teach you how to function autonomously as a team.
For this part of the evaluation, we assessed you team’s independence, spanning Sprint 6 to Sprint 10, based on the meetings with coaches, Sprint planning, and how you managed risk.
By this stage, coaches should no longer be necessary for the team to operate, i.e., you can organize yourselves, you don't need to be reminded about tasks, and you can conduct the Scrum ceremonies on your own.

- Your meetings are improving each week. However, late arrivals sometimes reduce the professionalism of the sessions, and occasional guidance from the coach would help ensure smoother transitions between topics.

For this part, you received 5.1/6.

---

## Summary

Based on the above points, your grade for this M3 milestone is 5.11/6. If you are interested in how this fits into the bigger grading scheme, please see [project README](https://github.com/swent-epfl/public/blob/main/project/README.md) and the [course README](https://github.com/swent-epfl/public/blob/main/README.md).

The entire SwEnt staff wishes you the very best in your career, and we look forward to seeing you do great things with what you learned this semester.



