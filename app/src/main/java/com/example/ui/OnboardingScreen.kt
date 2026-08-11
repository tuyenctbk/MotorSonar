package com.example.ui

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*

data class OnboardingStep(
    val title: String,
    val description: String,
    val badgeText: String,
    val iconColor: Color,
    val illustration: @Composable () -> Unit
)

@Composable
fun AcousticCaptureOnboarding(
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    var currentStepIdx by remember { mutableStateOf(0) }

    val steps = listOf(
        OnboardingStep(
            title = "1. Safety & Preparation",
            description = "Ensure your vehicle is in Park or Neutral with the handbrake engaged. The engine must be idling at a normal operating temperature in a well-ventilated outdoor space.",
            badgeText = "VEHICLE SAFETY",
            iconColor = AlertRed,
            illustration = { SafetyIllustration() }
        ),
        OnboardingStep(
            title = "2. Maintain Optimal Distance",
            description = "Position your phone between 12 to 24 inches (30 to 60 cm) from the engine bay. Avoid touching hot surfaces or placing your hands near moving radiator fans and drive belts.",
            badgeText = "PERFECT RANGE",
            iconColor = AmberOrange,
            illustration = { DistanceIllustration() }
        ),
        OnboardingStep(
            title = "3. Microscopic Target Focus",
            description = "Point the bottom edge (main microphone) of your phone directly toward the center of the engine block. Remove thick protective covers to avoid muffled signal captures.",
            badgeText = "MIC ALIGNMENT",
            iconColor = SafeGreen,
            illustration = { AlignmentIllustration() }
        ),
        OnboardingStep(
            title = "4. Isolate Ambient Noise",
            description = "Minimize talking, wind interference, and secondary garage echoing during recording. Do not test near heavy traffic or loud industrial surroundings for an accurate diagnostic read.",
            badgeText = "NOISE FILTERING",
            iconColor = WarnYellow,
            illustration = { NoiseIllustration() }
        )
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 24.dp)
                .testTag("onboarding_dialog_container"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Acoustic Scan Wizard",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable { onDismiss() }
                    ) {
                        Text(
                            text = "Skip Guide",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    steps[currentStepIdx].illustration()
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        color = steps[currentStepIdx].iconColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = steps[currentStepIdx].badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = steps[currentStepIdx].iconColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = steps[currentStepIdx].title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = steps[currentStepIdx].description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier
                            .heightIn(min = 72.dp)
                            .padding(horizontal = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    steps.forEachIndexed { idx, _ ->
                        val isCurrent = idx == currentStepIdx
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (isCurrent) 10.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStepIdx > 0) {
                        OutlinedButton(
                            onClick = { currentStepIdx-- },
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("onboarding_back_button")
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Back", fontSize = 13.sp)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(80.dp))
                    }

                    val isLast = currentStepIdx == steps.size - 1
                    Button(
                        onClick = {
                            if (isLast) {
                                onComplete()
                            } else {
                                currentStepIdx++
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLast) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .testTag(if (isLast) "onboarding_finish_button" else "onboarding_next_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isLast) "Let's Diagnose" else "Next Step",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (isLast) Icons.Default.CheckCircle else Icons.Default.ArrowForward,
                                contentDescription = if (isLast) "Complete" else "Next",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SafetyIllustration() {
    val containerBg = MaterialTheme.colorScheme.surfaceContainer
    val borderCol = MaterialTheme.colorScheme.outlineVariant
    val textCol = MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .size(140.dp)
            .background(containerBg, RoundedCornerShape(16.dp))
            .border(1.dp, borderCol, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f

            drawCircle(
                color = AlertRed.copy(alpha = 0.08f),
                radius = 50.dp.toPx(),
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = AlertRed.copy(alpha = 0.15f),
                radius = 35.dp.toPx(),
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = AlertRed,
                radius = 24.dp.toPx(),
                center = Offset(centerX, centerY),
                style = Stroke(width = 3.dp.toPx())
            )
        }
        Text(
            text = "P",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = textCol
        )
    }
}

@Composable
fun DistanceIllustration() {
    val containerBg = MaterialTheme.colorScheme.surfaceContainer
    val borderCol = MaterialTheme.colorScheme.outlineVariant
    val textCol = MaterialTheme.colorScheme.onSurface
    val primaryCol = MaterialTheme.colorScheme.primary
    val sleekCol = MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier = Modifier
            .size(140.dp)
            .background(containerBg, RoundedCornerShape(16.dp))
            .border(1.dp, borderCol, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerY = size.height / 2f
            val width = size.width

            drawRoundRect(
                color = sleekCol,
                topLeft = Offset(12.dp.toPx(), centerY - 30.dp.toPx()),
                size = Size(30.dp.toPx(), 60.dp.toPx()),
                cornerRadius = CornerRadius(6.dp.toPx())
            )
            drawCircle(
                color = primaryCol.copy(alpha = 0.6f),
                radius = 9.dp.toPx(),
                center = Offset(27.dp.toPx(), centerY)
            )

            val phoneWidth = 20.dp.toPx()
            val phoneHeight = 36.dp.toPx()
            drawRoundRect(
                color = SteelGrey,
                topLeft = Offset(width - 32.dp.toPx() - phoneWidth, centerY - phoneHeight / 2),
                size = Size(phoneWidth, phoneHeight),
                cornerRadius = CornerRadius(3.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )

            val rulerY = centerY
            val startX = 65.dp.toPx()
            val endX = width - 60.dp.toPx()

            drawLine(
                color = primaryCol,
                start = Offset(startX, rulerY),
                end = Offset(endX, rulerY),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            )
        }
        Text(
            text = "12\" - 24\"",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textCol
        )
    }
}

@Composable
fun AlignmentIllustration() {
    val containerBg = MaterialTheme.colorScheme.surfaceContainer
    val borderCol = MaterialTheme.colorScheme.outlineVariant
    val sleekCol = MaterialTheme.colorScheme.surfaceVariant
    val phoneCol = MaterialTheme.colorScheme.onSurface
    Canvas(
        modifier = Modifier
            .size(140.dp)
            .background(containerBg, RoundedCornerShape(16.dp))
            .border(1.dp, borderCol, RoundedCornerShape(16.dp))
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        drawCircle(
            color = sleekCol,
            radius = 28.dp.toPx(),
            center = Offset(centerX, 35.dp.toPx())
        )
        drawLine(
            color = SafeGreen.copy(alpha = 0.7f),
            start = Offset(centerX - 28.dp.toPx(), 35.dp.toPx()),
            end = Offset(centerX + 28.dp.toPx(), 35.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = SafeGreen.copy(alpha = 0.7f),
            start = Offset(centerX, 10.dp.toPx()),
            end = Offset(centerX, 60.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )

        val phoneWidth = 32.dp.toPx()
        val phoneHeight = 18.dp.toPx()
        val phoneY = centerY + 25.dp.toPx()

        drawRoundRect(
            color = phoneCol,
            topLeft = Offset(centerX - phoneWidth / 2f, phoneY),
            size = Size(phoneWidth, phoneHeight),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )
        drawCircle(
            color = SafeGreen,
            radius = 3.dp.toPx(),
            center = Offset(centerX, phoneY)
        )
    }
}

@Composable
fun NoiseIllustration() {
    val containerBg = MaterialTheme.colorScheme.surfaceContainer
    val borderCol = MaterialTheme.colorScheme.outlineVariant
    val micCol = MaterialTheme.colorScheme.onSurface
    Canvas(
        modifier = Modifier
            .size(140.dp)
            .background(containerBg, RoundedCornerShape(16.dp))
            .border(1.dp, borderCol, RoundedCornerShape(16.dp))
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        for (i in 1..3) {
            drawCircle(
                color = WarnYellow.copy(alpha = 0.12f / i),
                radius = (16 * i).dp.toPx(),
                center = Offset(centerX, centerY)
            )
        }

        drawRoundRect(
            color = micCol,
            topLeft = Offset(centerX - 6.dp.toPx(), centerY - 12.dp.toPx()),
            size = Size(12.dp.toPx(), 24.dp.toPx()),
            cornerRadius = CornerRadius(6.dp.toPx())
        )
        drawLine(
            color = micCol,
            start = Offset(centerX, centerY + 12.dp.toPx()),
            end = Offset(centerX, centerY + 20.dp.toPx()),
            strokeWidth = 2.dp.toPx()
        )

        drawArc(
            color = SafeGreen,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(centerX - 25.dp.toPx(), centerY - 25.dp.toPx()),
            size = Size(50.dp.toPx(), 50.dp.toPx()),
            style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f))
        )
    }
}

object OnboardingPrefs {
    private const val PREFS_NAME = "motosonar_prefs"
    private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

    fun isOnboardingCompleted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(context: Context, completed: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }
}
