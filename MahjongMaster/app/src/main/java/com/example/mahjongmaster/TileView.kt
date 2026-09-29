package com.example.mahjongmaster

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private const val TILE_ASPECT = 1.37f
private const val DEPTH_RATIO = 0.16f

/** Tahta ölçüleri — taş boyu, oyun alanının GERÇEK ölçülen boyutuna sığacak şekilde hesaplanır. */
@Immutable
internal data class BoardMetrics(
    val tileW: Dp,
    val tileH: Dp,
    val depth: Dp,
    val centerX: Float,
    val centerY: Float,
    val shiftX: Float,
    val shiftY: Float
)

internal fun computeBoardMetrics(tiles: List<Tile>, availW: Dp, availH: Dp, maxTileW: Dp): BoardMetrics {
    if (tiles.isEmpty()) return BoardMetrics(56.dp, (56 * TILE_ASPECT).dp, (56 * DEPTH_RATIO).dp, 5f, 5f, 0f, 0f)
    val minX = tiles.minOf { it.gridX }
    val maxX = tiles.maxOf { it.gridX }
    val minY = tiles.minOf { it.gridY }
    val maxY = tiles.maxOf { it.gridY }
    val maxLayer = tiles.maxOf { it.layer }

    val unitsW = (maxX - minX) * 0.5f + 1f + maxLayer * 0.12f + DEPTH_RATIO * 0.55f
    val unitsH = ((maxY - minY) * 0.5f * 0.85f + 1f + maxLayer * 0.20f) * TILE_ASPECT + DEPTH_RATIO * 1.35f
    val byW = (availW.value - maxLayer * 2f).coerceAtLeast(1f) / unitsW
    val byH = availH.value.coerceAtLeast(1f) / unitsH
    val tw = min(byW, byH).coerceIn(18f, maxTileW.value)
    val th = tw * TILE_ASPECT
    // Üst katmanlar sağa-yukarı kaydığı için görsel merkez de kayar — telafi edilir.
    val shiftX = -(maxLayer * (tw * 0.12f + 2f)) / 2f
    val shiftY = (maxLayer * th * 0.20f) / 2f
    return BoardMetrics(
        tw.dp, th.dp, (tw * DEPTH_RATIO).dp,
        (minX + maxX) / 2f, (minY + maxY) / 2f, shiftX, shiftY
    )
}

/**
 * Bir taşın tahta merkezine göre dp cinsinden konumu. Önceki sürümdeki
 * tileBoardOffset formülünün aynısıdır (+ merkez telafisi); hem taşın kendisi
 * hem de eşleşen çiftin "buluşma noktası" bu TEK fonksiyonla hesaplanır.
 */
internal fun tilePosition(tile: Tile, m: BoardMetrics): Offset {
    val tw = m.tileW.value
    val th = m.tileH.value
    val stepX = tw / 2f
    val stepY = th / 2f
    val layerLiftY = th * 0.20f
    val layerLiftX = tw * 0.12f
    val x = tile.gridX * stepX - m.centerX * stepX + tile.layer * layerLiftX + tile.layer * 2f + m.shiftX
    val y = tile.gridY * (stepY * 0.85f) - tile.layer * layerLiftY - m.centerY * stepY * 0.85f + m.shiftY
    return Offset(x, y)
}

