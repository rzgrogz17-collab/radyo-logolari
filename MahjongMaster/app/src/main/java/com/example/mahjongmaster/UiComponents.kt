package com.example.mahjongmaster

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

internal fun formatScore(value: Long): String {
    val n = kotlin.math.abs(value)
    return when {
        n >= 1_000_000 -> String.format("%.1fM", n / 1_000_000.0)
        n >= 10_000 -> String.format("%.1fk", n / 1_000.0)
        else -> java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(value)
    }
}

internal fun formatTime(totalSeconds: Int): String {
    val s = totalSeconds.coerceAtLeast(0)
    val m = s / 60
    val r = s % 60
    return "%d:%02d".format(m, r)
}

@Composable
fun MahjongBrandTitle(
    bigFontSize: TextUnit,
    smallFontSize: TextUnit,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    bigLineHeight: TextUnit = TextUnit.Unspecified,
    smallLineHeight: TextUnit = TextUnit.Unspecified,
    subtitle: String = "MASTER"
) {
    val goldBrush = Brush.linearGradient(
        listOf(GameColors.goldLight, GameColors.gold, GameColors.goldDeep)
    )
    Column(horizontalAlignment = horizontalAlignment) {
        EmbossedGoldText("MAHJONG", bigFontSize, goldBrush, bigLineHeight, deep = true)
        Spacer(Modifier.height(2.dp))
        EmbossedGoldText(subtitle, smallFontSize, goldBrush, smallLineHeight, deep = false)
    }
}

@Composable
private fun EmbossedGoldText(
    text: String,
    size: TextUnit,
    brush: Brush,
    lineHeight: TextUnit,
    deep: Boolean
) {
    val weight = if (deep) FontWeight.Black else FontWeight.Bold
    Box {
        Text(
            text, color = Color(0xFF4E2E00), fontSize = size, fontWeight = weight,
            lineHeight = lineHeight, modifier = Modifier.offset(if (deep) 3.dp else 2.dp, if (deep) 4.dp else 2.5.dp)
        )
        if (deep) {
            Text(
                text, color = Color(0xFF7A4A00), fontSize = size, fontWeight = weight,
                lineHeight = lineHeight, modifier = Modifier.offset(1.5.dp, 2.dp)
            )
        }
        Text(
            text, fontSize = size, fontWeight = weight, lineHeight = lineHeight,
            style = TextStyle(brush = brush, shadow = Shadow(Color.Black.copy(alpha = 0.55f), Offset(2f, 2f), 5f))
        )
    }
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    theme: BoardTheme,
    corner: Dp = 22.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(corner)
    Column(
        modifier
            .shadow(16.dp, shape, ambientColor = Color.Black.copy(alpha = 0.4f), spotColor = Color.Black.copy(alpha = 0.4f))
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(theme.panelTop.copy(alpha = 0.78f), theme.panelBottom.copy(alpha = 0.94f))
                )
            )
            .border(
                1.2.dp,
                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.5f), theme.accent.copy(alpha = 0.28f))),
                shape
            )
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
fun StatChip(title: String, value: String, modifier: Modifier = Modifier, accent: Color = Color.White) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .shadow(8.dp, shape, ambientColor = Color.Black.copy(alpha = 0.45f), spotColor = Color.Black.copy(alpha = 0.45f))
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.06f))))
            .border(1.dp, Color.White.copy(alpha = 0.28f), shape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            title, color = Color.White.copy(alpha = 0.78f), fontSize = 9.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp
        )
        Text(
            value, color = accent, fontSize = 15.sp, fontWeight = FontWeight.Black,
            style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.5f), Offset(1f, 1f), 2f))
        )
    }
}

