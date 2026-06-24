package com.example.ui

import android.app.Application
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.Build
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameDatabase
import com.example.data.GameRepository
import com.example.data.PlayerStats
import com.example.data.PowerUpType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val database = GameDatabase.getDatabase(application, viewModelScope)
    private val repository = GameRepository(database.playerStatsDao())

    // Expose DB-driven player stats
    val playerStats: StateFlow<PlayerStats> = repository.playerStats
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PlayerStats()
        )

    // Game states
    var screenState by mutableStateOf(ScreenState.MAIN_MENU)
        private set

    // Game Variables
    var score by mutableStateOf(0f)
        private set
    var diamondsThisRun by mutableStateOf(0)
        private set
    var isNewHighScore by mutableStateOf(false)
        private set

    // Speed Variables
    var baseSpeed by mutableStateOf(0.22f) // track units per second
        private set
    val currentSpeed: Float
        get() = if (isSpeedBoostActive) baseSpeed * 2.8f else baseSpeed

    // Player State
    var playerLane by mutableStateOf(0) // -1 (Left), 0 (Center), 1 (Right)
        private set
    var currentLaneX by mutableStateOf(0f) // For smooth transition between lanes
        private set
    var jumpProgress by mutableStateOf(0f) // 0.0 to 1.0 (height offset)
        private set
    var slideProgress by mutableStateOf(0f) // 0.0 or 1.0 (ducked state)
        private set
    var isJumping by mutableStateOf(false)
        private set
    var isSliding by mutableStateOf(false)
        private set

    private var jumpStartTime = 0L
    private var slideStartTime = 0L
    private val jumpDuration = 650L // ms
    private val slideDuration = 650L // ms

    // Active power-up durations remaining (ms)
    var speedBoostTimer by mutableStateOf(0L)
        private set
    var magnetTimer by mutableStateOf(0L)
        private set
    var shieldTimer by mutableStateOf(0L)
        private set
    var isShieldActive by mutableStateOf(false)
        private set

    val isSpeedBoostActive: Boolean get() = speedBoostTimer > 0L
    val isMagnetActive: Boolean get() = magnetTimer > 0L

    // Entities in world
    var obstacles = mutableStateOf<List<Obstacle>>(emptyList())
        private set
    var goldNodes = mutableStateOf<List<GoldNode>>(emptyList())
        private set
    var powerUps = mutableStateOf<List<PowerUpItem>>(emptyList())
        private set

    // Particle effect states
    var activeParticles = mutableStateOf<List<DiamondParticle>>(emptyList())
        private set

    // Track visual scroll
    var trackOffset by mutableStateOf(0f)
        private set

    // Timers for spawning
    private var obstacleSpawnTimer = 0f
    private var itemSpawnTimer = 0f
    private var lastFrameTime = 0L

    // For Vibrations
    private val vibrator = application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    fun triggerVibration(milliseconds: Long = 50) {
        try {
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(milliseconds)
                }
            }
        } catch (e: Exception) {
            // Ignored if vibrator permission or capability is missing
        }
    }

    fun setScreen(state: ScreenState) {
        screenState = state
        if (state == ScreenState.MAIN_MENU) {
            resetGame()
        }
    }

    private fun resetGame() {
        score = 0f
        diamondsThisRun = 0
        isNewHighScore = false
        baseSpeed = 0.22f
        playerLane = 0
        currentLaneX = 0f
        jumpProgress = 0f
        slideProgress = 0f
        isJumping = false
        isSliding = false
        speedBoostTimer = 0L
        magnetTimer = 0L
        shieldTimer = 0L
        isShieldActive = false
        obstacles.value = emptyList()
        goldNodes.value = emptyList()
        powerUps.value = emptyList()
        activeParticles.value = emptyList()
        obstacleSpawnTimer = 0f
        itemSpawnTimer = 0f
    }

    fun startGame() {
        resetGame()
        screenState = ScreenState.PLAYING
        lastFrameTime = System.currentTimeMillis()
        triggerVibration(100)
    }

    fun moveLeft() {
        if (screenState != ScreenState.PLAYING) return
        if (playerLane > -1) {
            playerLane -= 1
            triggerVibration(25)
        }
    }

    fun moveRight() {
        if (screenState != ScreenState.PLAYING) return
        if (playerLane < 1) {
            playerLane += 1
            triggerVibration(25)
        }
    }

    fun jump() {
        if (screenState != ScreenState.PLAYING) return
        if (!isJumping && !isSliding) {
            isJumping = true
            jumpStartTime = System.currentTimeMillis()
            triggerVibration(30)
        }
    }

    fun slide() {
        if (screenState != ScreenState.PLAYING) return
        if (!isSliding) {
            // If we were jumping, cancel it
            isJumping = false
            jumpProgress = 0f

            isSliding = true
            slideStartTime = System.currentTimeMillis()
            triggerVibration(30)
        }
    }

    // Upgrades
    fun purchaseUpgrade(type: PowerUpType) {
        viewModelScope.launch {
            val success = repository.upgradePowerUp(type)
            if (success) {
                triggerVibration(120)
            }
        }
    }

    // Main Update Tick (called by Compose frame clock)
    fun gameTick(frameTimeMillis: Long) {
        if (screenState != ScreenState.PLAYING) {
            lastFrameTime = frameTimeMillis
            return
        }

        val deltaTimeMs = (frameTimeMillis - lastFrameTime).coerceIn(1, 100)
        lastFrameTime = frameTimeMillis
        val deltaTimeSec = deltaTimeMs / 1000f

        // 1. Update Distances/Scores
        // Run speed increases slowly over time
        if (!isSpeedBoostActive) {
            baseSpeed += 0.003f * deltaTimeSec
            baseSpeed = baseSpeed.coerceAtMost(0.45f)
        }
        score += currentSpeed * 100f * deltaTimeSec

        // 2. Update player horizontal lane interpolation
        val laneTarget = playerLane.toFloat()
        currentLaneX += (laneTarget - currentLaneX) * 0.22f // Smooth visual shift

        // 3. Update Jump / Slide transitions
        val now = System.currentTimeMillis()
        if (isJumping) {
            val elapsed = now - jumpStartTime
            if (elapsed >= jumpDuration) {
                isJumping = false
                jumpProgress = 0f
            } else {
                val ratio = elapsed.toFloat() / jumpDuration
                // Elegant parabola: peak height of 1.0 at midpoint
                jumpProgress = 4f * ratio * (1f - ratio)
            }
        }

        if (isSliding) {
            val elapsed = now - slideStartTime
            if (elapsed >= slideDuration) {
                isSliding = false
                slideProgress = 0f
            } else {
                slideProgress = 1.0f
            }
        }

        // 4. Update Powerup Timers
        if (speedBoostTimer > 0) {
            speedBoostTimer = (speedBoostTimer - deltaTimeMs).coerceAtLeast(0)
            if (speedBoostTimer == 0L) {
                triggerVibration(80)
            }
        }
        if (magnetTimer > 0) {
            magnetTimer = (magnetTimer - deltaTimeMs).coerceAtLeast(0)
        }
        if (shieldTimer > 0) {
            shieldTimer = (shieldTimer - deltaTimeMs).coerceAtLeast(0)
            if (shieldTimer == 0L) {
                isShieldActive = false
            }
        }

        // 5. Scroll Track
        trackOffset = (trackOffset + currentSpeed * 5f * deltaTimeSec) % 1.0f

        // 6. Update Particles (Gold to Diamonds animation)
        updateParticles(deltaTimeSec)

        // 7. Update Entities (Obstacles, Diamonds, Power-ups)
        updateEntities(deltaTimeSec, now)

        // 8. Spawning Mechanism
        runSpawners(deltaTimeSec)
    }

    private fun updateParticles(deltaTimeSec: Float) {
        val particles = activeParticles.value.toMutableList()
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.progress += p.speed * deltaTimeSec
            if (p.progress >= 1.0f) {
                iterator.remove()
                // Convert finished particle to actual diamond reward
                diamondsThisRun += 1
                triggerVibration(10)
            }
        }
        activeParticles.value = particles
    }

    private fun updateEntities(deltaTimeSec: Float, now: Long) {
        val speed = currentSpeed

        // Update Obstacles
        val updatedObstacles = obstacles.value.mapNotNull { obs ->
            obs.z -= speed * deltaTimeSec
            if (obs.z <= -0.1f) {
                null // pass behind screen, remove
            } else {
                // Collision Detection
                // Player resides around z = 0.05f to 0.12f
                if (obs.z in 0.04f..0.12f) {
                    val isLaneHit = kotlin.math.abs(currentLaneX - obs.lane) < 0.6f
                    if (isLaneHit) {
                        val isDodged = when (obs.type) {
                            ObstacleType.BARRICADE -> jumpProgress > 0.35f
                            ObstacleType.LOW_LASER -> jumpProgress > 0.45f
                            ObstacleType.HIGH_SCANNER -> slideProgress > 0.5f
                            ObstacleType.CYBER_DRONE -> false // too massive to jump or slide
                        }
                        if (!isDodged) {
                            if (isSpeedBoostActive) {
                                // Powerup: Crash through it with sparks!
                                triggerVibration(100)
                                null // Obstacle disintegrates!
                            } else if (isShieldActive) {
                                // Shield breaks!
                                triggerVibration(250)
                                isShieldActive = false
                                shieldTimer = 0L
                                null // Obstacle breaks
                            } else {
                                // Real Crash!
                                handleGameOver()
                                null
                            }
                        } else {
                            obs
                        }
                    } else {
                        obs
                    }
                } else {
                    obs
                }
            }
        }
        obstacles.value = updatedObstacles

        // Update Gold Nodes (which we convert directly to Diamonds)
        val updatedGold = goldNodes.value.mapNotNull { node ->
            node.z -= speed * deltaTimeSec

            // Magnetic attraction
            if (isMagnetActive || isSpeedBoostActive) {
                if (node.z < 0.45f) {
                    // Pull lane towards player lane smoothly
                    val pullSpeed = 4f * deltaTimeSec
                    val diff = currentLaneX - node.lane
                    // Shift the node towards the player horizontally
                    // Note: representing this simple drift
                }
            }

            if (node.z <= -0.1f) {
                null
            } else if (!node.isCollected && node.z in 0.03f..0.15f) {
                // Collection check
                val isNearHorizontal = if (isMagnetActive || isSpeedBoostActive) {
                    true // Magnet sucks it in automatically
                } else {
                    kotlin.math.abs(currentLaneX - node.lane) < 0.45f
                }
                val isHeightHit = jumpProgress < 0.7f // can't collect if jumping way too high

                if (isNearHorizontal && isHeightHit) {
                    node.isCollected = true
                    triggerVibration(15)

                    // Spawn a gorgeous flying conversion particle!
                    // Coordinates will be estimated from screen mapping,
                    // we supply raw starting progress which the UI maps.
                    val screenX = 0.5f + (node.lane * 0.25f) // approximate lane center
                    val particle = DiamondParticle(
                        id = Random.nextLong(),
                        startX = screenX,
                        startY = 0.65f, // approximate height of collection
                        size = 14f + Random.nextFloat() * 12f
                    )
                    activeParticles.value = activeParticles.value + particle
                    null
                } else {
                    node
                }
            } else {
                node
            }
        }
        goldNodes.value = updatedGold

        // Update Powerups
        val updatedPowerUps = powerUps.value.mapNotNull { pUp ->
            pUp.z -= speed * deltaTimeSec
            if (pUp.z <= -0.1f) {
                null
            } else if (!pUp.isCollected && pUp.z in 0.03f..0.15f) {
                val isHit = kotlin.math.abs(currentLaneX - pUp.lane) < 0.45f && jumpProgress < 0.7f
                if (isHit) {
                    pUp.isCollected = true
                    activatePowerUp(pUp.type)
                    null
                } else {
                    pUp
                }
            } else {
                pUp
            }
        }
        powerUps.value = updatedPowerUps
    }

    private fun activatePowerUp(type: PowerUpItemType) {
        val stats = playerStats.value
        triggerVibration(180)

        when (type) {
            PowerUpItemType.SPEED_BOOST -> {
                speedBoostTimer = stats.getSpeedBoostDurationMs()
                // Also give short shield during speed boost to be safe
                shieldTimer = stats.getSpeedBoostDurationMs()
                isShieldActive = true
            }
            PowerUpItemType.MAGNET -> {
                magnetTimer = stats.getMagnetDurationMs()
            }
            PowerUpItemType.SHIELD -> {
                shieldTimer = stats.getShieldDurationMs()
                isShieldActive = true
            }
        }
    }

    private fun handleGameOver() {
        triggerVibration(400)
        screenState = ScreenState.GAME_OVER

        val finalScore = score.toInt()
        val earnedDiamonds = diamondsThisRun

        viewModelScope.launch {
            // Save to Database
            val hasNewHigh = repository.updateHighScore(finalScore)
            isNewHighScore = hasNewHigh
            repository.addDiamonds(earnedDiamonds)
        }
    }

    private fun runSpawners(deltaTimeSec: Float) {
        obstacleSpawnTimer += deltaTimeSec
        itemSpawnTimer += deltaTimeSec

        // 1. Spawning Obstacles
        // Spawning rate depends on score (gets faster!)
        val difficultyFactor = (score / 1500f).coerceIn(0f, 1.5f)
        val spawnInterval = (2.2f - difficultyFactor * 0.8f).coerceAtLeast(1.0f)

        if (obstacleSpawnTimer >= spawnInterval) {
            obstacleSpawnTimer = 0f
            spawnRandomObstacleCluster()
        }

        // 2. Spawning Items (Diamonds / Power-ups)
        if (itemSpawnTimer >= 1.2f) {
            itemSpawnTimer = 0f
            spawnRandomItems()
        }
    }

    private fun spawnRandomObstacleCluster() {
        // Choose a pattern so player always has an open route
        val pattern = Random.nextInt(5)
        val idBase = System.currentTimeMillis()

        when (pattern) {
            0 -> {
                // Centered barricade
                obstacles.value = obstacles.value + Obstacle(idBase, ObstacleType.BARRICADE, 0, 1.0f)
            }
            1 -> {
                // Laser gates in Left & Right lanes
                obstacles.value = obstacles.value + listOf(
                    Obstacle(idBase, ObstacleType.LOW_LASER, -1, 1.0f),
                    Obstacle(idBase + 1, ObstacleType.LOW_LASER, 1, 1.0f)
                )
            }
            2 -> {
                // High scanner scanning Center
                obstacles.value = obstacles.value + Obstacle(idBase, ObstacleType.HIGH_SCANNER, 0, 1.0f)
            }
            3 -> {
                // Large Drone in Left lane, barricade in Center, Right is free!
                obstacles.value = obstacles.value + listOf(
                    Obstacle(idBase, ObstacleType.CYBER_DRONE, -1, 1.0f),
                    Obstacle(idBase + 1, ObstacleType.BARRICADE, 0, 1.005f)
                )
            }
            4 -> {
                // Heavy drones in Center and Right, Left is free
                obstacles.value = obstacles.value + listOf(
                    Obstacle(idBase, ObstacleType.CYBER_DRONE, 0, 1.0f),
                    Obstacle(idBase + 1, ObstacleType.CYBER_DRONE, 1, 1.0f)
                )
            }
        }
    }

    private fun spawnRandomItems() {
        val idBase = System.currentTimeMillis()

        // Spawn a string of gold nodes (will convert to diamonds)
        // Highly satisfying if they are grouped in arcs or lanes
        val spawnRoll = Random.nextFloat()
        if (spawnRoll < 0.65f) {
            // Spawn gold lane
            val lane = Random.nextInt(-1, 2)
            // Spawn 3 consecutive nodes spaced along z
            goldNodes.value = goldNodes.value + listOf(
                GoldNode(idBase, lane, 1.0f),
                GoldNode(idBase + 1, lane, 1.15f),
                GoldNode(idBase + 2, lane, 1.30f)
            )
        } else if (spawnRoll < 0.85f) {
            // Spawn diagonal wave of gold nodes
            val startLane = if (Random.nextBoolean()) -1 else 1
            val centerLane = 0
            val endLane = -startLane
            goldNodes.value = goldNodes.value + listOf(
                GoldNode(idBase, startLane, 1.0f),
                GoldNode(idBase + 1, centerLane, 1.12f),
                GoldNode(idBase + 2, endLane, 1.24f)
            )
        } else {
            // Spawn a Power-up!
            val lane = Random.nextInt(-1, 2)
            val typeRoll = Random.nextInt(3)
            val powerUpType = when (typeRoll) {
                0 -> PowerUpItemType.SPEED_BOOST
                1 -> PowerUpItemType.MAGNET
                else -> PowerUpItemType.SHIELD
            }
            // Check if there are already too many powerups, avoid clutter
            if (powerUps.value.size < 2) {
                powerUps.value = powerUps.value + PowerUpItem(idBase, powerUpType, lane, 1.0f)
            }
        }
    }
}
