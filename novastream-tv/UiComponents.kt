package tv.garden.global.webapp

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import kotlin.math.abs

val DiamondBlack = Color(0xFF1A1C20)
val DiamondPanel = Color(0xFF252830)
val DiamondRed = Color(0xFFE50914)
val PastelPink = Color(0xFFFFB7B2)
val PastelBlue = Color(0xFFAEC6CF)
val PastelPurple = Color(0xFFC3B1E1)
val PastelGreen = Color(0xFF9BE8A5)
val FocusGlow = Color(0xFF7B68EE)

private fun channelTypeLabel(channel: Channel): String {
    val countryName = CountryCatalog.displayName(channel.country, channel.group)
    val raw = channel.group.trim()
    if (raw.isEmpty()) return ""
    val translated = LanguageManager.getTranslatedCategory(raw).trim()
    if (translated.isEmpty()) return ""
    val foldedType = CountryCatalog.fold(translated)
    val foldedCountry = CountryCatalog.fold(countryName)
    if (foldedType == foldedCountry) return ""
    if (foldedType == CountryCatalog.fold(channel.country)) return ""
    val iso = CountryCatalog.isoFromAny(raw)
    if (iso != null && !CountryCatalog.isGenre(raw)) return ""
    return translated
}

@Composable
fun CompactSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(0.28f))
            .border(1.dp, Color.Gray.copy(0.45f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Search,
            null,
            tint = PastelPurple,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(6.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(hint, fontSize = 12.sp, color = Color.Gray, maxLines = 1)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 12.sp, lineHeight = 14.sp),
                cursorBrush = SolidColor(PastelPink),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ErrorStateView(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Rounded.WifiOff,
                null,
                tint = DiamondRed.copy(0.6f),
                modifier = Modifier.size(60.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                message,
                color = Color.LightGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = DiamondRed)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(LanguageManager.retryBtn, color = Color.White)
            }
        }
    }
}