@Composable
fun RowScope.DockButton(
    text: String,
    icon: ImageVector,
    mainColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    badgeCount: Int? = null,
    shake: Boolean = false
) {
    val infinite = rememberInfiniteTransition(label = "shake")
    val shakeOffset by infinite.animateFloat(
        -3f, 3f,
        infiniteRepeatable(tween(80, easing = LinearEasing), RepeatMode.Reverse),
        label = "shakeOffset"
    )
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val glassShape = RoundedCornerShape(19.dp)
    Box(
        modifier
            .weight(1f)
            .aspectRatio(1f)
            .offset(x = if (shake) shakeOffset.dp else 0.dp)
            .scale(if (pressed && enabled) 0.94f else 1f)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .alpha(if (enabled) 1f else 0.4f)
                .shadow(
                    if (shake) 16.dp else 9.dp, glassShape,
                    ambientColor = mainColor.copy(alpha = 0.55f),
                    spotColor = mainColor.copy(alpha = 0.65f)
                )
                .clip(glassShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.22f),
                            mainColor.copy(alpha = 0.32f),
                            mainColor.copy(alpha = 0.14f)
                        )
                    )
                )
                .border(
                    if (shake) 1.8.dp else 1.3.dp,
                    Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.75f), mainColor.copy(alpha = 0.35f), Color.White.copy(alpha = 0.18f))
                    ),
                    glassShape
                )
                .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp).padding(bottom = 2.dp))
                Text(
                    text, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                    style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(1f, 1f), 2f))
                )
            }
        }
        if (badgeCount != null) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 2.dp)
                    .size(18.dp)
                    .shadow(4.dp, CircleShape)
                    .background(if (badgeCount > 0) mainColor else Color(0xFF555555), CircleShape)
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("$badgeCount", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun FancyDialog(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    message: String,
    confirmText: String,
    confirmColor: Color,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    theme: BoardTheme = BoardTheme.JADE
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(theme = theme, modifier = Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .size(64.dp)
                    .shadow(10.dp, CircleShape, ambientColor = iconColor, spotColor = iconColor)
                    .background(iconColor.copy(alpha = 0.22f), CircleShape)
                    .border(1.5.dp, iconColor.copy(alpha = 0.8f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(1f, 1f), 3f))
            )
            Spacer(Modifier.height(10.dp))
            Text(message, color = Color(0xFFE0E6E4), fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.4f))
                ) {
                    Text(dismissText, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .shadow(8.dp, RoundedCornerShape(12.dp), ambientColor = confirmColor, spotColor = confirmColor),
                    colors = ButtonDefaults.buttonColors(containerColor = confirmColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(confirmText, color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun PrimaryCta(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    val shape = RoundedCornerShape(16.dp)
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = color, disabledContainerColor = color.copy(alpha = 0.4f)),
        shape = shape,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(10.dp, shape, ambientColor = color.copy(alpha = 0.7f), spotColor = color.copy(alpha = 0.7f))
    ) {
        if (icon != null) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color.White)
    }
}

@Composable
fun ComboBadge(streak: Int, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = streak >= 2,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val pulse = rememberInfiniteTransition(label = "combo")
        val glow by pulse.animateFloat(
            0.55f, 1f,
            infiniteRepeatable(tween(520, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "comboGlow"
        )
        val shape = RoundedCornerShape(20.dp)
        Text(
            "×$streak",
            color = GameColors.goldLight,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .shadow(12.dp, shape, ambientColor = GameColors.gold.copy(alpha = 0.7f * glow), spotColor = GameColors.gold)
                .clip(shape)
                .background(Brush.horizontalGradient(listOf(Color(0xCC7A4A00), Color(0xCC4E2E00))))
                .border(1.2.dp, GameColors.gold.copy(alpha = glow), shape)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(1f, 1f), 3f))
        )
    }
}

@Composable
fun PairProgressBar(remaining: Int, total: Int, accent: Color, modifier: Modifier = Modifier) {
    val fraction = if (total <= 0) 0f else ((total - remaining).toFloat() / total).coerceIn(0f, 1f)
    val track = RoundedCornerShape(99.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(track)
            .background(Color.White.copy(alpha = 0.14f))
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .height(6.dp)
                .clip(track)
                .background(Brush.horizontalGradient(listOf(accent, GameColors.goldLight)))
        )
    }
}

@Composable
fun DeadlockBanner(text: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier
            .clip(shape)
            .background(Color(0xE6C62828))
            .border(1.dp, Color.White.copy(alpha = 0.35f), shape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun SettingRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            if (subtitle != null) {
                Text(subtitle, color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
            }
        }
        val track = RoundedCornerShape(99.dp)
        Box(
            Modifier
                .size(48.dp, 28.dp)
                .clip(track)
                .background(if (checked) Color(0xFF43A047) else Color.White.copy(alpha = 0.22f))
                .padding(3.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(Modifier.size(22.dp).shadow(3.dp, CircleShape).background(Color.White, CircleShape))
        }
    }
}

@Composable
fun GoldDialog(
    onDismiss: () -> Unit,
    theme: BoardTheme,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        GlassPanel(
            theme = theme,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
        ) { content() }
    }
}
