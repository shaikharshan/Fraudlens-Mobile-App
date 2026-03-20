package com.fraudlens.sdk.risk

/** Kilometers — same default as the FraudLens demo app. */
const val DEFAULT_LOCATION_DEVIATION_KM: Double = 100.0

const val DEFAULT_HIGH_RISK_COUNTDOWN_SEC: Int = 15

fun isLocationDeviationRisky(deviationKm: Double): Boolean =
    deviationKm >= DEFAULT_LOCATION_DEVIATION_KM
