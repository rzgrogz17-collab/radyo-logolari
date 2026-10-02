package com.example.satranc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.satranc.chess.Chess

@Composable
fun ChessBoard(
    board: IntArray,
    theme: BoardTheme,
    style: PieceStyle,
    flipped: Boolean,
    showCoords: Boolean,
    showDots: Boolean,
    selected: Int?,
    targets: Set<Int>,
    lastFrom: Int,
    lastTo: Int,
    hintFrom: Int,
    hintTo: Int,
    checkSquare: Int,
    onTap: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(theme.frame)
            .padding(if (showCoords) 14.dp else 8.dp)
    ) {
        Column(Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))) {
            for (row in 0 until 8) {
                val rank = if (flipped) row else 7 - row
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    for (col in 0 until 8) {
                        val file = if (flipped) 7 - col else col
                        val sq = Chess.square(file, rank)
                        val light = (file + rank) % 2 != 0
                        var color = if (light) theme.light else theme.dark
                        if (sq == lastFrom || sq == lastTo) color = blend(color, theme.last, 0.55f)
                        if (sq == hintFrom || sq == hintTo) color = blend(color, Gold, 0.45f)
                        if (sq == selected) color = blend(color, theme.select, 0.50f)
                        if (sq == checkSquare) color = blend(color, Danger, 0.55f)
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(color)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onTap(sq) }
                        ) {
                            val piece = board.getOrNull(sq) ?: 0
                            val target = sq in targets
                            if (showDots && target && piece == 0) {
                                Box(
                                    Modifier
                                        .align(Alignment.Center)
                                        .fillMaxSize(0.28f)
                                        .clip(RoundedCornerShape(50))
                                        .background(theme.dot.copy(alpha = 0.55f))
                                )
                            }
                            if (piece != 0) {
                                ChessGlyph(piece, style, Modifier.fillMaxSize().padding(2.dp))
                            }
                            if (showDots && target && piece != 0) {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .padding(3.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.Transparent)
                                        .align(Alignment.Center)
                                )
                                Box(
                                    Modifier
                                        .align(Alignment.Center)
                                        .fillMaxSize()
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Danger.copy(alpha = 0.28f))
                                )
                            }
                            val labelColor = if (light) theme.dark.copy(alpha = 0.85f) else theme.light.copy(alpha = 0.9f)
                            if (showCoords && col == 0) {
                                Text(
                                    "${rank + 1}",
                                    color = labelColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.TopStart).padding(start = 2.dp)
                                )
                            }
                            if (showCoords && row == 7) {
                                Text(
                                    "${'a' + file}",
                                    color = labelColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 2.dp, bottom = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun blend(base: Color, tint: Color, amount: Float): Color {
    val inv = 1f - amount
    return Color(
        red = base.red * inv + tint.red * amount,
        green = base.green * inv + tint.green * amount,
        blue = base.blue * inv + tint.blue * amount,
        alpha = 1f
    )
}

private fun Modifier.fillMaxSize(fraction: Float): Modifier = this.then(
    Modifier.fillMaxSize().padding(((1f - fraction) * 18).dp)
)
