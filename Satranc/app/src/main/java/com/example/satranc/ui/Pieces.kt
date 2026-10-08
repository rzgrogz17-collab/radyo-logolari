package com.example.satranc.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.satranc.chess.Chess

@Composable
fun ChessGlyph(piece: Int, style: PieceStyle, modifier: Modifier = Modifier) {
    if (piece == 0) return
    val colors = palette(style)
    val light = Chess.isWhite(piece)
    val fill = if (light) colors.light else colors.dark
    val rim = if (light) colors.lightRim else colors.darkRim
    val kind = Chess.typeOf(piece)
    Canvas(modifier.fillMaxSize().padding(1.dp)) {
        val w = size.width
        val h = size.height
        drawOval(
            color = Color.Black.copy(alpha = 0.28f),
            topLeft = Offset(w * 0.18f, h * 0.80f),
            size = Size(w * 0.64f, h * 0.10f)
        )
        when (kind) {
            Chess.WP -> pawn(fill, rim)
            Chess.WR -> rook(fill, rim)
            Chess.WN -> knight(fill, rim, light)
            Chess.WB -> bishop(fill, rim)
            Chess.WQ -> queen(fill, rim)
            Chess.WK -> king(fill, rim)
        }
    }
}

private fun DrawScope.base(fill: Color, rim: Color) {
    val w = size.width
    val h = size.height
    drawOval(rim, Offset(w * 0.16f, h * 0.72f), Size(w * 0.68f, h * 0.16f))
    drawOval(fill, Offset(w * 0.22f, h * 0.75f), Size(w * 0.56f, h * 0.10f))
}

private fun DrawScope.pawn(fill: Color, rim: Color) {
    base(fill, rim)
    val w = size.width
    val h = size.height
    val stem = Path().apply {
        moveTo(w * 0.40f, h * 0.62f)
        lineTo(w * 0.60f, h * 0.62f)
        lineTo(w * 0.54f, h * 0.74f)
        lineTo(w * 0.46f, h * 0.74f)
        close()
    }
    drawPath(stem, fill)
    drawPath(stem, rim, style = Stroke(w * 0.035f))
    drawCircle(fill, w * 0.16f, Offset(w * 0.50f, h * 0.40f))
    drawCircle(rim, w * 0.16f, Offset(w * 0.50f, h * 0.40f), style = Stroke(w * 0.035f))
}

private fun DrawScope.rook(fill: Color, rim: Color) {
    base(fill, rim)
    val w = size.width
    val h = size.height
    drawRoundRect(fill, Offset(w * 0.30f, h * 0.34f), Size(w * 0.40f, h * 0.40f), androidx.compose.ui.geometry.CornerRadius(w * 0.04f))
    drawRoundRect(rim, Offset(w * 0.30f, h * 0.34f), Size(w * 0.40f, h * 0.40f), androidx.compose.ui.geometry.CornerRadius(w * 0.04f), style = Stroke(w * 0.03f))
    for (i in 0..2) {
        val x = w * (0.30f + i * 0.14f)
        drawRect(fill, Offset(x, h * 0.22f), Size(w * 0.10f, h * 0.14f))
        drawRect(rim, Offset(x, h * 0.22f), Size(w * 0.10f, h * 0.14f), style = Stroke(w * 0.025f))
    }
}

private fun DrawScope.knight(fill: Color, rim: Color, light: Boolean) {
    base(fill, rim)
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(w * 0.28f, h * 0.74f)
        lineTo(w * 0.72f, h * 0.74f)
        lineTo(w * 0.66f, h * 0.60f)
        cubicTo(w * 0.86f, h * 0.52f, w * 0.78f, h * 0.30f, w * 0.58f, h * 0.34f)
        lineTo(w * 0.64f, h * 0.22f)
        lineTo(w * 0.46f, h * 0.30f)
        cubicTo(w * 0.30f, h * 0.18f, w * 0.18f, h * 0.36f, w * 0.30f, h * 0.50f)
        lineTo(w * 0.24f, h * 0.58f)
        close()
    }
    drawPath(path, fill)
    drawPath(path, rim, style = Stroke(w * 0.03f))
    drawCircle(if (light) Color(0xFF1A1D27) else Gold, w * 0.035f, Offset(w * 0.48f, h * 0.38f))
}

private fun DrawScope.bishop(fill: Color, rim: Color) {
    base(fill, rim)
    val w = size.width
    val h = size.height
    val body = Path().apply {
        moveTo(w * 0.50f, h * 0.16f)
        cubicTo(w * 0.78f, h * 0.40f, w * 0.70f, h * 0.66f, w * 0.58f, h * 0.74f)
        lineTo(w * 0.42f, h * 0.74f)
        cubicTo(w * 0.30f, h * 0.66f, w * 0.22f, h * 0.40f, w * 0.50f, h * 0.16f)
        close()
    }
    drawPath(body, fill)
    drawPath(body, rim, style = Stroke(w * 0.03f))
    drawLine(rim, Offset(w * 0.50f, h * 0.30f), Offset(w * 0.50f, h * 0.52f), w * 0.03f)
    drawCircle(fill, w * 0.05f, Offset(w * 0.50f, h * 0.16f))
    drawCircle(rim, w * 0.05f, Offset(w * 0.50f, h * 0.16f), style = Stroke(w * 0.02f))
}

private fun DrawScope.queen(fill: Color, rim: Color) {
    base(fill, rim)
    val w = size.width
    val h = size.height
    val crown = Path().apply {
        moveTo(w * 0.24f, h * 0.62f)
        lineTo(w * 0.30f, h * 0.34f)
        lineTo(w * 0.40f, h * 0.50f)
        lineTo(w * 0.50f, h * 0.26f)
        lineTo(w * 0.60f, h * 0.50f)
        lineTo(w * 0.70f, h * 0.34f)
        lineTo(w * 0.76f, h * 0.62f)
        close()
    }
    drawPath(crown, fill)
    drawPath(crown, rim, style = Stroke(w * 0.028f))
    for (x in floatArrayOf(0.30f, 0.50f, 0.70f)) {
        drawCircle(rim, w * 0.045f, Offset(w * x, h * if (x == 0.50f) 0.24f else 0.32f))
    }
}

private fun DrawScope.king(fill: Color, rim: Color) {
    queen(fill, rim)
    val w = size.width
    val h = size.height
    drawRect(rim, Offset(w * 0.47f, h * 0.08f), Size(w * 0.06f, h * 0.16f))
    drawRect(rim, Offset(w * 0.42f, h * 0.12f), Size(w * 0.16f, h * 0.055f))
}
