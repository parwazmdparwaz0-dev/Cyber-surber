package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.PowerUpType
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CyberSurferGame(
    modifier: Modifier = Modifier,
    viewModel: GameViewModel = viewModel()
) {
    val stats by viewModel.playerStats.collectAsStateWithLifecycle()

    // Sync game tick with Compose Frame Clock
    if (viewModel.screenState == ScreenState.PLAYING) {
        LaunchedEffect(Unit) {
            while (true) {
                withFrameMillis { frameTime ->
                    viewModel.gameTick(frameTime)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050505)) // Immersive deep black background
    ) {
        when (viewModel.screenState) {
            ScreenState.MAIN_MENU -> MainMenuScreen(viewModel = viewModel, stats = stats)
            ScreenState.PLAYING -> GamePlayScreen(viewModel = viewModel)
            ScreenState.PAUSED -> GamePlayScreen(viewModel = viewModel) // Shown with pause overlay
            ScreenState.GAME_OVER -> GameOverScreen(viewModel = viewModel)
            ScreenState.UPGRADES -> UpgradesScreen(viewModel = viewModel, stats = stats)
        }
    }
}

// -----------------------------------------------------------------------------
// MAIN MENU SCREEN
// -----------------------------------------------------------------------------
@Composable
fun MainMenuScreen(viewModel: GameViewModel, stats: com.example.data.PlayerStats) {
    // Pulse animation for the start button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "button_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                // Abstract cyber grid lines on background matching #22d3ee grid in HTML
                val gridColor = Color(0xFF22D3EE).copy(alpha = 0.12f)
                val spacing = 60.dp.toPx()
                for (x in 0..size.width.toInt() step spacing.toInt()) {
                    drawLine(gridColor, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), strokeWidth = 1f)
                }
                for (y in 0..size.height.toInt() step spacing.toInt()) {
                    drawLine(gridColor, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), strokeWidth = 1f)
                }
                // Ambient bottom cyan glow matching modern theme
                drawCircle(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF22D3EE).copy(alpha = 0.15f), Color.Transparent),
                        center = Offset(size.width / 2, size.height * 0.9f),
                        radius = size.width * 0.7f
                    )
                )
            }
            .padding(24.dp)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP HUD BAR
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // High Score
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "High Score",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "MAX SCORE",
                        color = Color(0xFF8A82A0),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = stats.highScore.toString(),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Diamonds Wallet
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF160B30))
                    .border(1.dp, Color(0xFF00FFCC).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite, // Diamond approximation
                    contentDescription = "Diamonds",
                    tint = Color(0xFF00FFCC),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stats.diamonds.toString(),
                    color = Color(0xFF00FFCC),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.testTag("menu_diamonds_text")
                )
            }
        }

        // TITLE & LOGO
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Glowing background orb
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF22D3EE).copy(alpha = 0.45f), Color.Transparent)
                            )
                        )
                )
                // Sleek cyberpunk vector symbol
                Icon(
                    imageVector = Icons.Default.PlayArrow, // Futuristic speed triangle
                    contentDescription = "Logo",
                    tint = Color.White,
                    modifier = Modifier.size(80.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "CYBER SURFER",
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color(0xFF22D3EE),
                        offset = Offset(0f, 0f),
                        blurRadius = 15f
                    )
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "REFINE GOLD TO DIAMONDS • SURVIVE HYPER-SPEED",
                color = Color(0xFF22D3EE),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center
            )
        }

        // ACTION BUTTONS
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Start Run Button
            Button(
                onClick = { viewModel.startGame() },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(64.dp)
                    .scale(scale)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF22D3EE), Color(0xFF2563EB))
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .testTag("start_run_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ACTIVATE RUN",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Upgrade Terminal Button
            OutlinedButton(
                onClick = { viewModel.setScreen(ScreenState.UPGRADES) },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp)
                    .testTag("upgrades_button"),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, Color(0xFF00FFCC)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF00FFCC)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color(0xFF00FFCC))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "UPGRADE TERMINAL",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Tutorial Line
            Text(
                text = "SWIPE OR USE HUD CONTROLS TO DODGE & JUMP",
                color = Color(0xFF8A82A0),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )
        }
    }
}

// -----------------------------------------------------------------------------
// GAMEPLAY SCREEN & CUSTOM RENDERER
// -----------------------------------------------------------------------------
@Composable
fun GamePlayScreen(viewModel: GameViewModel) {
    // Collect drag coordinates for swipe triggers
    var dragX by remember { mutableStateOf(0f) }
    var dragY by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragX += dragAmount.x
                        dragY += dragAmount.y
                    },
                    onDragEnd = {
                        val threshold = 45f
                        if (kotlin.math.abs(dragX) > kotlin.math.abs(dragY)) {
                            if (dragX > threshold) {
                                viewModel.moveRight()
                            } else if (dragX < -threshold) {
                                viewModel.moveLeft()
                            }
                        } else {
                            if (dragY > threshold) {
                                viewModel.slide()
                            } else if (dragY < -threshold) {
                                viewModel.jump()
                            }
                        }
                        dragX = 0f
                        dragY = 0f
                    },
                    onDragCancel = {
                        dragX = 0f
                        dragY = 0f
                    }
                )
            }
    ) {
        // 1. GAME RENDERER (Canvas)
        GameCanvasRenderer(viewModel = viewModel)

        // 2. HEADS-UP DISPLAY (HUD)
        GameHUD(viewModel = viewModel)

        // 3. OPTIONAL CYBER STEERING CONTROLS (Floating overlays for perfect emulator playability)
        FloatingCyberControls(viewModel = viewModel)

        // 4. PAUSE OVERLAY (if game state is PAUSED)
        AnimatedVisibility(
            visible = viewModel.screenState == ScreenState.PAUSED,
            enter = fadeIn(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(250))
        ) {
            PauseOverlay(viewModel = viewModel)
        }
    }
}

