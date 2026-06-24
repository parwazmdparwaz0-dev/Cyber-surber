package com.example.ui

enum class ObstacleType {
    BARRICADE,     // Low wall: jump over or lane swap
    LOW_LASER,     // Laser wire: jump over
    HIGH_SCANNER,  // Electric scanning field: slide under
    CYBER_DRONE    // Large machine: lane swap only
}

data class Obstacle(
    val id: Long,
    val type: ObstacleType,
    val lane: Int, // -1, 0, 1
    var z: Float,  // 1.0 (horizon) down to 0.0 (player)
    val width: Float = 0.6f
)

data class GoldNode(
    val id: Long,
    val lane: Int, // -1, 0, 1
    var z: Float,  // 1.0 down to 0.0
    var isCollected: Boolean = false
)

enum class PowerUpItemType {
    SPEED_BOOST,
    MAGNET,
    SHIELD
}

data class PowerUpItem(
    val id: Long,
    val type: PowerUpItemType,
    val lane: Int, // -1, 0, 1
    var z: Float,  // 1.0 down to 0.0
    var isCollected: Boolean = false
)

data class DiamondParticle(
    val id: Long,
    val startX: Float,
    val startY: Float,
    var progress: Float = 0.0f, // 0.0 -> 1.0
    val size: Float,
    val speed: Float = 3.0f // Speed of translation
)

enum class ScreenState {
    MAIN_MENU,
    PLAYING,
    PAUSED,
    GAME_OVER,
    UPGRADES
}
