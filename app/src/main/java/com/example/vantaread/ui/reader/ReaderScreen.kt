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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.vantaread.data.model.ReaderSettings
import com.example.vantaread.data.model.ReaderFont
import com.example.vantaread.data.model.ReaderTheme
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    chapterUrl: String,
    sourceId: String,
    novelUrl: String,
    onNavigateBack: () -> Unit,
    onNavigateToNovel: (novelUrl: String) -> Unit,
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

    val isTtsPlaying by viewModel.isTtsPlaying.collectAsState()
    val ttsHighlightIndex by viewModel.ttsHighlightIndex.collectAsState()
    val ttsVoices by viewModel.ttsVoices.collectAsState()

    val settings by viewModel.settings.collectAsState()

    // Parse HTML into chunks for the LazyColumn
    val paragraphs = remember(content) {
        content?.split(Regex("(?i)<br\\s*/?>|</p>|<p>"))
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() } ?: emptyList()
    }

    val parsedParagraphs = remember(paragraphs) {
        paragraphs.map { HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_COMPACT) }
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var showHud by remember { mutableStateOf(false) }
    var showChapterPicker by remember { mutableStateOf(false) }
    var hudInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Restore scroll position
    LaunchedEffect(initialScrollIndex, paragraphs.size) {
        if (initialScrollIndex > 0 && paragraphs.isNotEmpty() && initialScrollIndex < paragraphs.size) {
            listState.scrollToItem(initialScrollIndex)
        }
    }

    // Auto-scroll to TTS highlighted paragraph
    LaunchedEffect(ttsHighlightIndex) {
        if (isTtsPlaying && ttsHighlightIndex >= 0 && ttsHighlightIndex < paragraphs.size) {
            // Scroll so the highlighted item is near the top/center
            listState.animateScrollToItem(ttsHighlightIndex, -100)
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

    // Pull to next chapter logic
    var overscrollAccumulator by remember { mutableFloatStateOf(0f) }

    val nestedScrollConnection = remember(currentChapterIndex) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y < 0) {
                    overscrollAccumulator += available.y
                    if (overscrollAccumulator < -200f) { // Pull threshold
                        overscrollAccumulator = 0f
                        viewModel.navigateToChapter(currentChapterIndex + 1)?.let {
                            onNavigateToChapter(it.url, it.title)
                        }
                    }
                } else if (consumed.y != 0f) {
                    overscrollAccumulator = 0f
                }
                return Offset.Zero
            }
        }
    }

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
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { offset ->
                                    val width = size.width
                                    when {
                                        offset.x < width * 0.25f -> scrollPage(down = false)
                                        offset.x > width * 0.75f -> scrollPage(down = true)
                                        else -> {
                                            showHud = !showHud
                                            if (showHud) notifyInteraction()
                                        }
                                    }
                                }
                            )
                        },
                    contentPadding = PaddingValues(
                        horizontal = settings.horizontalMarginDp.dp,
                        vertical = 80.dp // Padding to prevent HUD from overlapping text at boundaries
                    )
                ) {
                    itemsIndexed(parsedParagraphs) { index, parsedParagraph ->
                        AndroidView(
                            factory = { context ->
                                TextView(context).apply {
                                    setLineSpacing(0f, settings.lineHeight)
                                }
                            },
                            update = { textView ->
                                textView.textSize = settings.fontSizeSp.toFloat()
                                textView.setTextColor(settings.themeMode.textColor.toArgb())
                                textView.typeface = settings.fontType.androidTypeface
                                textView.textAlignment = when (settings.textAlignment) {
                                    "Center" -> android.view.View.TEXT_ALIGNMENT_CENTER
                                    "Right" -> android.view.View.TEXT_ALIGNMENT_VIEW_END
                                    else -> android.view.View.TEXT_ALIGNMENT_VIEW_START
                                }
                                textView.text = parsedParagraph

                                if (isTtsPlaying && index == ttsHighlightIndex) {
                                    textView.setBackgroundColor(accentColor.copy(alpha = 0.2f).toArgb())
                                } else {
                                    textView.setBackgroundColor(Color.Transparent.toArgb())
                                }
                            },
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                }
            }
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Reader", color = settings.themeMode.textColor, style = MaterialTheme.typography.titleMedium)
                            if (chapters.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "${chapters.size} Chs",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
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
                actions = {
                    IconButton(onClick = { onNavigateToNovel(novelUrl) }) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = "Go to Novel Details",
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
                    viewModel.updateSettings(it)
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
                onOpenChapterPicker = {
                    showChapterPicker = true
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
                isTtsPlaying = isTtsPlaying,
                onToggleTts = {
                    if (isTtsPlaying) {
                        viewModel.pauseTts()
                    } else {
                        val startIndex = listState.firstVisibleItemIndex.coerceAtLeast(0)
                        viewModel.startTts(paragraphs, startIndex)
                    }
                },
                ttsVoices = ttsVoices,
                onTtsVoiceSelected = {
                    viewModel.selectTtsVoice(it)
                    notifyInteraction()
                },
                onTtsRateChanged = {
                    viewModel.setTtsSpeechRate(it)
                    notifyInteraction()
                },
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { notifyInteraction() }
                )
            )
        }

        if (showChapterPicker) {
            ChapterPickerSheet(
                chapters = chapters,
                currentChapterIndex = currentChapterIndex,
                onDismiss = { showChapterPicker = false },
                onChapterSelected = { chapter ->
                    showChapterPicker = false
                    onNavigateToChapter(chapter.url, chapter.title)
                }
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
    onOpenChapterPicker: () -> Unit,
    onPrevChapter: () -> Unit,
    onNextChapter: () -> Unit,
    isTtsPlaying: Boolean,
    onToggleTts: () -> Unit,
    ttsVoices: List<TtsVoiceOption>,
    onTtsVoiceSelected: (TtsVoiceOption) -> Unit,
    onTtsRateChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val hudBg = Color(0xFF1E1E1E).copy(alpha = 0.95f)
    val hudText = Color.White
    var showVoiceMenu by remember { mutableStateOf(false) }
    val selectedVoice = ttsVoices.firstOrNull { it.name == settings.ttsVoiceName }
        ?: ttsVoices.firstOrNull { it.localeTag == settings.ttsLocaleTag }

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

                IconButton(onClick = onOpenChapterPicker) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Find chapter",
                        tint = Color.White
                    )
                }

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

            // Auto-scroll and TTS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleTts,
                    modifier = Modifier.background(if (isTtsPlaying) accentColor else Color.DarkGray, CircleShape)
                ) {
                    Icon(if (isTtsPlaying) androidx.compose.material.icons.Icons.Default.Pause else androidx.compose.material.icons.Icons.Default.PlayArrow, contentDescription = "TTS")
                }

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = Color.White)
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { showVoiceMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text(
                            text = selectedVoice?.label ?: "Default voice",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = showVoiceMenu,
                        onDismissRequest = { showVoiceMenu = false }
                    ) {
                        ttsVoices.forEach { voice ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = voice.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                onClick = {
                                    showVoiceMenu = false
                                    onTtsVoiceSelected(voice)
                                }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Voice %.1fx".format(settings.ttsSpeechRate), style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = settings.ttsSpeechRate,
                    onValueChange = onTtsRateChanged,
                    valueRange = 0.5f..2.0f,
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

            // Line Height
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Line Height (%.1f)".format(settings.lineHeight), style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { if (settings.lineHeight > 1.0f) onSettingsChanged(settings.copy(lineHeight = settings.lineHeight - 0.1f)) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) { Text("-", color = Color.White) }
                    Button(
                        onClick = { if (settings.lineHeight < 3.0f) onSettingsChanged(settings.copy(lineHeight = settings.lineHeight + 0.1f)) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) { Text("+", color = Color.White) }
                }
            }

            // Text Alignment
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Alignment", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Left", "Center", "Right").forEach { align ->
                        val isSelected = settings.textAlignment == align
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) accentColor else Color.DarkGray)
                                .clickable { onSettingsChanged(settings.copy(textAlignment = align)) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = align,
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChapterPickerSheet(
    chapters: List<com.example.vantaread.data.db.ChapterEntity>,
    currentChapterIndex: Int,
    onDismiss: () -> Unit,
    onChapterSelected: (com.example.vantaread.data.db.ChapterEntity) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filteredChapters = remember(chapters, query) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            chapters
        } else {
            chapters.filterIndexed { index, chapter ->
                chapter.title.contains(trimmedQuery, ignoreCase = true) ||
                    (index + 1).toString() == trimmedQuery
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Jump to chapter", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                label = { Text("Search title or number") },
                singleLine = true
            )
            LazyColumn(
                modifier = Modifier.height(420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(filteredChapters) { _, chapter ->
                    val realIndex = chapters.indexOfFirst { it.url == chapter.url }
                    val isCurrent = realIndex == currentChapterIndex
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChapterSelected(chapter) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Chapter ${realIndex + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isCurrent) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                            Text(
                                text = chapter.title,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
