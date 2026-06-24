package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_stats")
data class PlayerStats(
    @PrimaryKey val id: Int = 1,
    val highScore: Int = 0,
    val diamonds: Int = 0,
    val speedBoostLevel: Int = 1,
    val magnetLevel: Int = 1,
    val shieldLevel: Int = 1
) {
    fun getSpeedBoostDurationMs(): Long = 3000L + speedBoostLevel * 1000L
    fun getMagnetDurationMs(): Long = 4000L + magnetLevel * 1000L
    fun getShieldDurationMs(): Long = 5000L + shieldLevel * 1000L

    fun getUpgradeCost(currentLevel: Int): Int {
        if (currentLevel >= 5) return -1 // Max level reached
        return currentLevel * 100 // Level 1->2 is 100, 2->3 is 200, etc.
    }
}
