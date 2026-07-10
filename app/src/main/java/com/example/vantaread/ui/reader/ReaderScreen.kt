package com.example.vantaread.ui.reader

import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
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
    novelUrl: String,
    onNavigateBack: () -> Unit,
    onNavigateToChapter: (chapterUrl: String, chapterTitle: String) -> Unit,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    LaunchedEffect(chapterUrl, sourceId, novelUrl) {
        viewModel.initialize(chapterUrl, sourceId, novelUrl)
    }

    val content by viewModel.content.collectAsState()
    val chapterTitle by viewModel.chapterTitle.collectAsState()
    val isAutoScrolling by viewModel.isAutoScrolling.collectAsState()
    val autoScrollSpeed by viewModel.autoScrollSpeed.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val currentChapterIndex by viewModel.currentChapterIndex.collectAsState()
    val initialScrollIndex by viewModel.initialScrollIndex.collectAsState()
    
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

    // Restore scroll position
    LaunchedEffect(initialScrollIndex, paragraphs.size) {
        if (initialScrollIndex > 0 && paragraphs.isNotEmpty() && initialScrollIndex < paragraphs.size) {
            listState.scrollToItem(initialScrollIndex)
        }
    }

    // Save scroll position
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { firstVisibleItemIndex ->
                if (paragraphs.isNotEmpty()) {
                    viewModel.saveScrollPosition(firstVisibleItemIndex, paragraphs.size)
                }
            }
    }

    // Auto-scroll logic
    LaunchedEffect(isAutoScrolling, autoScrollSpeed) {
        if (isAutoScrolling) {
            while (true) {
                listState.animateScrollBy(autoScrollSpeed * 2f, tween(50, easing = LinearEasing))
            }
        }
    }

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

    // Calculate progress
    val currentProgress = if (paragraphs.isNotEmpty()) {
        val firstVisible = listState.firstVisibleItemIndex
        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: firstVisible
        (lastVisible.toFloat() / paragraphs.size.coerceAtLeast(1)).coerceIn(0f, 1f)
    } else {
        0f
    }

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
            Column(modifier = Modifier.fillMaxSize()) {
                // Chapter progress indicator at the very top (below status bar ideally)
                LinearProgressIndicator(
                    progress = { currentProgress },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = accentColor,
                    trackColor = settings.themeMode.backgroundColor
                )

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
        
        // Auto-scrolling indicator
        if (isAutoScrolling && !showHud) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E1E1E).copy(alpha = 0.8f),
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Auto-scrolling", style = MaterialTheme.typography.bodySmall)
                    IconButton(onClick = { viewModel.toggleAutoScroll() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Stop", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Layer 3: System HUD Overlay
        AnimatedVisibility(
            visible = showHud,
            enter = slideInVertically(initialOffsetY = { -it }),
            exit = slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopAppBar(
                title = { 
                    Column {
                        Text("Reader", color = settings.themeMode.textColor, style = MaterialTheme.typography.titleMedium)
                        if (chapterTitle.isNotEmpty()) {
                            Text(chapterTitle, color = settings.themeMode.textColor.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
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
                currentChapterIndex = currentChapterIndex,
                totalChapters = chapters.size,
                isAutoScrolling = isAutoScrolling,
                autoScrollSpeed = autoScrollSpeed,
                onSettingsChanged = { 
                    settings = it
                    notifyInteraction()
                },
                onToggleAutoScroll = {
                    viewModel.toggleAutoScroll()
                    notifyInteraction()
                },
                onAutoScrollSpeedChanged = {
                    viewModel.setAutoScrollSpeed(it)
                    notifyInteraction()
                },
                onPrevChapter = {
                    viewModel.navigateToChapter(currentChapterIndex - 1)?.let {
                        onNavigateToChapter(it.url, it.title)
                    }
                },
                onNextChapter = {
                    viewModel.navigateToChapter(currentChapterIndex + 1)?.let {
                        onNavigateToChapter(it.url, it.title)
                    }
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
    currentChapterIndex: Int,
    totalChapters: Int,
    isAutoScrolling: Boolean,
    autoScrollSpeed: Float,
    onSettingsChanged: (ReaderSettings) -> Unit,
    onToggleAutoScroll: () -> Unit,
    onAutoScrollSpeedChanged: (Float) -> Unit,
    onPrevChapter: () -> Unit,
    onNextChapter: () -> Unit,
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
            
            // Chapter Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPrevChapter,
                    enabled = currentChapterIndex > 0
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, 
                        contentDescription = "Previous Chapter",
                        tint = if (currentChapterIndex > 0) Color.White else Color.Gray
                    )
                }
                
                Text(
                    text = if (totalChapters > 0) "Chapter ${currentChapterIndex + 1} / $totalChapters" else "Loading...",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
                
                IconButton(
                    onClick = onNextChapter,
                    enabled = currentChapterIndex < totalChapters - 1
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward, 
                        contentDescription = "Next Chapter",
                        tint = if (currentChapterIndex < totalChapters - 1) Color.White else Color.Gray
                    )
                }
            }

            // Auto-scroll
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleAutoScroll,
                    modifier = Modifier.background(if (isAutoScrolling) accentColor else Color.DarkGray, CircleShape)
                ) {
                    Icon(Icons.Default.Speed, contentDescription = "Auto-scroll")
                }
                
                Text("Speed", style = MaterialTheme.typography.bodyMedium)
                
                Slider(
                    value = autoScrollSpeed,
                    onValueChange = onAutoScrollSpeedChanged,
                    valueRange = 0.5f..5.0f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor
                    )
                )
            }

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