@Composable
internal fun TileGlyph(type: String, style: TileStyle, modifier: Modifier, colorFilter: ColorFilter? = null) {
    if (type == "Haku") return
    val context = LocalContext.current
    val request = remember(type, style) {
        ImageRequest.Builder(context)
            .data(style.assetPath(type))
            .decoderFactory(SvgDecoder.Factory())
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = type,
        colorFilter = colorFilter,
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

/**
 * Taşın 3D gövdesini tek bir çizim geçişinde çizer: gölge, renkli sırt,
 * fildişi katman, yüz, parlama; ayrıca kilitli/seçili/ipucu/eşleşebilir
 * durum katmanları. [pulse] sadece çizim aşamasında okunur (yeniden
 * kompozisyon tetiklemez).
 */
internal fun Modifier.drawTile(
    style: TileStyle,
    theme: BoardTheme,
    faceHeight: Dp,
    depth: Dp,
    isHaku: Boolean,
    dim: Float = 0f,
    selected: Boolean = false,
    hinted: Boolean = false,
    burst: (() -> Float)? = null
): Modifier = drawWithCache {
    val w = size.width
    val faceH = faceHeight.toPx()
    val d = depth.toPx()
    val radius = CornerRadius(w * 0.14f, w * 0.14f)
    val faceSize = Size(w, faceH)
    val faceBrush = Brush.verticalGradient(listOf(style.faceTop, style.faceBottom), 0f, faceH)
    val backBrush = Brush.verticalGradient(
        listOf(theme.tileBack, theme.tileBackDark), faceH * 0.6f + d, faceH + d
    )
    val glossBrush = Brush.verticalGradient(
        listOf(Color.White.copy(alpha = 0.42f), Color.White.copy(alpha = 0f)), 0f, faceH * 0.38f
    )
    val hairline = 1.dp.toPx()
    val borderStroke = Stroke(hairline)
    val glowSoft = Stroke(6.dp.toPx())
    val selectStroke = Stroke(3.dp.toPx())
    val hintStroke = Stroke(3.5.dp.toPx())
    val hakuStroke = Stroke(w * 0.065f)
    val hakuInner = Stroke(w * 0.022f)
    val center = Offset(w / 2f, faceH / 2f)
    onDrawWithContent {
        drawRoundRect(Color.Black.copy(alpha = 0.16f), Offset(d * 0.55f, d * 1.35f), faceSize, radius)
        drawRoundRect(Color.Black.copy(alpha = 0.22f), Offset(d * 0.25f, d * 1.1f), faceSize, radius)
        drawRoundRect(backBrush, Offset(0f, d), faceSize, radius)
        drawRoundRect(style.body, Offset(0f, d * 0.45f), faceSize, radius)
        drawRoundRect(faceBrush, Offset.Zero, faceSize, radius)
        drawRoundRect(style.faceBorder, Offset.Zero, faceSize, radius, style = borderStroke)
        if (isHaku) {
            val inset = w * 0.2f
            drawRoundRect(
                style.hakuFrame, Offset(inset, faceH * 0.2f), Size(w - 2 * inset, faceH * 0.6f),
                CornerRadius(w * 0.05f), style = hakuStroke
            )
            val inset2 = inset + w * 0.1f
            drawRoundRect(
                style.hakuFrame.copy(alpha = 0.6f), Offset(inset2, faceH * 0.2f + w * 0.1f),
                Size(w - 2 * inset2, faceH * 0.6f - 2 * w * 0.1f), CornerRadius(w * 0.03f), style = hakuInner
            )
        }
        drawContent()
        drawRoundRect(glossBrush, Offset.Zero, Size(w, faceH * 0.38f), radius)
        drawRoundRect(
            Color.White.copy(alpha = 0.35f), Offset(hairline, hairline),
            Size(w - 2 * hairline, faceH - 2 * hairline), radius, style = borderStroke
        )
        if (dim > 0f) {
            drawRoundRect(Color.Black.copy(alpha = GameTuning.LOCKED_TILE_DIM * dim), Offset.Zero, faceSize, radius)
            drawRoundRect(Color.White.copy(alpha = GameTuning.LOCKED_TILE_FROST_ALPHA * dim), Offset.Zero, faceSize, radius)
            clipRect(top = faceH) {
                drawRoundRect(Color.Black.copy(alpha = GameTuning.LOCKED_TILE_DIM * 0.8f * dim), Offset(0f, d), faceSize, radius)
            }
        }
        if (selected) {
            drawRoundRect(GameColors.selectedBorder.copy(alpha = 0.16f), Offset.Zero, faceSize, radius)
            drawRoundRect(GameColors.selectedBorder.copy(alpha = 0.35f), Offset.Zero, faceSize, radius, style = glowSoft)
            drawRoundRect(GameColors.selectedBorder, Offset.Zero, faceSize, radius, style = selectStroke)
        }
        if (hinted) {
            drawRoundRect(GameColors.hintBorder.copy(alpha = 0.18f), Offset.Zero, faceSize, radius)
            drawRoundRect(GameColors.hintBorder, Offset.Zero, faceSize, radius, style = hintStroke)
        }
        if (burst != null) {
            val b = burst()
            if (b > 0f) {
                drawCircle(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFFFFF9C4).copy(alpha = 0.95f * b),
                            theme.accent.copy(alpha = 0.5f * b),
                            Color.Transparent
                        ),
                        center = center, radius = w * 1.1f
                    ),
                    radius = w * 1.1f, center = center
                )
            }
        }
    }
}

