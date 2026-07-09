package com.example.vantaread.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import androidx.hilt.navigation.compose.hiltViewModel
import android.widget.TextView

enum class ReaderTheme(val bg: Color, val text: Color, val label: String) {
    Light(Color(0xFFFFFFFF), Color(0xFF000000), "Light"),
    Dark(Color(0xFF121212), Color(0xFFE0E0E0), "Dark"),
    Sepia(Color(0xFFF4ECD8), Color(0xFF5B4636), "Sepia")
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
    
    var showSettings by remember { mutableStateOf(false) }
    var fontSizeSp by remember { mutableFloatStateOf(18f) }
    var readerTheme by remember { mutableStateOf(ReaderTheme.Light) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reader") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = readerTheme.bg,
                    titleContentColor = readerTheme.text,
                    actionIconContentColor = readerTheme.text,
                    navigationIconContentColor = readerTheme.text
                )
            )
        },
        containerColor = readerTheme.bg
    ) { padding ->
        if (content == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                AndroidView(
                    factory = { context ->
                        TextView(context).apply {
                            setLineSpacing(0f, 1.4f)
                        }
                    },
                    update = { textView ->
                        textView.textSize = fontSizeSp
                        textView.setTextColor(readerTheme.text.toArgb())
                        textView.text = HtmlCompat.fromHtml(content!!, HtmlCompat.FROM_HTML_MODE_COMPACT)
                    }
                )
            }
        }
        
        if (showSettings) {
            ModalBottomSheet(onDismissRequest = { showSettings = false }) {
                Column(
                    modifier = Modifier.padding(16.dp).padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Settings", style = MaterialTheme.typography.titleLarge)
                    
                    Text("Font Size: ${fontSizeSp.toInt()}sp")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(onClick = { if (fontSizeSp > 12f) fontSizeSp -= 2f }) {
                            Text("A-")
                        }
                        Button(onClick = { if (fontSizeSp < 36f) fontSizeSp += 2f }) {
                            Text("A+")
                        }
                    }
                    
                    Text("Theme")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ReaderTheme.entries.forEach { theme ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(theme.bg)
                                    .clickable { readerTheme = theme }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(theme.label.take(1), color = theme.text)
                            }
                        }
                    }
                }
            }
        }
    }
}
