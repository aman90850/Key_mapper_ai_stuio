package com.example.model

enum class PlayerSituation(
    val id: String,
    val title: String,
    val chipLabel: String,
    val badgeIconName: String,
    val description: String
) {
    FOOT(
        id = "FOOT",
        title = "Infantry / On-Foot Combat",
        chipLabel = "🚶 FOOT",
        badgeIconName = "DirectionsRun",
        description = "Movement joystick, Camera look, Guns, Scope, Jump, Crouch, Prone"
    ),
    VEHICLE_CAR(
        id = "CAR",
        title = "Car / Buggy Driving",
        chipLabel = "🚗 CAR / BUGGY",
        badgeIconName = "DirectionsCar",
        description = "Gas, Reverse, Steer Left/Right, Nitro Boost, Handbrake, Horn, Exit"
    ),
    VEHICLE_BIKE(
        id = "BIKE",
        title = "Motorcycle Driving",
        chipLabel = "🏍️ BIKE",
        badgeIconName = "TwoWheeler",
        description = "Gas, Reverse, Steer, Bike Lean Down, Wheelie Up, Handbrake, Exit"
    );

    fun next(): PlayerSituation = when (this) {
        FOOT -> VEHICLE_CAR
        VEHICLE_CAR -> VEHICLE_BIKE
        VEHICLE_BIKE -> FOOT
    }
}