@Composable
fun GameCanvasRenderer(viewModel: GameViewModel) {
    // Continuously animate glowing variables
    val infiniteTransition = rememberInfiniteTransition(label = "canvas_glow")
    val laserPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_pulse"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .testTag("game_canvas")
    ) {
        val width = size.width
        val height = size.height

        // Define perspective landmarks
        val horizonY = height * 0.32f
        val maxLaneSpacing = width * 0.36f // width of lane at screen bottom

        // Utility: map 3D lane coordinates (L, Z) to 2D screen coordinates (X, Y)
        fun project(lane: Float, z: Float): Offset {
            val progress = (1.0f - z).coerceIn(0f, 1.1f)
            // Quadratic scaling for perfect depth illusion
            val y = horizonY + (height - horizonY) * progress * progress
            val laneSpacing = maxLaneSpacing * progress
            val x = (width / 2f) + lane * laneSpacing
            return Offset(x, y)
        }

        // -------------------------------------------------------------
        // DRAW BACKGROUND (Above Horizon)
        // -------------------------------------------------------------
        // Space Sky Gradient matching from-[#0a0f1d] to-[#1e1b4b]
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0A0F1D), Color(0xFF0F122B), Color(0xFF151233)),
                startY = 0f,
                endY = horizonY
            ),
            size = Size(width, horizonY)
        )

        // Draw Retro Cyber Sun
        val sunCenter = Offset(width / 2, horizonY - 10.dp.toPx())
        val sunRadius = 70.dp.toPx()
        drawCircle(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFF007F), Color(0xFFFF9900)),
                startY = sunCenter.y - sunRadius,
                endY = sunCenter.y + sunRadius
            ),
            radius = sunRadius,
            center = sunCenter
        )
        // Draw digital scanlines cuts through sun (Retro-futuristic synthwave style)
        for (i in 1..6) {
            val cutY = sunCenter.y + (sunRadius * (i / 7f))
            val cutHeight = 2.dp.toPx() * i
            drawRect(
                color = Color(0xFF050505),
                topLeft = Offset(sunCenter.x - sunRadius, cutY),
                size = Size(sunRadius * 2, cutHeight)
            )
        }

        // Distant Glowing Grid Silhouette
        val cityPath = Path().apply {
            moveTo(0f, horizonY)
            lineTo(width * 0.1f, horizonY - 40f)
            lineTo(width * 0.15f, horizonY - 40f)
            lineTo(width * 0.18f, horizonY - 15f)
            lineTo(width * 0.25f, horizonY - 70f)
            lineTo(width * 0.3f, horizonY - 70f)
            lineTo(width * 0.34f, horizonY)
            lineTo(width * 0.65f, horizonY)
            lineTo(width * 0.7f, horizonY - 50f)
            lineTo(width * 0.74f, horizonY - 50f)
            lineTo(width * 0.77f, horizonY - 10f)
            lineTo(width * 0.82f, horizonY - 80f)
            lineTo(width * 0.88f, horizonY - 80f)
            lineTo(width * 0.93f, horizonY)
            lineTo(width, horizonY)
        }
        drawPath(cityPath, color = Color(0xFF0C051D))

        // Draw Horizon Glowing line
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Color(0xFF00FFCC), Color(0xFFFF007F), Color.Transparent)
            ),
            start = Offset(0f, horizonY),
            end = Offset(width, horizonY),
            strokeWidth = 3f
        )

        // -------------------------------------------------------------
        // DRAW ROAD & LANES (Below Horizon)
        // -------------------------------------------------------------
        // Draw perspective track surface
        val trackPath = Path().apply {
            val tl = project(-1.5f, 1.0f)
            val tr = project(1.5f, 1.0f)
            val br = project(1.5f, 0.0f)
            val bl = project(-1.5f, 0.0f)
            moveTo(tl.x, tl.y)
            lineTo(tr.x, tr.y)
            lineTo(br.x, br.y)
            lineTo(bl.x, bl.y)
            close()
        }
        drawPath(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF151233).copy(alpha = 0.85f), Color(0xFF1E1B4B).copy(alpha = 0.95f)),
                startY = horizonY,
                endY = height
            ),
            path = trackPath
        )

        // Draw 3 Lane Rails (Neon Tracks)
        val lanesList = listOf(-1.5f, -0.5f, 0.5f, 1.5f)
        lanesList.forEach { laneOffset ->
            val start = project(laneOffset, 1.0f)
            val end = project(laneOffset, 0.0f)
            drawLine(
                color = Color(0xFF22D3EE).copy(alpha = 0.7f),
                start = start,
                end = end,
                strokeWidth = 3f
            )
        }

        // Draw scrolling horizontal grid lines (creates high speed illusion)
        for (i in 0..12) {
            // Distribute lines exponentially for correct 3D perspective scroll
            val z = (i.toFloat() / 12f - viewModel.trackOffset).let {
                if (it < 0f) it + 1.0f else it
            }
            val lineLeft = project(-1.5f, z)
            val lineRight = project(1.5f, z)
            val alpha = (1.0f - z).coerceIn(0f, 1f) // Fades near the horizon

            drawLine(
                color = Color(0xFF00FFCC).copy(alpha = alpha * 0.4f),
                start = Offset(lineLeft.x, lineLeft.y),
                end = Offset(lineRight.x, lineRight.y),
                strokeWidth = (2f + (1f - z) * 4f) // lines get thicker as they get closer
            )
        }

        // Speed Boost Warp Trails (Blurs along side edges when super fast)
        if (viewModel.isSpeedBoostActive) {
            for (side in listOf(-1.6f, 1.6f)) {
                for (j in 0..15) {
                    val z = (j / 15f + viewModel.trackOffset * 2f) % 1.0f
                    val pt = project(side, z)
                    val trailLength = 0.08f
                    val ptEnd = project(side, (z - trailLength).coerceAtLeast(0f))
                    val pAlpha = (1f - z) * 0.6f

                    drawLine(
                        color = Color(0xFFFF007F).copy(alpha = pAlpha),
                        start = pt,
                        end = ptEnd,
                        strokeWidth = 6f
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // DRAW GOLD NODES (Convert into Diamonds on approach/pickup)
        // -------------------------------------------------------------
        viewModel.goldNodes.value.forEach { node ->
            if (node.z in 0.0f..1.0f && !node.isCollected) {
                val pt = project(node.lane.toFloat(), node.z)
                val sizeScale = (1.0f - node.z).coerceIn(0.1f, 1.0f)
                val radius = 12.dp.toPx() * sizeScale

                // Drawn as glowing spinning Cyan Diamonds / Crystals
                val angle = (System.currentTimeMillis() / 250f) % (2f * Math.PI.toFloat())
                val points = (0..3).map { index ->
                    val a = angle + index * (Math.PI.toFloat() / 2f)
                    Offset(
                        pt.x + radius * cos(a),
                        pt.y + radius * 0.6f * sin(a) // slight squash for projection
                    )
                }

                // Inner core
                drawCircle(
                    color = Color(0xFF00FFCC),
                    radius = radius * 0.4f,
                    center = pt
                )

                // Wireframe glowing crystal outline
                for (idx in points.indices) {
                    val p1 = points[idx]
                    val p2 = points[(idx + 1) % points.size]
                    drawLine(
                        color = Color(0xFF22D3EE),
                        start = p1,
                        end = p2,
                        strokeWidth = 2f * sizeScale
                    )
                    // Draw lines from core to vertices
                    drawLine(
                        color = Color(0xFFE0F7FA),
                        start = pt,
                        end = p1,
                        strokeWidth = 1f * sizeScale
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // DRAW POWER-UP HOVERING CAPSULES
        // -------------------------------------------------------------
        viewModel.powerUps.value.forEach { pUp ->
            if (pUp.z in 0.0f..1.0f && !pUp.isCollected) {
                val pt = project(pUp.lane.toFloat(), pUp.z)
                val sizeScale = (1.0f - pUp.z).coerceIn(0.1f, 1.0f)
                val capsuleW = 16.dp.toPx() * sizeScale
                val capsuleH = 26.dp.toPx() * sizeScale

                // Hovering vertical displacement
                val hoverOffset = sin(System.currentTimeMillis() / 150f) * 6.dp.toPx() * sizeScale
                val center = Offset(pt.x, pt.y - capsuleH / 2 + hoverOffset)

                val themeColor = when (pUp.type) {
                    PowerUpItemType.SPEED_BOOST -> Color(0xFFFF007F)
                    PowerUpItemType.MAGNET -> Color(0xFF00FFCC)
                    PowerUpItemType.SHIELD -> Color(0xFF0099FF)
                }

                // Draw glowing aura
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(themeColor.copy(alpha = 0.5f), Color.Transparent),
                        center = center,
                        radius = capsuleH * 1.2f
                    ),
                    radius = capsuleH * 1.2f,
                    center = center
                )

                // Draw Capsule Outer Frame
                drawRoundRect(
                    color = themeColor,
                    topLeft = Offset(center.x - capsuleW / 2, center.y - capsuleH / 2),
                    size = Size(capsuleW, capsuleH),
                    cornerRadius = CornerRadius(capsuleW / 2, capsuleW / 2),
                    style = Stroke(width = 2.5f * sizeScale)
                )

                // Draw Capsule core
                drawCircle(
                    color = Color.White,
                    radius = capsuleW * 0.3f,
                    center = center
                )
            }
        }

        // -------------------------------------------------------------
        // DRAW OBSTACLES (Dodging items)
        // -------------------------------------------------------------
        viewModel.obstacles.value.forEach { obs ->
            if (obs.z in 0.0f..1.0f) {
                val pt = project(obs.lane.toFloat(), obs.z)
                val scale = (1.0f - obs.z).coerceIn(0.1f, 1.0f)

                when (obs.type) {
                    ObstacleType.BARRICADE -> {
                        // Drawing a cybernetic neon wall hurdle
                        val wallW = 54.dp.toPx() * scale
                        val wallH = 24.dp.toPx() * scale
                        val left = pt.x - wallW / 2f
                        val top = pt.y - wallH

                        // Filled back gradient
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF3B0066).copy(alpha = 0.7f), Color(0xFF140026).copy(alpha = 0.9f)),
                                startY = top,
                                endY = pt.y
                            ),
                            topLeft = Offset(left, top),
                            size = Size(wallW, wallH),
                            cornerRadius = CornerRadius(6.dp.toPx() * scale, 6.dp.toPx() * scale)
                        )

                        // Glowing Pink outlines
                        drawRoundRect(
                            color = Color(0xFFFF007F),
                            topLeft = Offset(left, top),
                            size = Size(wallW, wallH),
                            cornerRadius = CornerRadius(6.dp.toPx() * scale, 6.dp.toPx() * scale),
                            style = Stroke(width = 2.5f * scale)
                        )

                        // Yellow hazard decals on wall face
                        val stripW = 6.dp.toPx() * scale
                        for (dx in 0..3) {
                            val stripeX = left + (wallW * 0.2f) + dx * (wallW * 0.2f)
                            drawLine(
                                color = Color(0xFFFFCC00),
                                start = Offset(stripeX, pt.y),
                                end = Offset(stripeX - stripW, top),
                                strokeWidth = 2.5f * scale
                            )
                        }
                    }

                    ObstacleType.LOW_LASER -> {
                        // Laser emitter poles on lanes
                        val emitterRadius = 7.dp.toPx() * scale
                        val laserHeight = 12.dp.toPx() * scale
                        val poleY = pt.y - laserHeight

                        // Draw emitter nodes
                        drawCircle(color = Color(0xFF1F1138), radius = emitterRadius, center = Offset(pt.x, poleY))
                        drawCircle(color = Color(0xFFFF007F), radius = emitterRadius * 0.4f, center = Offset(pt.x, poleY))

                        // Draw horizontal pulsing laser beam (full lane block)
                        val beamW = width * 0.28f * scale
                        drawLine(
                            color = Color(0xFFFF007F).copy(alpha = laserPulse),
                            start = Offset(pt.x - beamW / 2, poleY),
                            end = Offset(pt.x + beamW / 2, poleY),
                            strokeWidth = 4f * scale
                        )
                        // Core hot-white laser thread
                        drawLine(
                            color = Color.White,
                            start = Offset(pt.x - beamW / 2, poleY),
                            end = Offset(pt.x + beamW / 2, poleY),
                            strokeWidth = 1.2f * scale
                        )
                    }

                    ObstacleType.HIGH_SCANNER -> {
                        // Floating high warning arch scanning downwards
                        val archW = width * 0.32f * scale
                        val archH = 50.dp.toPx() * scale
                        val topY = pt.y - archH

                        // Left & Right arch legs
                        drawLine(Color(0xFF00FFCC), Offset(pt.x - archW / 2, pt.y), Offset(pt.x - archW / 2, topY), strokeWidth = 2f * scale)
                        drawLine(Color(0xFF00FFCC), Offset(pt.x + archW / 2, pt.y), Offset(pt.x + archW / 2, topY), strokeWidth = 2f * scale)

                        // Top horizontal bar
                        drawLine(Color(0xFF00FFCC), Offset(pt.x - archW / 2, topY), Offset(pt.x + archW / 2, topY), strokeWidth = 3f * scale)

                        // Scanning field descending from topY down to pt.y
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF00FFCC).copy(alpha = 0.4f * laserPulse), Color.Transparent),
                                startY = topY,
                                endY = pt.y
                            ),
                            topLeft = Offset(pt.x - archW / 2 + 2f, topY),
                            size = Size(archW - 4f, archH)
                        )
                    }

                    ObstacleType.CYBER_DRONE -> {
                        // Floating cyber drone obstacle
                        val droneW = 38.dp.toPx() * scale
                        val droneH = 26.dp.toPx() * scale
                        // Floating movement
                        val bobY = sin(System.currentTimeMillis() / 120f) * 6.dp.toPx() * scale
                        val center = Offset(pt.x, pt.y - 45.dp.toPx() * scale + bobY)

                        // Draw metal wings
                        val leftWing = Offset(center.x - droneW * 0.7f, center.y + droneH * 0.1f)
                        val rightWing = Offset(center.x + droneW * 0.7f, center.y + droneH * 0.1f)
                        drawLine(Color(0xFF00FFFF), center, leftWing, strokeWidth = 4f * scale)
                        drawLine(Color(0xFF00FFFF), center, rightWing, strokeWidth = 4f * scale)

                        // Draw Drone Spherical Body
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF141A30), Color(0xFF080C16)),
                                center = center,
                                radius = droneW / 2
                            ),
                            radius = droneW / 2,
                            center = center
                        )

                        // Central Eye (scanning indicator)
                        val eyeColor = if ((System.currentTimeMillis() / 400) % 2 == 0L) Color.Red else Color(0xFFFFCC00)
                        drawCircle(
                            color = eyeColor,
                            radius = droneW * 0.15f,
                            center = center
                        )

                        // Thrust sparks underneath
                        drawLine(
                            color = Color(0xFFFF4500),
                            start = Offset(center.x, center.y + droneH / 2),
                            end = Offset(center.x, center.y + droneH / 2 + 15.dp.toPx() * scale * laserPulse),
                            strokeWidth = 3f * scale
                        )
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // DRAW PARTICLES (Gold Refined flying up to top corner HUD)
        // -------------------------------------------------------------
        // Top right destination for flying diamonds
        val targetX = width * 0.85f
        val targetY = 32.dp.toPx()

        viewModel.activeParticles.value.forEach { particle ->
            // Draw a shiny diamond particle at its animated position
            // Interpolating from relative start coordinates to real target pixel coordinates
            val currentPixelX = particle.startX * width + (targetX - particle.startX * width) * particle.progress
            val arcOffset = -150.dp.toPx() * (4f * particle.progress * (1f - particle.progress))
            val currentPixelY = particle.startY * height + (targetY - particle.startY * height) * particle.progress + arcOffset

            val r = particle.size.dp.toPx()
            val diamondPath = Path().apply {
                moveTo(currentPixelX, currentPixelY - r)
                lineTo(currentPixelX + r * 0.7f, currentPixelY)
                lineTo(currentPixelX, currentPixelY + r)
                lineTo(currentPixelX - r * 0.7f, currentPixelY)
                close()
            }

            // Glow backing
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF00FFCC).copy(alpha = 0.5f), Color.Transparent),
                    center = Offset(currentPixelX, currentPixelY),
                    radius = r * 2.2f
                ),
                radius = r * 2.2f,
                center = Offset(currentPixelX, currentPixelY)
            )

            // Diamond solid face
            drawPath(
                color = Color(0xFFE0FFFF),
                path = diamondPath
            )
            drawPath(
                color = Color(0xFF00FFCC),
                path = diamondPath,
                style = Stroke(width = 2f)
            )
        }

        // -------------------------------------------------------------
        // DRAW PLAYER (Hoverboard Cyber Runner)
        // -------------------------------------------------------------
        // Player is anchored near bottom of road
        val playerBaseZ = 0.08f
        val playerPos = project(viewModel.currentLaneX, playerBaseZ)

        // Adjust coordinates based on jumping/sliding state
        val jumpHeightPixels = viewModel.jumpProgress * 130.dp.toPx()
        val pX = playerPos.x
        val pY = playerPos.y - jumpHeightPixels

        val stats = viewModel.playerStats.value

        // Draw engine plasma trail on ground (glow behind board)
        if (!viewModel.isSliding) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF00FFCC).copy(alpha = 0.4f), Color.Transparent),
                    center = Offset(pX, playerPos.y + 10.dp.toPx()),
                    radius = 36.dp.toPx()
                ),
                radius = 36.dp.toPx(),
                center = Offset(pX, playerPos.y + 10.dp.toPx())
            )
        } else {
            // Sliding sparks trail on floor
            val sparkWidth = 40.dp.toPx()
            drawOval(
                color = Color(0xFFFF9900).copy(alpha = 0.5f),
                topLeft = Offset(pX - sparkWidth / 2, playerPos.y - 4.dp.toPx()),
                size = Size(sparkWidth, 10.dp.toPx())
            )
        }

        // Draw Hoverboard Vector
        val boardW = 22.dp.toPx()
        val boardH = 8.dp.toPx()
        val boardPath = Path().apply {
            moveTo(pX, pY + boardH) // Tip
            lineTo(pX - boardW / 2, pY)
            lineTo(pX, pY - boardH * 0.4f)
            lineTo(pX + boardW / 2, pY)
            close()
        }
        drawPath(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF1A1A1A), Color(0xFF333333), Color(0xFF1A1A1A))
            ),
            path = boardPath
        )
        // Board Neon glow border
        drawPath(
            color = Color(0xFF00FFCC),
            path = boardPath,
            style = Stroke(width = 2f)
        )

        // Draw Rider Vector (stylized geometric futuristic runner silhouette)
        val riderH = if (viewModel.isSliding) 24.dp.toPx() else 46.dp.toPx()
        val riderW = if (viewModel.isSliding) 18.dp.toPx() else 14.dp.toPx()

        // Legs/Knees connection to board
        drawLine(Color(0xFF00FFCC), Offset(pX - 5.dp.toPx(), pY), Offset(pX - 4.dp.toPx(), pY - riderH * 0.2f), strokeWidth = 3f)
        drawLine(Color(0xFF00FFCC), Offset(pX + 5.dp.toPx(), pY), Offset(pX + 4.dp.toPx(), pY - riderH * 0.2f), strokeWidth = 3f)

        // Torso/Chest
        val torsoPath = Path().apply {
            moveTo(pX - riderW / 2, pY - riderH * 0.2f)
            lineTo(pX + riderW / 2, pY - riderH * 0.2f)
            lineTo(pX + riderW * 0.4f, pY - riderH * 0.75f)
            lineTo(pX - riderW * 0.4f, pY - riderH * 0.75f)
            close()
        }
        drawPath(
            brush = Brush.verticalGradient(colors = listOf(Color(0xFF160A30), Color(0xFF321E5C))),
            path = torsoPath
        )
        drawPath(
            color = Color(0xFFFF007F),
            path = torsoPath,
            style = Stroke(width = 1.5f)
        )

        // Glowing visor helmet (Head)
        val headCenter = Offset(pX, pY - riderH * 0.86f)
        val headRadius = 6.dp.toPx()
        drawCircle(color = Color(0xFF1A0A30), radius = headRadius, center = headCenter)
        drawCircle(color = Color(0xFFFF007F), radius = headRadius, center = headCenter, style = Stroke(width = 1.5f))

        // Visor glow
        drawLine(
            color = Color(0xFF00FFCC),
            start = Offset(headCenter.x - headRadius * 0.7f, headCenter.y - headRadius * 0.1f),
            end = Offset(headCenter.x + headRadius * 0.7f, headCenter.y - headRadius * 0.1f),
            strokeWidth = 2.5f
        )

        // Engine thruster flames on the back
        val thrusterY = pY - riderH * 0.4f
        val thrusterFlare = 15.dp.toPx() * (1f + 0.3f * laserPulse)
        drawLine(
            color = Color(0xFF00FFCC),
            start = Offset(pX, thrusterY),
            end = Offset(pX, thrusterY + thrusterFlare),
            strokeWidth = 3.5f
        )

        // -------------------------------------------------------------
        // ACTIVE FORCEFIELD SHIELD (Concentric Hexagonal Energy Shield)
        // -------------------------------------------------------------
        if (viewModel.isShieldActive) {
            val shieldRadius = 40.dp.toPx()
            val shieldCenter = Offset(pX, pY - riderH * 0.5f)

            // Radiant background field fill
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF0099FF).copy(alpha = 0.2f), Color.Transparent),
                    center = shieldCenter,
                    radius = shieldRadius * 1.3f
                ),
                radius = shieldRadius * 1.3f,
                center = shieldCenter
            )

            // Neon cyan force ring
            drawCircle(
                color = Color(0xFF00BFFF).copy(alpha = 0.8f),
                radius = shieldRadius,
                center = shieldCenter,
                style = Stroke(width = 1.5f)
            )

            // Inner electric web
            for (ang in 0..360 step 60) {
                val rad = Math.toRadians(ang.toDouble())
                val endX = shieldCenter.x + shieldRadius * cos(rad).toFloat()
                val endY = shieldCenter.y + shieldRadius * sin(rad).toFloat()
                drawLine(
                    color = Color(0xFF87CEFA).copy(alpha = 0.4f),
                    start = shieldCenter,
                    end = Offset(endX, endY),
                    strokeWidth = 1f
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// HEADS-UP DISPLAY (HUD)
// -----------------------------------------------------------------------------
@Composable
fun GameHUD(viewModel: GameViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .safeDrawingPadding()
    ) {
        // TOP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // PAUSE BUTTON
            IconButton(
                onClick = {
                    if (viewModel.screenState == ScreenState.PLAYING) {
                        viewModel.setScreen(ScreenState.PAUSED)
                    }
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A).copy(alpha = 0.8f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                    .size(48.dp)
                    .testTag("pause_game_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Pause",
                    tint = Color.White
                )
            }

            // SCORE / DISTANCE RUN (Immersive UI: Current Score italic font-black cyan glow)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "CURRENT SCORE",
                    color = Color(0xFF22D3EE),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = viewModel.score.toInt().toString(),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color(0xFF22D3EE).copy(alpha = 0.8f),
                            offset = Offset(0f, 0f),
                            blurRadius = 12f
                        )
                    ),
                    modifier = Modifier.testTag("live_score_text")
                )
            }

            // LIVE DIAMONDS COUNT (Immersive UI: white/10 rounded-full badge with cyan-400 circular diamond)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22D3EE))
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Diamonds",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = viewModel.diamondsThisRun.toString(),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("live_diamonds_text")
                )
            }
        }

        // ACTIVE POWER-UP METERS (Gauges displayed vertically in top-left)
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 72.dp)
                .width(190.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Speed Booster Meter
            if (viewModel.isSpeedBoostActive) {
                PowerUpMeter(
                    name = "HYPER BOOST",
                    color = Color(0xFFFF007F),
                    icon = Icons.Default.PlayArrow,
                    durationMs = viewModel.speedBoostTimer,
                    maxDurationMs = viewModel.playerStats.value.getSpeedBoostDurationMs()
                )
            }

            // Magnet Meter
            if (viewModel.isMagnetActive) {
                PowerUpMeter(
                    name = "DIAMOND MAGNET",
                    color = Color(0xFF00FFCC),
                    icon = Icons.Default.Star,
                    durationMs = viewModel.magnetTimer,
                    maxDurationMs = viewModel.playerStats.value.getMagnetDurationMs()
                )
            }

            // Shield Meter
            if (viewModel.isShieldActive && !viewModel.isSpeedBoostActive) {
                PowerUpMeter(
                    name = "REFINERY SHIELD",
                    color = Color(0xFF00BFFF),
                    icon = Icons.Default.Check,
                    durationMs = viewModel.shieldTimer,
                    maxDurationMs = viewModel.playerStats.value.getShieldDurationMs()
                )
            }
        }
    }
}