@Composable
fun MiniFlag(country: String, group: String = "", size: Int = 18) {
    val context = LocalContext.current
    val code = CountryCatalog.resolve(country, group)
    val painter = rememberAsyncImagePainter(
        model = coil.request.ImageRequest.Builder(context)
            .data(CountryCatalog.flagUrl(country, group, 80))
            .crossfade(true)
            .build()
    )
    val loaded = painter.state is AsyncImagePainter.State.Success
    Box(
        modifier = Modifier.padding(end = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.28f)),
            contentAlignment = Alignment.Center
        ) {
            if (!loaded) {
                if (code == "INT" || code == "UN" || code == "EU") {
                    Icon(
                        Icons.Rounded.Public,
                        contentDescription = null,
                        tint = Color.White.copy(0.85f),
                        modifier = Modifier.size((size * 0.68f).dp)
                    )
                } else {
                    Text(
                        code.take(2),
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold,
                        fontSize = (size * 0.36f).sp
                    )
                }
            }
            Image(
                painter = painter,
                contentDescription = CountryCatalog.displayName(country, group),
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun ChannelRow(
    channel: Channel,
    isFav: Boolean,
    isPlaying: Boolean,
    indexLabel: String,
    onPlay: () -> Unit,
    onFav: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val bgColor = when {
        isPlaying -> PastelPurple.copy(0.18f)
        isFocused -> FocusGlow.copy(0.12f)
        else -> Color.Transparent
    }
    val borderMod = if (isFocused || isPlaying) Modifier.border(
        1.5.dp,
        if (isPlaying) PastelPurple.copy(0.55f) else FocusGlow.copy(0.5f),
        RoundedCornerShape(8.dp)
    ) else Modifier
    val quality = detectQuality(channel)
    val countryName = CountryCatalog.displayName(channel.country, channel.group)
    val typeLabel = channelTypeLabel(channel)

    Row(
        borderMod
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onPlay() }
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            indexLabel,
            color = Color.Gray,
            fontSize = 10.sp,
            modifier = Modifier.width(24.dp)
        )
        SmartChannelLogo(channel.logo, channel.name)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (channel.is_premium) "⭐ ${channel.name}" else channel.name,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (quality.isNotEmpty()) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier
                            .background(
                                when (quality) {
                                    "4K" -> Color(0xFFFFD700); "FHD" -> Color(0xFF4CAF50)
                                    "HD" -> PastelBlue; else -> Color.Gray
                                }.copy(0.7f), RoundedCornerShape(3.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            quality,
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 1.dp)
            ) {
                MiniFlag(channel.country, channel.group, size = 14)
                Text(
                    countryName,
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (typeLabel.isNotEmpty()) {
                    Spacer(Modifier.width(5.dp))
                    Text(
                        typeLabel,
                        color = PastelBlue.copy(0.95f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        IconButton(onClick = onFav, modifier = Modifier.size(32.dp)) {
            Icon(
                if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                null, tint = if (isFav) DiamondRed else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
    HorizontalDivider(color = Color.Black.copy(0.5f), thickness = 1.dp)
}

@Composable
fun SmartChannelLogo(logoUrl: String, channelName: String) {
    val context = LocalContext.current
    val firstLetter = channelName.firstOrNull()?.toString()?.uppercase() ?: "?"
    val colorIndex = abs(channelName.hashCode()) % 5
    val bgColor = when (colorIndex) {
        0 -> Color(0xFFE57373); 1 -> Color(0xFF81C784); 2 -> Color(0xFF64B5F6)
        3 -> Color(0xFFFFD54F); else -> Color(0xFF9575CD)
    }
    val painter = rememberAsyncImagePainter(
        model = coil.request.ImageRequest.Builder(context).data(logoUrl).crossfade(true).build()
    )
    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .size(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Transparent)
    ) {
        if (painter.state !is AsyncImagePainter.State.Success) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(bgColor), Alignment.Center
            ) {
                Text(
                    firstLetter,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
        Image(
            painter = painter, contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun SettingsDialog(
    hiddenCount: Int,
    onDismiss: () -> Unit,
    onClearCache: () -> Unit,
    onSetTimer: (Int) -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onRestoreHidden: () -> Unit
) {
    var timerInput by remember { mutableStateOf("") }
    val sectionShape = RoundedCornerShape(14.dp)
    val actionButtonWidth = 148.dp

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2229)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    LanguageManager.settingsTitle,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(14.dp))

                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(0.05f), sectionShape)
                        .padding(14.dp)
                ) {
                    Text(
                        LanguageManager.timerTitle,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = timerInput,
                            onValueChange = {
                                if (it.all { c -> c.isDigit() } && (it.toIntOrNull() ?: 0) <= 180) {
                                    timerInput = it
                                }
                            },
                            placeholder = { Text("0", color = Color.Gray) },
                            modifier = Modifier
                                .width(88.dp)
                                .heightIn(min = 48.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PastelPurple,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { onSetTimer(timerInput.toIntOrNull() ?: 0) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DiamondRed,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.width(actionButtonWidth),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(LanguageManager.setTimer, color = Color.White, maxLines = 1)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Button(
                    onClick = onClearCache,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(0.08f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Delete, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(LanguageManager.clearCache, color = Color.White)
                }

                if (hiddenCount > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        String.format(LanguageManager.hiddenBroken, hiddenCount),
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = onRestoreHidden,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Replay, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(LanguageManager.restoreBroken, fontSize = 12.sp)
                    }
                }

                TextButton(onClick = onOpenPrivacyPolicy) {
                    Text("🔗 ${LanguageManager.privacyPolicy}", color = PastelBlue, fontSize = 12.sp)
                }
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DiamondRed,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.width(actionButtonWidth),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(LanguageManager.close, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun GestureOverlay(activity: Activity) {
    val context = LocalContext.current
    var showIndicator by remember { mutableStateOf(false) }
    var indicatorIcon by remember { mutableStateOf(Icons.AutoMirrored.Rounded.VolumeUp) }
    var indicatorText by remember { mutableStateOf("") }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { showIndicator = true },
                    onDragEnd = { showIndicator = false },
                    onDragCancel = { showIndicator = false }
                ) { change, dragAmount ->
                    val xPos = change.position.x
                    if (xPos > size.width / 2) {
                        val cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                        val newVol = (cur + (-dragAmount / 30)).coerceIn(0f, maxVol).toInt()
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                        indicatorIcon = Icons.Rounded.VolumeUp
                        indicatorText = "${(newVol / maxVol * 100).toInt()}%"
                    } else {
                        val attr = activity.window.attributes
                        val cur = if (attr.screenBrightness < 0) 0.5f else attr.screenBrightness
                        val newBright = (cur + (-dragAmount / 500)).coerceIn(0.01f, 1f)
                        attr.screenBrightness = newBright
                        activity.window.attributes = attr
                        indicatorIcon = Icons.Rounded.Brightness6
                        indicatorText = "${(newBright * 100).toInt()}%"
                    }
                }
            }
    ) {
        if (showIndicator) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(0.6f), RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(indicatorIcon, null, tint = Color.White, modifier = Modifier.size(40.dp))
                    Text(indicatorText, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TvFocusableIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(40.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) Modifier.border(2.dp, FocusGlow, RoundedCornerShape(8.dp))
                else Modifier
            )
            .background(
                if (isFocused) FocusGlow.copy(0.2f) else Color.DarkGray.copy(0.3f),
                RoundedCornerShape(8.dp)
            )
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NeonGlassButton(
    text: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "scale")
    val infiniteTransition = rememberInfiniteTransition(label = "neon")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Reverse),
        label = "alpha"
    )
    val activeBrush = Brush.linearGradient(
        colors = listOf(
            PastelPink.copy(alpha = borderAlpha),
            PastelBlue.copy(alpha = borderAlpha),
            PastelPurple.copy(alpha = borderAlpha)
        )
    )
    val focusBrush = Brush.linearGradient(colors = listOf(FocusGlow, FocusGlow))
    val inactiveBrush =
        Brush.linearGradient(colors = listOf(Color.Gray.copy(0.3f), Color.Gray.copy(0.3f)))
    val borderBrush = when {
        isFocused -> focusBrush; isActive -> activeBrush; else -> inactiveBrush
    }
    val containerColor = when {
        isFocused -> FocusGlow.copy(0.15f); isActive -> Color.White.copy(0.1f)
        else -> Color.Black.copy(0.2f)
    }

    Box(
        modifier = modifier
            .scale(scale)
            .heightIn(min = 34.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .background(containerColor)
            .border(if (isFocused) 2.dp else 1.dp, borderBrush, RoundedCornerShape(10.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isActive || isFocused) Color.White else Color.LightGray,
            fontSize = 11.sp,
            fontWeight = if (isActive || isFocused) FontWeight.Bold else FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.basicMarquee()
        )
    }
}

@Composable
fun CountryListRow(
    countryCode: String,
    displayName: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.97f else 1f, label = "rowScale")
    val rowShape = RoundedCornerShape(14.dp)
    val borderColor = when {
        isFocused -> FocusGlow
        isSelected -> PastelGreen
        else -> Color.White.copy(alpha = 0.10f)
    }
    val bgColor = when {
        isSelected -> PastelGreen.copy(alpha = 0.12f)
        isFocused -> FocusGlow.copy(alpha = 0.12f)
        else -> Color.White.copy(alpha = 0.04f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(rowShape)
            .background(bgColor)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .border(if (isFocused || isSelected) 2.dp else 1.dp, borderColor, rowShape)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MiniFlag(countryCode, size = 26)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                displayName,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                String.format(LanguageManager.channelCountFmt, count),
                color = Color.Gray,
                fontSize = 11.sp
            )
        }
        if (isSelected) {
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = PastelGreen,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SelectionDialog(
    title: String,
    list: List<String>,
    selectedValue: String,
    counts: Map<String, Int>,
    onSelect: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(list, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) list
        else list.filter {
            CountryCatalog.displayName(it).lowercase().contains(q) ||
                it.lowercase().contains(q)
        }
    }
    Dialog(onDismissRequest = { onSelect("") }) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DiamondPanel.copy(0.97f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxHeight(0.82f)
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                CompactSearchField(
                    value = query,
                    onValueChange = { query = it },
                    hint = LanguageManager.countrySearchHint,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it }) { item ->
                        CountryListRow(
                            countryCode = item,
                            displayName = CountryCatalog.displayName(item),
                            count = counts[item] ?: 0,
                            isSelected = item == selectedValue ||
                                CountryCatalog.resolve(item) == CountryCatalog.resolve(selectedValue)
                        ) { onSelect(item) }
                    }
                }
                Button(
                    onClick = { onSelect("") },
                    colors = ButtonDefaults.buttonColors(containerColor = DiamondRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) { Text(LanguageManager.close, color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