/** Menülerde / eğitimde kullanılan statik taş. */
@Composable
fun TilePreview(
    type: String,
    width: Dp,
    style: TileStyle,
    theme: BoardTheme,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
    selected: Boolean = false,
    hinted: Boolean = false
) {
    val height = width * TILE_ASPECT
    val depth = width * DEPTH_RATIO
    val filter = remember(dimmed) {
        if (dimmed) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(GameTuning.LOCKED_TILE_SATURATION) }) else null
    }
    Box(
        modifier
            .size(width, height + depth)
            .drawTile(
                style, theme, height, depth, type == "Haku",
                dim = if (dimmed) 1f else 0f, selected = selected, hinted = hinted
            )
    ) {
        TileGlyph(
            type, style,
            Modifier
                .size(width, height)
                .padding(horizontal = width * 0.08f, vertical = height * 0.08f),
            filter
        )
    }
}

internal data class ScorePopup(val id: Long, val x: Float, val y: Float, val points: Long, val streak: Int)

@Composable
internal fun MahjongBoard(
    tiles: List<Tile>,
    settings: GameSettings,
    boardEpoch: Int,
    shuffleEpoch: Int,
    events: SharedFlow<GameEvent>,
    onTileClick: (Tile) -> Unit,
    onBlockedTap: (Tile) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier.clipToBounds()) {
        val boxW = maxWidth
        val boxH = maxHeight
        val maxTile = if (boxW > 600.dp && boxH > 600.dp) 112.dp else 96.dp
        val metrics = remember(boardEpoch, tiles.size, boxW, boxH) {
            computeBoardMetrics(tiles, boxW - 12.dp, boxH - 18.dp, maxTile)
        }

        val dealStart = remember(boardEpoch) { SystemClock.uptimeMillis() }
        val latestTiles by rememberUpdatedState(tiles)
        val latestMetrics by rememberUpdatedState(metrics)
        val popups = remember { mutableStateListOf<ScorePopup>() }
        val shakes = remember { mutableStateMapOf<Int, Int>() }
        var popupSeq by remember { mutableLongStateOf(0L) }

        LaunchedEffect(events) {
            events.collect { e ->
                when (e) {
                    is GameEvent.Matched -> {
                        val a = latestTiles.firstOrNull { it.id == e.tileId1 }
                        val b = latestTiles.firstOrNull { it.id == e.tileId2 }
                        if (a != null && b != null) {
                            val m = latestMetrics
                            val pa = tilePosition(a, m)
                            val pb = tilePosition(b, m)
                            popups.add(
                                ScorePopup(
                                    popupSeq++,
                                    (pa.x + pb.x) / 2f,
                                    (pa.y + pb.y) / 2f - m.depth.value / 2f - m.tileH.value * 0.3f,
                                    e.points, e.streak
                                )
                            )
                        }
                    }
                    is GameEvent.Blocked -> shakes[e.tileId] = (shakes[e.tileId] ?: 0) + 1
                    else -> Unit
                }
            }
        }

        val matchTargets = remember(tiles, metrics) {
            val map = HashMap<Int, Offset>()
            tiles.filter { it.isVisible && it.isMatched && it.matchPairId != null }
                .groupBy { it.matchPairId!! }
                .values
                .forEach { pair ->
                    if (pair.size == 2) {
                        val a = tilePosition(pair[0], metrics)
                        val b = tilePosition(pair[1], metrics)
                        val mid = Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
                        map[pair[0].id] = mid
                        map[pair[1].id] = mid
                    }
                }
            map
        }

        Box(Modifier.fillMaxSize()) {
            tiles.forEach { tile ->
                key(boardEpoch, tile.id) {
                    if (tile.isVisible) {
                        val dealDelay = remember {
                            if (SystemClock.uptimeMillis() - dealStart < 350L) {
                                (tile.layer * 110L + tile.gridY * 16L + tile.gridX * 9L).coerceAtMost(900L)
                            } else -1L
                        }
                        BoardTile(
                            tile = tile,
                            metrics = metrics,
                            settings = settings,
                            matchTarget = matchTargets[tile.id],
                            dealDelayMs = dealDelay,
                            shuffleEpoch = shuffleEpoch,
                            shakeCount = shakes[tile.id] ?: 0,
                            onClick = { onTileClick(tile) },
                            onBlockedTap = { onBlockedTap(tile) }
                        )
                    }
                }
            }
            popups.forEach { p ->
                key(p.id) {
                    ScorePopupView(p, metrics, settings.boardTheme) { popups.remove(p) }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.BoardTile(
    tile: Tile,
    metrics: BoardMetrics,
    settings: GameSettings,
    matchTarget: Offset?,
    dealDelayMs: Long,
    shuffleEpoch: Int,
    shakeCount: Int,
    onClick: () -> Unit,
    onBlockedTap: () -> Unit
) {
    val tileW = metrics.tileW
    val tileH = metrics.tileH
    val depth = metrics.depth
    val base = remember(tile.gridX, tile.gridY, tile.layer, metrics) { tilePosition(tile, metrics) }

    // Eşleşme animasyonu — iki taş ortak buluşma noktasına havalanarak uçar,
    // orada çarpışıp parlar ve küçülerek kaybolur.
    val matchProgress = remember { Animatable(0f) }
    LaunchedEffect(tile.isMatched) {
        if (tile.isMatched) {
            matchProgress.snapTo(0f)
            matchProgress.animateTo(
                1f, tween(GameTuning.MATCH_ANIM_DURATION_MS, easing = FastOutSlowInEasing)
            )
        } else {
            matchProgress.snapTo(0f)
        }
    }

    // Dağıtma (deal) animasyonu — yeni tahtada taşlar sırayla yerine düşer.
    val appear = remember { Animatable(0f) }
    val isDealing = dealDelayMs >= 0
    LaunchedEffect(Unit) {
        if (isDealing) {
            delay(dealDelayMs)
            appear.animateTo(1f, tween(420, easing = EaseOutBack))
        } else {
            appear.animateTo(1f, tween(240, easing = FastOutSlowInEasing))
        }
    }

    // Karıştırma — taşlar dalga hâlinde dönerek yeni yüzlerini gösterir.
    val flip = remember { Animatable(1f) }
    var seenShuffle by remember { mutableIntStateOf(shuffleEpoch) }
    LaunchedEffect(shuffleEpoch) {
        if (shuffleEpoch != seenShuffle) {
            seenShuffle = shuffleEpoch
            flip.snapTo(0f)
            delay(((tile.gridX + tile.gridY) * 12L + tile.layer * 40L).coerceAtMost(420L))
            flip.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
        }
    }

    // Kapalı taşa dokunulunca kısa sallanma
    val shake = remember { Animatable(0f) }
    var seenShake by remember { mutableIntStateOf(shakeCount) }
    LaunchedEffect(shakeCount) {
        if (shakeCount != seenShake) {
            seenShake = shakeCount
            shake.snapTo(0f)
            shake.animateTo(0f, keyframes {
                durationMillis = 360
                -1f at 45
                1f at 110
                -0.7f at 175
                0.7f at 240
                -0.3f at 300
            })
        }
    }

    val lift by animateDpAsState(if (tile.isSelected) 5.dp else 0.dp, tween(140), label = "lift")
    val dimTarget = if (settings.dimBlockedTiles && !tile.isSelectable && !tile.isMatched) 1f else 0f
    val dim by animateFloatAsState(dimTarget, tween(240), label = "dim")
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.94f else 1f, tween(90), label = "press")

    val zIndex = if (tile.isMatched) 100000f + tile.id
    else tile.layer * 1000f + tile.gridY * 100 + tile.gridX
    val saturationFilter = remember(dim) {
        if (dim <= 0.01f) null
        else ColorFilter.colorMatrix(ColorMatrix().apply {
            setToSaturation(1f - (1f - GameTuning.LOCKED_TILE_SATURATION) * dim)
        })
    }
    val burst: () -> Float = {
        val t = matchProgress.value
        when {
            !tile.isMatched -> 0f
            t < 0.48f -> 0f
            t < 0.68f -> ((t - 0.48f) / 0.20f).coerceIn(0f, 1f)
            t < 1f -> (1f - (t - 0.68f) / 0.32f).coerceIn(0f, 1f)
            else -> 0f
        }
    }

    Box(
        Modifier
            .align(Alignment.Center)
            .zIndex(zIndex)
            .offset {
                val t = matchProgress.value
                var x = base.x
                var y = base.y
                if (tile.isMatched && matchTarget != null) {
                    x += (matchTarget.x - base.x) * t
                    y += (matchTarget.y - base.y) * t
                }
                val arc = if (tile.isMatched) sin(t * PI).toFloat() * tileH.value * 0.32f else 0f
                val shakeX = shake.value * tileW.value * 0.07f
                IntOffset(
                    (x + shakeX).dp.roundToPx(),
                    (y - arc).dp.roundToPx() - lift.roundToPx()
                )
            }
            .size(tileW, tileH + depth)
            .graphicsLayer {
                val t = matchProgress.value
                val matchScale = when {
                    !tile.isMatched -> 1f
                    t < 0.6f -> 1f + 0.20f * (t / 0.6f)
                    else -> (1.2f * (1f - (t - 0.6f) / 0.4f)).coerceAtLeast(0f)
                }
                val matchAlpha = when {
                    !tile.isMatched -> 1f
                    t < 0.72f -> 1f
                    else -> (1f - (t - 0.72f) / 0.28f).coerceIn(0f, 1f)
                }
                val a = appear.value
                val s = matchScale * (0.55f + 0.45f * a) * press
                scaleX = s
                scaleY = s
                rotationY = (1f - flip.value) * 90f
                cameraDistance = 14f * density
                alpha = matchAlpha * a.coerceIn(0f, 1f)
                translationY = if (isDealing) (1f - a) * -tileH.toPx() * 0.9f else 0f
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = !tile.isMatched
            ) {
                if (tile.isSelectable) onClick() else onBlockedTap()
            }
            .drawTile(
                settings.tileStyle, settings.boardTheme, tileH, depth, tile.type == "Haku",
                dim = dim,
                selected = tile.isSelected,
                hinted = tile.isHinted,
                burst = burst
            )
    ) {
        TileGlyph(
            tile.type, settings.tileStyle,
            Modifier
                .size(tileW, tileH)
                .padding(horizontal = tileW * 0.08f, vertical = tileH * 0.08f),
            saturationFilter
        )
    }
}

/** Eşleşme anında çarpışma halkası + kıvılcımlar ve yukarı süzülen "+puan" balonu. */
@Composable
private fun BoxScope.ScorePopupView(
    popup: ScorePopup,
    metrics: BoardMetrics,
    theme: BoardTheme,
    onDone: () -> Unit
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(popup.id) {
        progress.animateTo(1f, tween(1300, easing = LinearEasing))
        onDone()
    }
    val impactAt = 0.2f
    val boxSize = metrics.tileW * 3.2f
    Box(
        Modifier
            .align(Alignment.Center)
            .zIndex(200000f)
            .offset { IntOffset(popup.x.dp.roundToPx(), popup.y.dp.roundToPx()) }
            .size(boxSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val p = progress.value
            if (p < impactAt) return@Canvas
            val q = ((p - impactAt) / 0.45f).coerceIn(0f, 1f)
            if (q >= 1f) return@Canvas
            val c = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.minDimension / 2f
            drawCircle(
                theme.accent.copy(alpha = (1f - q) * 0.9f),
                radius = maxR * (0.25f + 0.75f * q), center = c,
                style = Stroke(width = maxR * 0.08f * (1f - q) + 1f)
            )
            for (i in 0 until 10) {
                val angle = (i / 10f) * 2f * PI.toFloat() + 0.3f
                val dist = maxR * (0.2f + 0.8f * q)
                val pos = Offset(c.x + cos(angle) * dist, c.y + sin(angle) * dist)
                drawCircle(
                    if (i % 2 == 0) Color(0xFFFFF6D0) else theme.accent,
                    radius = maxR * 0.045f * (1f - q * 0.6f),
                    center = pos,
                    alpha = 1f - q
                )
            }
        }
        val fontSize = (metrics.tileW.value * 0.42f).coerceIn(14f, 30f).sp
        Text(
            text = "+${popup.points}",
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            color = if (popup.streak >= 5) Color(0xFFFFF176) else Color.White,
            style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.75f), Offset(2f, 3f), 6f)),
            modifier = Modifier.graphicsLayer {
                val p = progress.value
                val q = ((p - impactAt) / (1f - impactAt)).coerceIn(0f, 1f)
                alpha = if (p < impactAt) 0f else (1f - q * q)
                val pop = if (q < 0.15f) 0.6f + 0.4f * (q / 0.15f) * 1.25f else 1.1f - 0.1f * ((q - 0.15f) / 0.85f)
                scaleX = pop
                scaleY = pop
                translationY = -q * metrics.tileH.toPx() * 0.9f
            }
        )
    }
}
