package com.github.warnastrophy.core.domain.model

/**
 * A data class representing a health card for an individual, containing personal and medical
 * information. This class can be used to store and manage a person's health-related data.
 *
 * @property fullName The full name of the individual.
 * @property birthDate The birth date of the individual, represented as a `String` (e.g.,
 *   "YYYY-MM-DD").
 * @property sex The sex of the individual (e.g., "Male", "Female", "Other"). This property is
 *   optional and nullable.
 * @property bloodType The blood type of the individual (e.g., "A+", "O-", etc.). This property is
 *   optional and nullable.
 * @property heightCm The height of the individual in centimeters. This property is optional and
 *   nullable.
 * @property weightKg The weight of the individual in kilograms. This property is optional and
 *   nullable.
 * @property chronicConditions A list of chronic conditions the individual may have (e.g.,
 *   "Diabetes", "Hypertension"). This property is optional and defaults to an empty list.
 * @property allergies A list of allergies the individual has (e.g., "Peanuts", "Dust"). This
 *   property is optional and defaults to an empty list.
 * @property medications A list of medications the individual is currently taking (e.g., "Aspirin",
 *   "Insulin"). This property is optional and defaults to an empty list.
 * @property organDonor A flag indicating whether the individual has consented to organ donation.
 *   Defaults to `false`.
 * @property notes Additional notes or comments about the individual’s health. This property is
 *   optional and nullable.
 */
data class HealthCard(
    val fullName: String,
    val birthDate: String,
    val socialSecurityNumber: String,
    val sex: String? = null,
    val bloodType: String? = null,
    val heightCm: Int? = null,
    val weightKg: Double? = null,
    val chronicConditions: List<String> = emptyList(),
    val allergies: List<String> = emptyList(),
    val medications: List<String> = emptyList(),
    val onGoingTreatments: List<String> = emptyList(),
    val medicalHistory: List<String> = emptyList(),
    val organDonor: Boolean = false,
    val notes: String? = null
)