@Composable
fun PowerUpMeter(
    name: String,
    color: Color,
    imageVector: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.PlayArrow, // placeholder
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    durationMs: Long,
    maxDurationMs: Long
) {
    val progress = (durationMs.toFloat() / maxDurationMs.toFloat()).coerceIn(0f, 1f)
    val secondsLeft = String.format(java.util.Locale.US, "%.1fs", durationMs.toFloat() / 1000f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F172A).copy(alpha = 0.8f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Glowing Icon Box matching HTML: bg-gradient-to-br from-cyan-400 to-blue-600
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(color, Color(0xFF2563EB))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        // Title and Bar Column
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = name,
                    color = color,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = secondsLeft,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            // Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// CYBER STEERING FLOATING HUD CONTROLS
// -----------------------------------------------------------------------------
@Composable
fun FloatingCyberControls(viewModel: GameViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        // Horizontal Movement Keys (Lanes) at bottom left
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Move Left (Immersive: slate-900 bg with cyan-400 border)
            IconButton(
                onClick = { viewModel.moveLeft() },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                    .border(2.dp, Color(0xFF22D3EE), RoundedCornerShape(12.dp))
                    .size(54.dp)
                    .testTag("hud_left_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Left",
                    tint = Color(0xFF22D3EE),
                    modifier = Modifier.size(24.dp)
                )
            }

            // Move Right (Immersive: slate-900 bg with cyan-400 border)
            IconButton(
                onClick = { viewModel.moveRight() },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                    .border(2.dp, Color(0xFF22D3EE), RoundedCornerShape(12.dp))
                    .size(54.dp)
                    .testTag("hud_right_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Right",
                    tint = Color(0xFF22D3EE),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Action Keys (Jump / Slide) at bottom right (Immersive: slate-900 bg with cyan-400 border)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Jump Button
            Button(
                onClick = { viewModel.jump() },
                modifier = Modifier
                    .height(48.dp)
                    .width(96.dp)
                    .border(1.dp, Color(0xFF22D3EE), RoundedCornerShape(10.dp))
                    .testTag("hud_jump_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0F172A).copy(alpha = 0.85f)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = Color(0xFF22D3EE),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "JUMP",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Slide Button
            Button(
                onClick = { viewModel.slide() },
                modifier = Modifier
                    .height(48.dp)
                    .width(96.dp)
                    .border(1.dp, Color(0xFF22D3EE), RoundedCornerShape(10.dp))
                    .testTag("hud_slide_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0F172A).copy(alpha = 0.85f)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0xFF22D3EE),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SLIDE",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// PAUSE OVERLAY
// -----------------------------------------------------------------------------
@Composable
fun PauseOverlay(viewModel: GameViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050505).copy(alpha = 0.85f))
            .clickable(enabled = false) {}, // absorb clicks
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(280.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0F172A))
                .border(2.dp, Color(0xFF22D3EE), RoundedCornerShape(24.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "RUN PAUSED",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                style = TextStyle(
                    shadow = Shadow(color = Color(0xFF22D3EE), blurRadius = 12f)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "REFINERY OFFLINE TEMPORARILY",
                color = Color(0xFF22D3EE).copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Resume Button with linear gradient
            Button(
                onClick = { viewModel.setScreen(ScreenState.PLAYING) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF22D3EE), Color(0xFF2563EB))
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .testTag("resume_run_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RESUME RUN", fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 1.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Exit Button styled with cyan outline
            OutlinedButton(
                onClick = { viewModel.setScreen(ScreenState.MAIN_MENU) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("exit_run_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, Color(0xFF22D3EE)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF22D3EE))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color(0xFF22D3EE))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("TERMINATE RUN", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// GAME OVER SCREEN
// -----------------------------------------------------------------------------
@Composable
fun GameOverScreen(viewModel: GameViewModel) {
    val stats by viewModel.playerStats.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                // Diagonal speed lines backplate in cyan
                val color = Color(0xFF22D3EE).copy(alpha = 0.05f)
                for (i in 0..size.width.toInt() step 40) {
                    drawLine(
                        color = color,
                        start = Offset(i.toFloat(), 0f),
                        end = Offset(i.toFloat() - 150f, size.height),
                        strokeWidth = 2f
                    )
                }
            }
            .padding(24.dp)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP HEADER
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 16.dp)) {
            Text(
                text = "CONNECTION TERMINATED",
                color = Color(0xFFFF3333),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "CRITICAL COLLISION",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                style = TextStyle(
                    shadow = Shadow(color = Color(0xFF22D3EE), blurRadius = 12f)
                )
            )
        }

        // STATS BOX (Immersive UI slate card with cyan border)
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F172A))
                .border(2.dp, Color(0xFF22D3EE).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // New Highscore banner
            if (viewModel.isNewHighScore) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF22D3EE).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFF22D3EE), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "NEW RECORD SECURED!",
                        color = Color(0xFF22D3EE),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            // Distance Ran
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("DISTANCE COVERED", color = Color(0xFF8A82A0), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text("${viewModel.score.toInt()}m", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }

            Divider(color = Color(0xFF1E293B), thickness = 1.dp)

            // Diamonds Refined
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("DIAMONDS REFINED", color = Color(0xFF8A82A0), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFF22D3EE), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${viewModel.diamondsThisRun}", color = Color(0xFF22D3EE), fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.testTag("game_over_earned_text"))
                }
            }

            Divider(color = Color(0xFF1E293B), thickness = 1.dp)

            // High Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("PERSONAL HIGH SCORE", color = Color(0xFF8A82A0), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(stats.highScore.toString(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
        }

        // CONTROL BUTTONS
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Run Again with gradient
            Button(
                onClick = { viewModel.startGame() },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(58.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF22D3EE), Color(0xFF2563EB))
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .testTag("run_again_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RE-ACTIVATE RUN", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.5.sp)
                }
            }

            // Return Main Menu with cyan outline
            OutlinedButton(
                onClick = { viewModel.setScreen(ScreenState.MAIN_MENU) },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(54.dp)
                    .testTag("menu_button"),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, Color(0xFF22D3EE)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF22D3EE))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = Color(0xFF22D3EE))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("MAIN DASHBOARD", fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// UPGRADES / STORE TERMINAL SCREEN
// -----------------------------------------------------------------------------
@Composable
fun UpgradesScreen(viewModel: GameViewModel, stats: com.example.data.PlayerStats) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                // Elegant diagonal cyan grids
                val color = Color(0xFF22D3EE).copy(alpha = 0.05f)
                val spacing = 45.dp.toPx()
                for (i in -size.height.toInt()..size.width.toInt() step spacing.toInt()) {
                    drawLine(color, Offset(i.toFloat(), 0f), Offset(i.toFloat() + size.height, size.height), strokeWidth = 1f)
                }
            }
            .padding(20.dp)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP WALLET HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.setScreen(ScreenState.MAIN_MENU) },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0xFF22D3EE).copy(alpha = 0.4f), CircleShape)
                    .testTag("upgrades_back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF22D3EE))
            }

            Text(
                text = "UPGRADE TERMINAL",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )

            // Wallet (slate-900 with cyan border)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0xFF22D3EE).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFF22D3EE), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stats.diamonds.toString(),
                    color = Color(0xFF22D3EE),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.testTag("upgrade_diamonds_text")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // LIST OF UPGRADE CARDS
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. SPEED BOOSTER
            UpgradeCard(
                name = "SPEED BOOST",
                desc = "Increases hyper-speed warp powerup duration.",
                icon = Icons.Default.PlayArrow,
                color = Color(0xFFFF007F),
                currentLvl = stats.speedBoostLevel,
                cost = stats.getUpgradeCost(stats.speedBoostLevel),
                walletDiamonds = stats.diamonds,
                onUpgrade = { viewModel.purchaseUpgrade(PowerUpType.SPEED_BOOST) },
                testTagPrefix = "speed_boost"
            )

            // 2. DIAMOND MAGNET
            UpgradeCard(
                name = "DIAMOND MAGNET",
                desc = "Extends space-gold diamond vacuum pull duration.",
                icon = Icons.Default.Star,
                color = Color(0xFF00FFCC),
                currentLvl = stats.magnetLevel,
                cost = stats.getUpgradeCost(stats.magnetLevel),
                walletDiamonds = stats.diamonds,
                onUpgrade = { viewModel.purchaseUpgrade(PowerUpType.MAGNET) },
                testTagPrefix = "magnet"
            )

            // 3. REFINERY SHIELD
            UpgradeCard(
                name = "REFINERY SHIELD",
                desc = "Boosts protective forcefield durability duration.",
                icon = Icons.Default.Check,
                color = Color(0xFF00BFFF),
                currentLvl = stats.shieldLevel,
                cost = stats.getUpgradeCost(stats.shieldLevel),
                walletDiamonds = stats.diamonds,
                onUpgrade = { viewModel.purchaseUpgrade(PowerUpType.SHIELD) },
                testTagPrefix = "shield"
            )
        }

        // FOOTER INFO
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "DIAMONDS EARNED DURING GAME RUNS ARE PRESERVED HERE",
                color = Color(0xFF8A82A0),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun UpgradeCard(
    name: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    currentLvl: Int,
    cost: Int,
    walletDiamonds: Int,
    onUpgrade: () -> Unit,
    testTagPrefix: String
) {
    val isMax = cost == -1
    val canAfford = !isMax && walletDiamonds >= cost

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(desc, color = Color(0xFF8A82A0), fontSize = 10.sp, lineHeight = 14.sp)

            Spacer(modifier = Modifier.height(10.dp))

            // Level Dots Indicator e.g. [■ ■ ■ □ □]
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (lvl in 1..5) {
                    val dotColor = if (lvl <= currentLvl) color else Color(0xFF1E293B)
                    Box(
                        modifier = Modifier
                            .size(width = 16.dp, height = 6.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(dotColor)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Action Button (Immersive UI: color when active, slate-800 when disabled)
        Button(
            onClick = onUpgrade,
            enabled = canAfford,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = color,
                disabledContainerColor = Color(0xFF1E293B)
            ),
            modifier = Modifier
                .width(105.dp)
                .height(48.dp)
                .testTag("${testTagPrefix}_upgrade_button"),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isMax) {
                    Text("MAX LVL", color = Color(0xFF8A82A0), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text(
                        text = "UPGRADE",
                        color = if (canAfford) Color.White else Color(0xFF8A82A0),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = if (canAfford) Color.White else Color(0xFF8A82A0),
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = cost.toString(),
                            color = if (canAfford) Color.White else Color(0xFF8A82A0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}
