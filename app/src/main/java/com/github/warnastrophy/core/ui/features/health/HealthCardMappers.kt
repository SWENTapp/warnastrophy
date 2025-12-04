package com.github.warnastrophy.core.ui.features.health

import com.github.warnastrophy.core.model.HealthCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

private val UI_DF =
    DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)

private fun String.uiToIsoOrBlank(): String =
    if (isBlank()) "" else LocalDate.parse(this, UI_DF).toString()

private fun String.isoToUiOrBlank(): String =
    if (isBlank()) "" else LocalDate.parse(this).format(UI_DF)

/** UI → Domain */
fun HealthCardFormState.toDomain(): HealthCard =
    HealthCard(
        fullName = fullName.trim(),
        dateOfBirthIso = birthDate.uiToIsoOrBlank(),
        idNumber = socialSecurityNumber.trim(),
        sex = sex.ifBlank { null },
        bloodType = bloodType.ifBlank { null },
        heightCm = heightCm.toIntOrNull(),
        weightKg = weightKg.toDoubleOrNull(),
        chronicConditions = chronicConditions.splitToList(),
        allergies = allergies.splitToList(),
        medications = medications.splitToList(),
        onGoingTreatments = onGoingTreatments.splitToList(),
        medicalHistory = medicalHistory.splitToList(),
        organDonor = organDonor,
        notes = notes.ifBlank { null })

/** Domain → UI */
fun HealthCard.toFormState(): HealthCardFormState =
    HealthCardFormState(
        fullName = fullName,
        birthDate = dateOfBirthIso.isoToUiOrBlank(),
        socialSecurityNumber = idNumber,
        sex = sex.orEmpty(),
        bloodType = bloodType.orEmpty(),
        heightCm = heightCm?.toString().orEmpty(),
        weightKg = weightKg?.toString().orEmpty(),
        chronicConditions = chronicConditions.joinToString(", "),
        allergies = allergies.joinToString(", "),
        medications = medications.joinToString(", "),
        onGoingTreatments = onGoingTreatments.joinToString(", "),
        medicalHistory = medicalHistory.joinToString(", "),
        organDonor = organDonor ?: false,
        notes = notes.orEmpty())

/** This method converts a HealthCard to a HealthCardPreviewState */
fun HealthCard.toPreviewState(): HealthCardPreviewState =
    HealthCardPreviewState(
        fullName = fullName,
        birthDate = dateOfBirthIso.isoToUiOrBlank(),
        sex = sex.orEmpty().ifBlank { "-" },
        bloodType = bloodType.orEmpty().ifBlank { "-" },
        allergies = if (allergies.isEmpty()) "-" else allergies.joinToString(", "),
        medications = if (medications.isEmpty()) "-" else medications.joinToString(", "),
        organDonor = organDonor ?: false,
        notes = notes.orEmpty().ifBlank { "-" })

internal fun String.splitToList(): List<String> =
    split(",").map { it.trim() }.filter { it.isNotEmpty() }
