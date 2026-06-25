package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlin.random.Random

data class Particle(
    val id: Int,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    val size: Float,
    var rotation: Float,
    val rotSpeed: Float,
    val shapeType: Int // 0 = Rectangle, 1 = Circle, 2 = Triangle
)

@Composable
fun ConfettiCelebration(
    trigger: Int,
    onFinished: () -> Unit = {}
) {
    if (trigger == 0) return

    val configuration = LocalConfiguration.current
    val screenWidthPx = configuration.screenWidthDp * 3f // Approximate scale factor to pixels
    val screenHeightPx = configuration.screenHeightDp * 3f

    // Spawning 60 particles with random properties
    var particles by remember(trigger) {
        val list = List(65) { index ->
            val angle = Random.nextFloat() * Math.PI.toFloat()
            val speed = Random.nextFloat() * 15f + 5f
            Particle(
                id = index,
                // Spawn in random horizontal spread near the top
                x = Random.nextFloat() * screenWidthPx,
                y = -50f - (Random.nextFloat() * 300f), // staggered entry
                vx = (Random.nextFloat() * 6f - 3f), // slight drift
                vy = speed,
                color = Color(
                    red = Random.nextFloat() * 0.6f + 0.4f, // bright colors
                    green = Random.nextFloat() * 0.6f + 0.4f,
                    blue = Random.nextFloat() * 0.6f + 0.4f,
                    alpha = 1f
                ),
                size = Random.nextFloat() * 16f + 12f,
                rotation = Random.nextFloat() * 360f,
                rotSpeed = (Random.nextFloat() * 8f - 4f),
                shapeType = Random.nextInt(3)
            )
        }
        mutableStateOf(list)
    }

    // Game loop running at ~60fps
    LaunchedEffect(trigger) {
        val startTime = System.currentTimeMillis()
        // Run animation for 3.5 seconds
        while (System.currentTimeMillis() - startTime < 3500) {
            delay(16)
            particles = particles.map { p ->
                val nextVy = p.vy + 0.25f // Gravity acceleration
                val nextVx = p.vx + (Random.nextFloat() * 0.4f - 0.2f) // Wind gust simulation
                p.copy(
                    x = p.x + nextVx,
                    y = p.y + nextVy,
                    vx = nextVx,
                    vy = nextVy,
                    rotation = p.rotation + p.rotSpeed
                )
            }
        }
        onFinished()
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .testTag("confetti_canvas")
    ) {
        val width = size.width
        val height = size.height

        particles.forEach { p ->
            // Keep particles inside horizontal screen boundaries (wrap around)
            val wrappedX = if (p.x < 0) {
                (p.x % width + width) % width
            } else {
                p.x % width
            }

            // Draw particle if within vertical screen boundaries
            if (p.y < height) {
                this.rotate(p.rotation, pivot = androidx.compose.ui.geometry.Offset(wrappedX + p.size/2, p.y + p.size/2)) {
                    when (p.shapeType) {
                        0 -> { // Rectangle / Ribbon
                            drawRect(
                                color = p.color,
                                topLeft = androidx.compose.ui.geometry.Offset(wrappedX, p.y),
                                size = Size(p.size, p.size / 2f)
                            )
                        }
                        1 -> { // Circle
                            drawCircle(
                                color = p.color,
                                radius = p.size / 2f,
                                center = androidx.compose.ui.geometry.Offset(wrappedX + p.size/2, p.y + p.size/2)
                            )
                        }
                        else -> { // Square
                            drawRect(
                                color = p.color,
                                topLeft = androidx.compose.ui.geometry.Offset(wrappedX, p.y),
                                size = Size(p.size, p.size)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeMotivationDialog(
    onDismiss: () -> Unit,
    onAction: () -> Unit,
    onSendNotification: () -> Unit
) {
    // Elegant pulsing and scale animations for dialog entry
    var animateStart by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        animateStart = true
    }

    val scale by animateFloatAsState(
        targetValue = if (animateStart) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    val opacity by animateFloatAsState(
        targetValue = if (animateStart) 1f else 0f,
        animationSpec = tween(durationMillis = 400)
    )

    // Pulsing glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val rotateAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .scale(scale)
                    .border(
                        BorderStroke(
                            1.5.dp,
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        ),
                        RoundedCornerShape(32.dp)
                    )
                    .testTag("welcome_motivation_dialog"),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header Visual Layer (Animated Planet / Rocket layout)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(110.dp)
                    ) {
                        // Ambient Glowing background circles
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        )

                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
                        )

                        // Outer spinning stars
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier
                                .size(32.dp)
                                .align(Alignment.TopEnd)
                                .scale(pulseScale)
                        )

                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.BottomStart)
                                .scale(pulseScale)
                        )

                        // Core Hero Icon
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(54.dp),
                            shadowElevation = 4.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.RocketLaunch,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    // Dialog Title & Greeting
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "مرحباً بك في مُبتكِر الذكي! 👋",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "تذكير الأهداف اليومية والمشاريع 🎯",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Motivation Body Text
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "حان الوقت للمرور على أهدافك ومشاريعك اليومية ومتابعة خطواتك البرمجية قيد التنفيذ اليوم لتواصل تميزك البرمجي.",
                                fontSize = 13.sp,
                                lineHeight = 21.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                            Text(
                                text = "💡 \"الخطوات الصغيرة والمنظمة تقود دائمًا إلى إنجازات برمجية عظيمة! أبداً لا تقلل من قيمة فكرة اليوم.\"",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary,
                                lineHeight = 18.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Animated Action Buttons
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = onAction,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("motivation_dialog_review_btn"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "مراجعة مشاريعي قيد التنفيذ 🚀",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        TextButton(
                            onClick = onSendNotification,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("motivation_dialog_notif_btn"),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "إرسال إشعار خارجي للتذكير 🔔",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = "إغلاق مؤقت",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}
