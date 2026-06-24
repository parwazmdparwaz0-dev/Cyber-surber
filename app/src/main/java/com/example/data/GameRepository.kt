package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class GameRepository(private val playerStatsDao: PlayerStatsDao) {

    val playerStats: Flow<PlayerStats> = playerStatsDao.getPlayerStatsFlow()
        .map { it ?: PlayerStats() }
        .flowOn(Dispatchers.IO)

    suspend fun getStats(): PlayerStats = withContext(Dispatchers.IO) {
        playerStatsDao.getPlayerStatsDirect() ?: PlayerStats().also {
            playerStatsDao.insertOrUpdate(it)
        }
    }

    suspend fun saveStats(stats: PlayerStats) = withContext(Dispatchers.IO) {
        playerStatsDao.insertOrUpdate(stats)
    }

    suspend fun addDiamonds(count: Int) = withContext(Dispatchers.IO) {
        val current = getStats()
        saveStats(current.copy(diamonds = current.diamonds + count))
    }

    suspend fun updateHighScore(newScore: Int): Boolean = withContext(Dispatchers.IO) {
        val current = getStats()
        if (newScore > current.highScore) {
            saveStats(current.copy(highScore = newScore))
            true
        } else {
            false
        }
    }

    suspend fun upgradePowerUp(type: PowerUpType): Boolean = withContext(Dispatchers.IO) {
        val current = getStats()
        when (type) {
            PowerUpType.SPEED_BOOST -> {
                val cost = current.getUpgradeCost(current.speedBoostLevel)
                if (cost in 0..current.diamonds) {
                    saveStats(current.copy(
                        diamonds = current.diamonds - cost,
                        speedBoostLevel = current.speedBoostLevel + 1
                    ))
                    true
                } else false
            }
            PowerUpType.MAGNET -> {
                val cost = current.getUpgradeCost(current.magnetLevel)
                if (cost in 0..current.diamonds) {
                    saveStats(current.copy(
                        diamonds = current.diamonds - cost,
                        magnetLevel = current.magnetLevel + 1
                    ))
                    true
                } else false
            }
            PowerUpType.SHIELD -> {
                val cost = current.getUpgradeCost(current.shieldLevel)
                if (cost in 0..current.diamonds) {
                    saveStats(current.copy(
                        diamonds = current.diamonds - cost,
                        shieldLevel = current.shieldLevel + 1
                    ))
                    true
                } else false
            }
        }
    }
}

enum class PowerUpType {
    SPEED_BOOST,
    MAGNET,
    SHIELD
}
