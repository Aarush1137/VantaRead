package com.example.vantaread.ui.reader

import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ReaderSettings(
    val themeMode: ReaderTheme = ReaderTheme.VANTA_BLACK,
    val fontSizeSp: Int = 18,
    val fontType: ReaderFont = ReaderFont.SERIF,
    val lineSpacingMultiplier: Float = 1.5f,
    val horizontalMarginDp: Int = 16,
    val accentColorHex: String = "#8A2BE2" // Vanta Purple Default
)

enum class ReaderTheme(val backgroundColor: Color, val textColor: Color) {
    VANTA_BLACK(Color(0xFF000000), Color(0xFFE0E0E0)),
    CHARCOAL(Color(0xFF1C1C1E), Color(0xFFE5E5EA)),
    SEPIA(Color(0xFFF4ECD8), Color(0xFF5B4636)),
    LIGHT(Color(0xFFFFFFFF), Color(0xFF1C1C1E))
}

enum class ReaderFont(val fontFamily: FontFamily, val androidTypeface: android.graphics.Typeface) {
    SERIF(FontFamily.Serif, android.graphics.Typeface.SERIF),
    SANS_SERIF(FontFamily.SansSerif, android.graphics.Typeface.SANS_SERIF),
    MONOSPACE(FontFamily.Monospace, android.graphics.Typeface.MONOSPACE)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    chapterUrl: String,
    sourceId: String,
    onNavigateBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    LaunchedEffect(chapterUrl, sourceId) {
        viewModel.initialize(chapterUrl, sourceId)
    }

    val content by viewModel.content.collectAsState()
    var settings by remember { mutableStateOf(ReaderSettings()) }
    
    // Parse HTML into chunks for the LazyColumn
    val paragraphs = remember(content) {
        content?.split(Regex("(?i)<br\\s*/?>|</p>|<p>"))
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() } ?: emptyList()
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    var showHud by remember { mutableStateOf(false) }
    var hudInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Auto-hide HUD after 4 seconds of inactivity
    LaunchedEffect(showHud, hudInteractionTime) {
        if (showHud) {
            delay(4000)
            showHud = false
        }
    }

    fun scrollPage(down: Boolean) {
        coroutineScope.launch {
            val viewportHeight = listState.layoutInfo.viewportSize.height
            val scrollAmount = viewportHeight * 0.8f
            listState.animateScrollBy(if (down) scrollAmount else -scrollAmount)
        }
    }

    fun notifyInteraction() {
        if (showHud) hudInteractionTime = System.currentTimeMillis()
    }

    val accentColor = Color(android.graphics.Color.parseColor(settings.accentColorHex))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(settings.themeMode.backgroundColor)
    ) {
        // Layer 1: Text Canvas (LazyColumn)
        if (paragraphs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accentColor)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = settings.horizontalMarginDp.dp,
                    vertical = 80.dp // Padding to prevent HUD from overlapping text at boundaries
                )
            ) {
                items(paragraphs) { paragraph ->
                    AndroidView(
                        factory = { context ->
                            TextView(context).apply {
                                setLineSpacing(0f, settings.lineSpacingMultiplier)
                            }
                        },
                        update = { textView ->
                            textView.textSize = settings.fontSizeSp.toFloat()
                            textView.setTextColor(settings.themeMode.textColor.toArgb())
                            textView.typeface = settings.fontType.androidTypeface
                            textView.text = HtmlCompat.fromHtml(paragraph, HtmlCompat.FROM_HTML_MODE_COMPACT)
                        },
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }
        }

        // Layer 2: Gesture Detection Layer
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.25f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { scrollPage(down = false) }
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.5f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { 
                            showHud = !showHud 
                            if (showHud) notifyInteraction()
                        }
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.25f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { scrollPage(down = true) }
                    )
            )
        }

        // Layer 3: System HUD Overlay
        AnimatedVisibility(
            visible = showHud,
            enter = slideInVertically(initialOffsetY = { -it }),
            exit = slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopAppBar(
                title = { Text("Reader", color = settings.themeMode.textColor) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back",
                            tint = settings.themeMode.textColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = settings.themeMode.backgroundColor.copy(alpha = 0.95f)
                ),
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { notifyInteraction() }
                )
            )
        }

        AnimatedVisibility(
            visible = showHud,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            BottomControlBar(
                settings = settings,
                accentColor = accentColor,
                onSettingsChanged = { 
                    settings = it
                    notifyInteraction()
                },
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { notifyInteraction() }
                )
            )
        }
    }
}

@Composable
fun BottomControlBar(
    settings: ReaderSettings,
    accentColor: Color,
    onSettingsChanged: (ReaderSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    val hudBg = Color(0xFF1E1E1E).copy(alpha = 0.95f)
    val hudText = Color.White

    Surface(
        color = hudBg,
        contentColor = hudText,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Font Size
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Font Size (${settings.fontSizeSp}sp)", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { if (settings.fontSizeSp > 12) onSettingsChanged(settings.copy(fontSizeSp = settings.fontSizeSp - 2)) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) { Text("A-", color = Color.White) }
                    Button(
                        onClick = { if (settings.fontSizeSp < 36) onSettingsChanged(settings.copy(fontSizeSp = settings.fontSizeSp + 2)) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) { Text("A+", color = Color.White) }
                }
            }

            // Margin
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Margins (${settings.horizontalMarginDp}dp)", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { if (settings.horizontalMarginDp > 0) onSettingsChanged(settings.copy(horizontalMarginDp = settings.horizontalMarginDp - 8)) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) { Text("-", color = Color.White) }
                    Button(
                        onClick = { if (settings.horizontalMarginDp < 64) onSettingsChanged(settings.copy(horizontalMarginDp = settings.horizontalMarginDp + 8)) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) { Text("+", color = Color.White) }
                }
            }
            
            // Font Family
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Font", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReaderFont.entries.forEach { font ->
                        val isSelected = settings.fontType == font
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) accentColor else Color.DarkGray)
                                .clickable { onSettingsChanged(settings.copy(fontType = font)) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = font.name.lowercase().replaceFirstChar { it.uppercase() },
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            // Theme Modes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Theme", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ReaderTheme.entries.forEach { theme ->
                        val isSelected = settings.themeMode == theme
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(theme.backgroundColor)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) accentColor else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable { onSettingsChanged(settings.copy(themeMode = theme)) }
                        )
                    }
                }
            }
        }
    }
}
