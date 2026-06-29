package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import com.example.data.model.Dossier
import com.example.data.model.StudyTopicAddon
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

class ClutchColors(
    val bg: Color,
    val primary: Color,
    val surface: Color,
    val border: Color,
    val text: Color,
    val mutedText: Color,
    val cardBg: Color,
    val isDark: Boolean
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CramTimeApp(
    viewModel: CramTimeViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val localTime by viewModel.localTime.collectAsStateWithLifecycle()
    val dossiers by viewModel.allDossiers.collectAsStateWithLifecycle()
    val allAddons by viewModel.allAddons.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()

    val colors = remember(isDarkTheme) {
        ClutchColors(
            bg = if (isDarkTheme) TacticalObsidianBg else StudioChalkBg,
            primary = if (isDarkTheme) CyberMint else DeepIndigo,
            surface = if (isDarkTheme) TacticalSurface else StudioSurface,
            border = if (isDarkTheme) TacticalBorder else StudioBorder,
            text = if (isDarkTheme) HighContrastWhite else InkBlack,
            mutedText = if (isDarkTheme) MutedText else StudioMuted,
            cardBg = if (isDarkTheme) TacticalCardBg else StudioCardBg,
            isDark = isDarkTheme
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .drawBehind {
                val isDark = colors.isDark
                val glowRadius = size.minDimension * 0.9f

                // Top-right atmosphere glow (fuchsia/terracotta)
                drawCircle(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = if (isDark) {
                            listOf(Color(0xFFD946EF).copy(0.08f), Color.Transparent)
                        } else {
                            listOf(Color(0xFFEA580C).copy(0.04f), Color.Transparent)
                        },
                        center = Offset(size.width * 0.95f, size.height * 0.05f),
                        radius = glowRadius
                    )
                )

                // Bottom-left atmosphere glow (violet/amber)
                drawCircle(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = if (isDark) {
                            listOf(Color(0xFF9F7AEA).copy(0.06f), Color.Transparent)
                        } else {
                            listOf(Color(0xFFF59E0B).copy(0.03f), Color.Transparent)
                        },
                        center = Offset(size.width * 0.05f, size.height * 0.95f),
                        radius = glowRadius
                    )
                )

                // Procedural Grid Backdrop
                val step = 40.dp.toPx()
                val alpha = if (isDark) 0.04f else 0.02f
                val gridColor = if (isDark) Color.White else Color.Black
                var x = 0f
                while (x < size.width) {
                    drawLine(
                        color = gridColor,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1f,
                        alpha = alpha
                    )
                    x += step
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f,
                        alpha = alpha
                    )
                    y += step
                }
            }
    ) {
        Crossfade(
            targetState = uiState,
            animationSpec = tween(400, easing = LinearOutSlowInEasing),
            label = "ScreenTransition"
        ) { state ->
            when (state) {
                is CramState.Splash -> {
                    CinematicSplashScreen(
                        onSplashFinished = {
                            viewModel.goBackToInput()
                        }
                    )
                }
                is CramState.Input -> {
                    CommandCenterScreen(
                        viewModel = viewModel,
                        localTime = localTime,
                        dossiers = dossiers,
                        allAddons = allAddons,
                        colors = colors
                    )
                }
                is CramState.Generating -> {
                    KineticIngestionScreen(
                        state = state,
                        colors = colors
                    )
                }
                is CramState.DossierDetail -> {
                    ImmersiveDossierScreen(
                        dossier = state.dossier,
                        viewModel = viewModel,
                        colors = colors
                    )
                }
            }
        }
    }
}

// ==========================================
// SCREEN 1: THE COMMAND CENTER
// ==========================================
@Composable
fun CommandCenterScreen(
    viewModel: CramTimeViewModel,
    localTime: String,
    dossiers: List<Dossier>,
    allAddons: List<StudyTopicAddon>,
    colors: ClutchColors
) {
    val topic by viewModel.topic.collectAsStateWithLifecycle()
    val selectedMode by viewModel.selectedMode.collectAsStateWithLifecycle()
    val deadlineText by viewModel.deadlineText.collectAsStateWithLifecycle()
    val companyBriefing by viewModel.companyBriefing.collectAsStateWithLifecycle()
    val slideCount by viewModel.slideCount.collectAsStateWithLifecycle()
    val attachments by viewModel.attachments.collectAsStateWithLifecycle()

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.addAttachment(it) }
    }

    val docLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.addAttachment(it) }
    }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // --- 1. TOP HEADER BAR ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SYSTEM STATUS",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.mutedText,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "SYNCED // READY",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )
            }

            // Theme Toggle Button
            IconButton(
                onClick = { viewModel.toggleTheme() },
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.cardBg)
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = if (colors.isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle Theme",
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "ZERO HOUR WINDOW",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.mutedText,
                    letterSpacing = 2.sp
                )
                Text(
                    text = localTime,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text,
                    letterSpacing = (-0.5).sp
                )
            }
        }

        // --- 2. HERO TITLE ---
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Hourglass geometric icon
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .drawBehind {
                            val w = size.width
                            val h = size.height
                            val topPath = androidx.compose.ui.graphics.Path().apply {
                                moveTo(w * 0.2f, h * 0.2f)
                                lineTo(w * 0.8f, h * 0.2f)
                                lineTo(w * 0.5f, h * 0.5f)
                                close()
                            }
                            drawPath(topPath, colors.primary)
                            val startY = h * 0.5f
                            val endY = h * 0.8f
                            val streamCount = 4
                            for (i in 0 until streamCount) {
                                val ratio = i.toFloat() / (streamCount - 1)
                                val currentY = startY + (endY - startY) * ratio
                                val halfWidth = w * (0.05f + 0.25f * ratio)
                                drawLine(
                                    color = colors.primary.copy(alpha = 1f - (ratio * 0.3f)),
                                    start = Offset(w * 0.5f - halfWidth, currentY),
                                    end = Offset(w * 0.5f + halfWidth, currentY),
                                    strokeWidth = 2.dp.toPx()
                                )
                            }
                        }
                )
                Text(
                    text = "ZERO HOUR",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = colors.text,
                    letterSpacing = (-1.5).sp,
                    lineHeight = 36.sp
                )
            }
            Text(
                text = "High-yield intelligence generated for critical deadlines.",
                fontSize = 13.sp,
                color = colors.mutedText,
                modifier = Modifier.widthIn(max = 300.dp),
                lineHeight = 18.sp
            )
        }

        // --- STUDY PROGRESS DASHBOARD ---
        if (dossiers.isNotEmpty()) {
            StudyProgressDashboardCard(
                dossiers = dossiers,
                allAddons = allAddons,
                onDossierClick = { viewModel.loadDossier(it) },
                onDeleteDossier = { viewModel.deleteDossier(it) },
                colors = colors
            )
        }

        // --- 3. MODE SELECTOR: SEGMENTED BENTO ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "SELECT LAST HOUR/MINUTE PREPARATION CONFIGURATION MODE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MutedText,
                letterSpacing = 1.5.sp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.cardBg)
                    .border(1.dp, colors.border, RoundedCornerShape(18.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Exam", "Interview", "Presentation").forEach { mode ->
                    val isSelected = selectedMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) Color.White.copy(0.12f) else Color.Transparent)
                            .border(
                                1.dp,
                                if (isSelected) Color.White.copy(0.18f) else Color.Transparent,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { viewModel.selectMode(mode) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) HighContrastWhite else TextWhite40,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        // --- 4. THE MATRIX (TACTILE DIAL & CONFIG ROW) ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "THE DEADLINE MATRIX & METRIC CONFIG",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MutedText,
                letterSpacing = 1.5.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left Cell: Tactile Dial
                TactileDialCard(
                    deadlineText = deadlineText,
                    onDeadlineClick = {
                        val deadlines = listOf("3 Hours", "12 Hours", "24 Hours", "3 Days", "7 Days")
                        val currentIndex = deadlines.indexOf(deadlineText)
                        val nextIndex = (currentIndex + 1) % deadlines.size
                        viewModel.selectDeadline(deadlines[nextIndex])
                    },
                    colors = colors,
                    modifier = Modifier.weight(1f)
                )

                // Right Cell: Secondary Config panels (Depth & Format)
                SecondaryConfigPanel(
                    mode = selectedMode,
                    slideCount = slideCount,
                    colors = colors,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // --- 5. SUBJECT INPUT FIELD ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "SUBJECT DEADLINE OBJECTIVE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MutedText,
                letterSpacing = 1.5.sp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(InputBg)
                    .border(1.dp, colors.border, RoundedCornerShape(18.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedTextField(
                        value = topic,
                        onValueChange = { viewModel.topic.value = it },
                        placeholder = {
                            Text(
                                text = "Paste syllabus or topic...",
                                color = TextWhite40,
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("topic_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = HighContrastWhite,
                            unfocusedTextColor = HighContrastWhite,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        singleLine = true
                    )

                    // Monospace shortcut indicator
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(0.05f))
                            .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "⌘V",
                            color = TextWhite40,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // --- 5.5 STUDY SOURCE ATTACHMENTS (OPTIONAL PHOTOS / DOCUMENTS) ---
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "STUDY SOURCE ATTACHMENTS (OPTIONAL)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colors.mutedText,
                letterSpacing = 1.5.sp
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors.cardBg
                ),
                border = BorderStroke(1.dp, colors.border)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Attach textbook snaps, handwritten notes, syllabus photos, or PDF/text guides. Gemini will ingest them directly to build hyper-customized prep resources.",
                        fontSize = 12.sp,
                        color = colors.mutedText,
                        lineHeight = 16.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { photoLauncher.launch("image/*") },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("attach_photo_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(0.06f),
                                contentColor = colors.text
                            ),
                            border = BorderStroke(1.dp, Color.White.copy(0.12f)),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Attach Photo",
                                    tint = colors.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Add Photo",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        Button(
                            onClick = { docLauncher.launch("*/*") },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("attach_doc_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(0.06f),
                                contentColor = colors.text
                            ),
                            border = BorderStroke(1.dp, Color.White.copy(0.12f)),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = "Attach Document",
                                    tint = colors.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Add Document",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    if (attachments.isNotEmpty()) {
                        HorizontalDivider(
                            color = Color.White.copy(0.08f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Text(
                            text = "${attachments.size} ATTACHED FILE(S)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary,
                            letterSpacing = 1.sp
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            attachments.forEach { attachment ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(0.03f))
                                        .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        val isImage = attachment.mimeType.startsWith("image/")
                                        Icon(
                                            imageVector = if (isImage) Icons.Default.Image else Icons.Default.Description,
                                            contentDescription = attachment.mimeType,
                                            tint = if (isImage) Color(0xFFD946EF) else Color(0xFFF59E0B),
                                            modifier = Modifier.size(18.dp)
                                        )

                                        Column {
                                            Text(
                                                text = attachment.name,
                                                color = colors.text,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = attachment.mimeType.uppercase(),
                                                color = colors.mutedText,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { viewModel.removeAttachment(attachment) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("remove_attachment_${attachment.name}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove attachment",
                                            tint = Color.Red.copy(0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 6. DYNAMIC FORM FIELDS (Morphing/Staggered Visibility) ---
        AnimatedVisibility(
            visible = selectedMode == "Interview" || selectedMode == "Presentation",
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.cardBg)
                    .border(1.dp, colors.border, RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (selectedMode == "Interview") {
                    Text(
                        text = "TARGET COMPANY / ROLE BRIEFING (OPTIONAL)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MutedText,
                        letterSpacing = 1.5.sp
                    )
                    OutlinedTextField(
                        value = companyBriefing,
                        onValueChange = { viewModel.companyBriefing.value = it },
                        placeholder = {
                            Text(
                                text = "e.g., Google Senior Android Engineer, Stripe Staff L6...",
                                color = TextWhite40,
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = HighContrastWhite,
                            unfocusedTextColor = HighContrastWhite,
                            focusedBorderColor = Color.White.copy(0.15f),
                            unfocusedBorderColor = Color.White.copy(0.08f),
                            focusedContainerColor = Color.Black.copy(0.2f),
                            unfocusedContainerColor = Color.Black.copy(0.2f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                if (selectedMode == "Presentation") {
                    Text(
                        text = "TOTAL SLIDES: $slideCount SLIDES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MutedText,
                        letterSpacing = 1.5.sp
                    )
                    Slider(
                        value = slideCount.toFloat(),
                        onValueChange = { viewModel.setSlideCount(it.toInt()) },
                        valueRange = 3f..12f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(0.15f)
                        )
                    )
                }
            }
        }

        // --- 7. ACTION FOOTER GENERATE BUTTON ---
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (topic.trim().isNotEmpty()) Color.White else Color.White.copy(0.08f)
                    )
                    .border(
                        width = 1.dp,
                        color = if (topic.trim().isNotEmpty()) Color.White else Color.White.copy(0.12f),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable(enabled = topic.trim().isNotEmpty()) {
                        viewModel.generateCramMaterial()
                    }
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GENERATE MATERIAL",
                        color = if (topic.trim().isNotEmpty()) Color.Black else Color.White.copy(0.3f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (topic.trim().isNotEmpty()) Color.Black.copy(0.1f) else Color.White.copy(0.05f)
                            )
                            .border(
                                1.dp,
                                if (topic.trim().isNotEmpty()) Color.Black.copy(0.1f) else Color.White.copy(0.1f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "ENTER ↵",
                            color = if (topic.trim().isNotEmpty()) Color.Black else Color.White.copy(0.3f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "V.2.40-ALPHA",
                    color = TextWhite40,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "GEMINI-3.5-FLASH",
                    color = NeonEmerald,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TactileDialCard(
    deadlineText: String,
    onDeadlineClick: () -> Unit,
    colors: ClutchColors,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(160.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(colors.cardBg)
            .border(1.dp, colors.border, RoundedCornerShape(24.dp))
            .clickable { onDeadlineClick() }
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val center = Offset(size.width / 2, size.height / 2)
                    drawCircle(
                        color = Color.White,
                        radius = 80.dp.toPx(),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f),
                        alpha = 0.04f
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 60.dp.toPx(),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f),
                        alpha = 0.07f
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 45.dp.toPx(),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f),
                        alpha = 0.10f
                    )
                }
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "DURATION",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = TextWhite40,
                letterSpacing = 1.5.sp
            )

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .background(Color.Black.copy(0.4f))
                    .border(1.5.dp, Color.White.copy(0.08f), RoundedCornerShape(40.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 4.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 3.dp, height = 7.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(Color.White)
                    )
                }

                val shortDeadline = remember(deadlineText) {
                    when (deadlineText) {
                        "3 Hours" -> "3h"
                        "12 Hours" -> "12h"
                        "24 Hours" -> "24h"
                        "3 Days" -> "3d"
                        "7 Days" -> "7d"
                        else -> deadlineText
                    }
                }

                Text(
                    text = shortDeadline,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = HighContrastWhite
                )
            }

            Text(
                text = "TAP TO ROTATE",
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                color = NeonEmerald,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SecondaryConfigPanel(
    mode: String,
    slideCount: Int,
    colors: ClutchColors,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.height(160.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.cardBg)
                .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "DEPTH",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = colors.mutedText,
                    letterSpacing = 1.5.sp
                )
                Column {
                    Text(
                        text = if (mode == "Exam") "Syllabus Max" else "Extreme",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.text
                    )
                    Text(
                        text = if (mode == "Exam") "Full node mapping" else "80% High-Yield",
                        fontSize = 9.sp,
                        color = colors.mutedText
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.cardBg)
                .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "FORMAT",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = colors.mutedText,
                    letterSpacing = 1.5.sp
                )
                Column {
                    Text(
                        text = when (mode) {
                            "Exam" -> "Dossier + Cards"
                            "Interview" -> "Briefing + Q&As"
                            else -> "Slides ($slideCount)"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.text
                    )
                    Text(
                        text = when (mode) {
                            "Exam" -> "Full Markdown study layout"
                            "Interview" -> "Talking points framework"
                            else -> "Visual deck structures"
                        },
                        fontSize = 9.sp,
                        color = colors.mutedText
                    )
                }
            }
        }
    }
}

@Composable
fun ModeBentoCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(130.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) AmberGlow.copy(0.06f) else RichGraySurface)
            .border(
                1.dp,
                if (isSelected) AmberGlow else Color.White.copy(0.08f),
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) AmberGlow else Color.White.copy(0.5f),
                    modifier = Modifier.size(22.dp)
                )

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(AmberGlow)
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isSelected) AmberGlow else HighContrastWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = MutedText,
                    maxLines = 2,
                    lineHeight = 12.sp,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun HorizontalScrollViewRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .fillMaxWidth()
    ) {
        content()
    }
}

// ==========================================
// SCREEN 0: CINEMATIC SPLASH SCREEN
// ==========================================
@Composable
fun CinematicSplashScreen(
    onSplashFinished: () -> Unit
) {
    var loadingStep by remember { mutableStateOf(0) }
    val steps = listOf(
        "INITIALIZING TEMPORAL INTEGRITY DECK...",
        "LAUNCHING COGNITIVE GEMINI API MATRIX...",
        "STABILIZING CRITICAL STUDY VECTOR CORRIDORS...",
        "ESTABLISHING TACTICAL SYSTEM FEEDBACK ENGINE..."
    )

    LaunchedEffect(Unit) {
        for (i in steps.indices) {
            loadingStep = i
            delay(1000)
        }
        onSplashFinished()
    }

    // Dynamic hour-glass rotational angle or pulse
    val infiniteTransition = rememberInfiniteTransition(label = "HourglassRotate")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Angle"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1250, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalObsidianBg), // High-focus Obsidian Black Theme Bg
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Minimalist hour-glass canvas logo
            Canvas(
                modifier = Modifier
                    .size(80.dp)
                    .graphicsLayer(rotationZ = angle, scaleX = scale, scaleY = scale)
            ) {
                val width = size.width
                val height = size.height

                val path = androidx.compose.ui.graphics.Path().apply {
                    // Top horizontal bar
                    moveTo(width * 0.2f, height * 0.15f)
                    lineTo(width * 0.8f, height * 0.15f)
                    // Angle down to neck
                    lineTo(width * 0.52f, height * 0.5f)
                    // Flare down to base
                    lineTo(width * 0.8f, height * 0.85f)
                    // Bottom horizontal bar
                    lineTo(width * 0.2f, height * 0.85f)
                    // Angle up to neck
                    lineTo(width * 0.48f, height * 0.5f)
                    close()
                }

                // Draw solid geometric wireframe
                drawPath(
                    path = path,
                    color = CyberMint, // Dynamic primary neon color
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 4.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                    )
                )

                // Bottom break apart stream lines
                drawLine(
                    color = CyberMint.copy(0.7f),
                    start = Offset(width * 0.5f, height * 0.52f),
                    end = Offset(width * 0.5f, height * 0.82f),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = CyberMint.copy(0.4f),
                    start = Offset(width * 0.44f, height * 0.65f),
                    end = Offset(width * 0.44f, height * 0.82f),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = CyberMint.copy(0.4f),
                    start = Offset(width * 0.56f, height * 0.65f),
                    end = Offset(width * 0.56f, height * 0.82f),
                    strokeWidth = 2.dp.toPx()
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "ZERO HOUR",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 6.sp,
                fontFamily = FontFamily.Monospace
            )

            Text(
                text = "HIGH-YIELD INTELLIGENCE MATRIX",
                color = CyberMint,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Animated step logger
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text(
                    text = "SYSTEM STATUS: BOOT SEQUENCE",
                    fontSize = 9.sp,
                    color = Color.White.copy(0.5f),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = Color(0x00, 0xFF, 0x87)
                    )
                    Text(
                        text = steps[loadingStep],
                        fontSize = 9.sp,
                        color = Color(0x00, 0xFF, 0x87),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ==========================================
// SCREEN 2: THE KINETIC INGESTION STATE (LOADING)
// ==========================================
@Composable
fun KineticIngestionScreen(
    state: CramState.Generating,
    colors: ClutchColors
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AlphaPulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Futuristic Orbit Loading Ring
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(120.dp),
                color = colors.primary,
                strokeWidth = 3.dp,
                trackColor = colors.primary.copy(0.08f)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${state.secondsElapsed}s",
                    color = colors.primary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ELAPSED",
                    color = colors.mutedText,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Contextual loading subtext with pulsing opacity
        Text(
            text = state.phase.uppercase(),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = colors.text,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.graphicsLayer { alpha = pulseAlpha }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Gemini is structuring high-yield question vectors, key syllabus summaries, and interactive training modules. Stay here.",
            color = colors.mutedText,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Virtual streaming terminal lines
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(InputBg)
                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                TerminalLine("[OK] Connected to Google generative endpoints")
                TerminalLine("[INFO] Loading Gemini flash-3.5 engine constraints")
                TerminalLine("[OK] Instantiating JSON output schema blueprint")
                TerminalLine("[COMPILING] Fusing syllabus nodes directly to local cache")
            }
        }
    }
}

@Composable
fun TerminalLine(text: String) {
    Text(
        text = text,
        color = Color(0xFF34D399),
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

// ==========================================
// SCREEN 3: THE IMMERSIVE DOSSIER VIEW
// ==========================================
@Composable
fun ImmersiveDossierScreen(
    dossier: Dossier,
    viewModel: CramTimeViewModel,
    colors: ClutchColors
) {
    val countdown by viewModel.countdownTimer.collectAsStateWithLifecycle()
    val activeAddons by viewModel.activeAddons.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val jsonObject = remember(dossier.materialText) {
        try {
            JSONObject(dossier.materialText)
        } catch (e: Exception) {
            JSONObject()
        }
    }

    var selectedSection by remember { mutableStateOf("SUMMARY") } // "SUMMARY", "SYLLABUS", "FLASHCARDS", "MOCK", "SLIDES"
    var chatOpen by remember { mutableStateOf(false) }

    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isSendingMsg by viewModel.isSendingChatMessage.collectAsStateWithLifecycle()
    var userChatQuery by remember { mutableStateOf("") }

    val sections = remember(dossier.mode) {
        when (dossier.mode) {
            "Exam" -> listOf("SUMMARY", "SYLLABUS", "FLASHCARDS")
            "Interview" -> listOf("SUMMARY", "MOCK_INTERVIEW")
            else -> listOf("SUMMARY", "SLIDES_DECK")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // --- Sticky Header Bar ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.goBackToInput() }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.text
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = dossier.topic.uppercase(),
                        color = colors.text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "MODE: ${dossier.mode.uppercase()}",
                        color = colors.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Quick Copy Action
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Last Hour/Minute Preparation Material", dossier.materialText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied entire raw JSON blueprint!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = colors.mutedText
                    )
                }

                // Quick Text Share Action
                IconButton(
                    onClick = {
                        val shareText = getReadableMaterialString(dossier, jsonObject)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Last Hour/Minute Preparation Prep Text", shareText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied readable study blueprint to clipboard!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = colors.primary
                    )
                }

                // Export to PDF Offline
                IconButton(
                    onClick = {
                        com.example.util.PdfExporter.exportAndShare(context, dossier, activeAddons)
                    },
                    modifier = Modifier.testTag("export_pdf_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "Export PDF",
                        tint = colors.primary
                    )
                }
            }

            // --- Active Ticking Monospace Countdown Banner ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.cardBg)
                    .border(1.dp, colors.border, RoundedCornerShape(0.dp))
                    .padding(vertical = 10.dp, horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TIME TO DEADLINE",
                        color = colors.mutedText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = countdown,
                        color = colors.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

        // --- Custom Segment Slider Selector ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.cardBg)
                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            sections.forEach { sec ->
                val active = selectedSection == sec
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) Color.White.copy(0.12f) else Color.Transparent)
                        .border(
                            1.dp,
                            if (active) Color.White.copy(0.18f) else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedSection = sec }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = sec.replace("_", " "),
                        color = if (active) HighContrastWhite else TextWhite40,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // --- Scrollable Main Content ---
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            if (selectedSection == "SUMMARY") {
                item {
                    DossierSummaryCard(
                        summary = jsonObject.optString("summary", "Structuring high-yield data outline..."),
                        companyBriefing = jsonObject.optString("companyBriefing"),
                        roleStrategy = jsonObject.optString("roleStrategy"),
                        title = jsonObject.optString("title"),
                        subtitle = jsonObject.optString("subtitle"),
                        colors = colors
                    )
                }
                item {
                    OfflineExportCard(
                        onExportClick = {
                            com.example.util.PdfExporter.exportAndShare(context, dossier, activeAddons)
                        },
                        colors = colors
                    )
                }
            }

            if (selectedSection == "SYLLABUS") {
                val syllabusArray = jsonObject.optJSONArray("syllabus") ?: JSONArray()
                itemsIndexed((0 until syllabusArray.length()).toList()) { _, idx ->
                    val obj = syllabusArray.optJSONObject(idx) ?: JSONObject()
                    val topicName = obj.optString("topic", "Subtopic Node")
                    val addon = activeAddons.find { it.topicName == topicName }
                    val aiNotes = obj.optString("studyNotes", "")
                    val aiVideosJson = obj.optJSONArray("youtubeVideos")
                    SyllabusCard(
                        dossierId = dossier.id,
                        topic = topicName,
                        importance = obj.optString("importance", "HIGH"),
                        content = obj.optString("content", ""),
                        aiNotes = aiNotes,
                        aiVideosJson = aiVideosJson,
                        addon = addon,
                        onSaveAddon = { notes, links ->
                            viewModel.saveTopicAddon(dossier.id, topicName, notes, links)
                        },
                        onToggleCompletion = { completed ->
                            viewModel.toggleTopicCompletion(dossier.id, topicName, completed)
                        },
                        colors = colors
                    )
                }
            }

            if (selectedSection == "FLASHCARDS") {
                val flashcardsArray = jsonObject.optJSONArray("flashcards") ?: JSONArray()
                item {
                    Text(
                        text = "TAP FLASHCARD TO REVEAL KEY INSIGHT",
                        fontSize = 11.sp,
                        color = MutedText,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                itemsIndexed((0 until flashcardsArray.length()).toList()) { _, idx ->
                    val obj = flashcardsArray.optJSONObject(idx) ?: JSONObject()
                    InteractiveFlashcard(
                        question = obj.optString("question", "Question?"),
                        answer = obj.optString("answer", "Answer details."),
                        colors = colors
                    )
                }
            }

            if (selectedSection == "MOCK_INTERVIEW") {
                val questionsArray = jsonObject.optJSONArray("questions") ?: JSONArray()
                itemsIndexed((0 until questionsArray.length()).toList()) { _, idx ->
                    val obj = questionsArray.optJSONObject(idx) ?: JSONObject()
                    MockInterviewCard(
                        index = idx + 1,
                        question = obj.optString("question", "Question text?"),
                        idealResponse = obj.optString("idealResponse", ""),
                        talkingPoints = obj.optJSONArray("talkingPoints") ?: JSONArray(),
                        colors = colors,
                        viewModel = viewModel
                    )
                }
            }

            if (selectedSection == "SLIDES_DECK") {
                val slidesArray = jsonObject.optJSONArray("slides") ?: JSONArray()
                item {
                    InteractiveSlideDeckPreview(slides = slidesArray, colors = colors)
                }
            }
        }
    }

    // Floating Action Button to toggle refinement drawer
    Box(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(bottom = 80.dp, end = 16.dp) // Adjusted to avoid overlap
    ) {
        FloatingActionButton(
            onClick = { chatOpen = !chatOpen },
            containerColor = colors.primary,
            contentColor = if (colors.isDark) Color.Black else Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = if (chatOpen) Icons.Default.Close else Icons.Default.Chat,
                contentDescription = "Refine Material"
            )
        }
    }

    // Refinement Chat Drawer overlay (covering 85% width)
    AnimatedVisibility(
        visible = chatOpen,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .fillMaxHeight()
            .fillMaxWidth(0.85f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.cardBg)
                .border(1.dp, colors.border, RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header of Drawer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ZERO HOUR REFINEMENT",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "GEMINI ACTIVE FEEDBACK LOOP",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp,
                        color = colors.mutedText
                    )
                }
                IconButton(onClick = { chatOpen = false }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Close", tint = colors.mutedText)
                }
            }

            HorizontalDivider(color = colors.border)

            // Scrollable messages list
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                if (chatMessages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Ask Gemini to add subtopics, simplify terminology, or create customized mock focus questions. Try:\n\n'Add a section on quantum tunneling' or 'simplify section 2'",
                                color = colors.mutedText,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(0.85f)
                            )
                        }
                    }
                } else {
                    itemsIndexed(chatMessages) { _, msg ->
                        val isUser = msg.sender == "User"
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 12.dp,
                                            topEnd = 12.dp,
                                            bottomStart = if (isUser) 12.dp else 2.dp,
                                            bottomEnd = if (isUser) 2.dp else 12.dp
                                        )
                                    )
                                    .background(if (isUser) colors.primary.copy(0.15f) else colors.bg)
                                    .border(
                                        1.dp,
                                        if (isUser) colors.primary.copy(0.3f) else colors.border,
                                        RoundedCornerShape(
                                            topStart = 12.dp,
                                            topEnd = 12.dp,
                                            bottomStart = if (isUser) 12.dp else 2.dp,
                                            bottomEnd = if (isUser) 2.dp else 12.dp
                                        )
                                    )
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = msg.message,
                                    color = colors.text,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                            Text(
                                text = msg.sender.uppercase(),
                                fontSize = 8.sp,
                                color = colors.mutedText,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
                            )
                        }
                    }
                }
            }

            if (isSendingMsg) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = colors.primary)
                    Text(text = "REBUILDING DOSSIER MATRIX...", fontSize = 9.sp, color = colors.primary, fontFamily = FontFamily.Monospace)
                }
            }

            // Input box
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = userChatQuery,
                    onValueChange = { userChatQuery = it },
                    placeholder = { Text("Command refinement...", fontSize = 11.sp, color = colors.mutedText) },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = colors.bg,
                        unfocusedContainerColor = colors.bg,
                        focusedTextColor = colors.text,
                        unfocusedTextColor = colors.text,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )
                IconButton(
                    onClick = {
                        if (userChatQuery.trim().isNotEmpty()) {
                            viewModel.sendChatMessage(userChatQuery, dossier)
                            userChatQuery = ""
                        }
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.primary)
                        .size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (colors.isDark) Color.Black else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
}

// ==========================================
// DETAILED REUSABLE PRESENTATIONAL CARDS
// ==========================================

@Composable
fun DossierSummaryCard(
    summary: String,
    companyBriefing: String,
    roleStrategy: String,
    title: String,
    subtitle: String,
    colors: ClutchColors
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.cardBg)
            .border(1.dp, colors.border, RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = NeonEmerald,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "EXECUTIVE BRIEF",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = NeonEmerald,
                letterSpacing = 1.5.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        if (title.isNotEmpty()) {
            Text(
                text = title,
                fontSize = 22.sp,
                color = HighContrastWhite,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = TextWhite40,
                    fontWeight = FontWeight.Medium
                )
            }
            HorizontalDivider(color = Color.White.copy(0.08f))
        }

        if (summary.isNotEmpty()) {
            Text(
                text = summary,
                color = HighContrastWhite,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
        }

        if (companyBriefing.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "COMPANY EXPECTATIONS & CULTURE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = NeonEmerald,
                letterSpacing = 1.5.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = companyBriefing,
                color = HighContrastWhite,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }

        if (roleStrategy.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "PREP STRATEGY BLUEPRINT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFA78BFA),
                letterSpacing = 1.5.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = roleStrategy,
                color = HighContrastWhite,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
fun OfflineExportCard(
    onExportClick: () -> Unit,
    colors: ClutchColors
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.cardBg)
            .border(1.dp, colors.border, RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "OFFLINE STUDY COMPANION",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colors.primary,
                letterSpacing = 1.5.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Text(
            text = "Take your preparation offline! Generate a beautifully paginated PDF study guide containing your executive brief, topics, detailed AI copilot notes, recommended videos, and your personal additions.",
            color = colors.mutedText,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )

        Button(
            onClick = onExportClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = if (colors.isDark) Color.Black else Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("export_pdf_card_button")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Generate & Share Study PDF",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun SyllabusCard(
    dossierId: Int,
    topic: String,
    importance: String,
    content: String,
    aiNotes: String,
    aiVideosJson: org.json.JSONArray?,
    addon: StudyTopicAddon?,
    onSaveAddon: (notes: String, youtubeLinks: String) -> Unit,
    onToggleCompletion: (Boolean) -> Unit,
    colors: ClutchColors
) {
    var expanded by remember { mutableStateOf(false) }
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.cardBg)
            .border(
                1.dp,
                if (importance == "CRITICAL") RedCritical.copy(0.4f) else colors.border,
                RoundedCornerShape(16.dp)
            )
            .clickable { expanded = !expanded }
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Checkbox(
                        checked = addon?.isCompleted == true,
                        onCheckedChange = { onToggleCompletion(it) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = colors.primary,
                            uncheckedColor = Color.White.copy(0.3f),
                            checkmarkColor = if (colors.isDark) Color.Black else Color.White
                        ),
                        modifier = Modifier.testTag("topic_completion_checkbox_${topic.hashCode()}")
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (importance == "CRITICAL") RedCritical.copy(0.12f) else Color.White.copy(0.08f)
                                    )
                                    .border(
                                        1.dp,
                                        if (importance == "CRITICAL") RedCritical.copy(0.2f) else Color.White.copy(0.1f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = importance,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (importance == "CRITICAL") RedCritical else HighContrastWhite,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = topic,
                            color = if (addon?.isCompleted == true) colors.mutedText else HighContrastWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            style = if (addon?.isCompleted == true) {
                                androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                            } else {
                                androidx.compose.ui.text.TextStyle.Default
                            }
                        )
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = TextWhite40
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color.White.copy(0.06f))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = content,
                        color = Color.White.copy(0.85f),
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )

                    // --- AI COPILOT STUDY GUIDE ---
                    if (aiNotes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.primary.copy(0.04f))
                                .border(1.dp, colors.primary.copy(0.12f), RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = colors.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "AI COPILOT STUDY GUIDE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.primary,
                                        letterSpacing = 1.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                
                                Text(
                                    text = aiNotes,
                                    color = Color.White.copy(0.9f),
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }

                    // --- AI RECOMMENDATION: BEST YOUTUBE TUTORIALS ---
                    if (aiVideosJson != null && aiVideosJson.length() > 0) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Red.copy(0.03f))
                                .border(1.dp, Color.Red.copy(0.15f), RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.Red,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "BEST YOUTUBE TUTORIALS FOUND",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Red,
                                        letterSpacing = 1.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    for (i in 0 until aiVideosJson.length()) {
                                        val videoObj = aiVideosJson.optJSONObject(i)
                                        if (videoObj != null) {
                                            val vTitle = videoObj.optString("title", "Recommended Video")
                                            val vUrl = videoObj.optString("url", "")
                                            if (vUrl.isNotBlank()) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color.White.copy(0.02f))
                                                        .border(1.dp, Color.White.copy(0.04f), RoundedCornerShape(8.dp))
                                                        .clickable {
                                                            try {
                                                                uriHandler.openUri(vUrl)
                                                            } catch (e: Exception) {
                                                                // handle error
                                                            }
                                                        }
                                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = "Watch",
                                                        tint = Color.Red,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = vTitle,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = HighContrastWhite,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = "Tap to search / watch on YouTube",
                                                            fontSize = 10.sp,
                                                            color = colors.mutedText
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color.White.copy(0.1f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "MY STUDY HUB",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary,
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Personal Notes Input
                    var notesText by remember(addon?.notes) { mutableStateOf(addon?.notes ?: "") }
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Personal Study Notes", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("notes_input_${topic}"),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = colors.text),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.border.copy(0.5f),
                            focusedLabelColor = colors.primary,
                            unfocusedLabelColor = colors.mutedText,
                            focusedTextColor = colors.text,
                            unfocusedTextColor = colors.text
                        ),
                        trailingIcon = {
                            if (notesText != (addon?.notes ?: "")) {
                                IconButton(
                                    onClick = { onSaveAddon(notesText, addon?.youtubeLinks ?: "") },
                                    modifier = Modifier.testTag("save_note_${topic}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Save,
                                        contentDescription = "Save Note",
                                        tint = colors.primary
                                    )
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // YouTube Links Section
                    var newLinkText by remember { mutableStateOf("") }
                    val linksList = remember(addon?.youtubeLinks) {
                        addon?.youtubeLinks?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()
                    }

                    OutlinedTextField(
                        value = newLinkText,
                        onValueChange = { newLinkText = it },
                        label = { Text("Share Study YouTube Link", fontSize = 12.sp) },
                        placeholder = { Text("https://youtube.com/...", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("youtube_input_${topic}"),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = colors.text),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.border.copy(0.5f),
                            focusedLabelColor = colors.primary,
                            unfocusedLabelColor = colors.mutedText,
                            focusedTextColor = colors.text,
                            unfocusedTextColor = colors.text
                        ),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (newLinkText.isNotBlank()) {
                                        val updatedLinks = (linksList + newLinkText.trim()).joinToString("\n")
                                        onSaveAddon(notesText, updatedLinks)
                                        newLinkText = ""
                                    }
                                },
                                modifier = Modifier.testTag("add_link_${topic}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = "Add Link",
                                    tint = colors.primary
                                )
                            }
                        }
                    )

                    if (linksList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "YOUTUBE REFERENCES:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.mutedText,
                            letterSpacing = 1.sp
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            linksList.forEach { link ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(0.02f))
                                        .border(1.dp, Color.White.copy(0.04f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play icon",
                                            tint = Color.Red,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = link,
                                            fontSize = 11.sp,
                                            color = colors.text,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.clickable {
                                                try {
                                                    uriHandler.openUri(link)
                                                } catch (e: Exception) {
                                                    // handle invalid url
                                                }
                                            }
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            val updatedLinks = linksList.filter { it != link }.joinToString("\n")
                                            onSaveAddon(notesText, updatedLinks)
                                        },
                                        modifier = Modifier.size(24.dp).testTag("delete_link_${link}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Link",
                                            tint = Color.Red.copy(0.7f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveFlashcard(
    question: String,
    answer: String,
    colors: ClutchColors
) {
    var rotated by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (rotated) 180f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "Rotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { rotated = !rotated },
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, colors.border),
        colors = CardDefaults.cardColors(
            containerColor = if (rotated) colors.bg else colors.cardBg
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
            contentAlignment = Alignment.Center
        ) {
            if (rotation <= 90f) {
                // Front Side (Question)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "QUESTION",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = question,
                        color = colors.text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 21.sp
                    )
                }
            } else {
                // Back Side (Answer - Rotated back horizontally so text is readable)
                Column(
                    modifier = Modifier.graphicsLayer { rotationY = 180f },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "HIGH-YIELD BLUEPRINT ANSWER",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = answer,
                        color = colors.text.copy(0.9f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MockInterviewCard(
    index: Int,
    question: String,
    idealResponse: String,
    talkingPoints: JSONArray,
    colors: ClutchColors,
    viewModel: CramTimeViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // --- State variables for voice simulation ---
    var spokenAnswerText by remember { mutableStateOf("") }
    var critiqueText by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var isGeneratingCritique by remember { mutableStateOf(false) }

    // TextToSpeech setup
    var tts by remember { mutableStateOf<android.speech.tts.TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        var t: android.speech.tts.TextToSpeech? = null
        t = android.speech.tts.TextToSpeech(context) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                t?.language = java.util.Locale.US
                isTtsReady = true
            }
        }
        tts = t
        onDispose {
            t?.stop()
            t?.shutdown()
        }
    }

    // SpeechRecognizer Intent Launcher
    val speechIntent = remember {
        android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault())
            putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Recite your response...")
        }
    }

    val speechLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isRecording = false
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val matches = data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                spokenAnswerText = matches[0]
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.cardBg)
            .border(1.dp, colors.border, RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded }
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Q$index",
                        color = colors.primary,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp
                    )
                    Text(
                        text = question,
                        color = colors.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 20.sp
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = colors.mutedText
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = colors.border.copy(0.3f))

                    // Voice controls toolbar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                tts?.let {
                                    val res = it.speak(question, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "QuestionSpeak")
                                    if (res == android.speech.tts.TextToSpeech.ERROR) {
                                        Toast.makeText(context, "TTS speech failed to play.", Toast.LENGTH_SHORT).show()
                                    }
                                } ?: run {
                                    Toast.makeText(context, "Voice engine initializing...", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary.copy(0.12f),
                                contentColor = colors.primary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Read Aloud", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SPEAK QUESTION", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }

                        Button(
                            onClick = {
                                isRecording = true
                                try {
                                    speechLauncher.launch(speechIntent)
                                } catch (e: Exception) {
                                    isRecording = false
                                    Toast.makeText(context, "Voice recognition not supported on this device", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecording) Color.Red.copy(0.15f) else colors.primary.copy(0.12f),
                                contentColor = if (isRecording) Color.Red else colors.primary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(if (isRecording) Icons.Default.MicOff else Icons.Default.Mic, contentDescription = "Record Answer", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isRecording) "RECORDING..." else "RECORD ANSWER",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Answer Response/Transcription Area (fully editable fallback)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.bg)
                            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "YOUR RESPONSE / TRANSCRIPTION",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = colors.primary,
                                fontWeight = FontWeight.Bold
                            )
                            androidx.compose.material3.OutlinedTextField(
                                value = spokenAnswerText,
                                onValueChange = { spokenAnswerText = it },
                                placeholder = {
                                    Text(
                                        text = if (isRecording) "Awaiting voice response stream..." else "Type your response here, or tap RECORD ANSWER to speak...",
                                        fontSize = 11.sp,
                                        color = colors.mutedText
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 80.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = colors.text),
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.primary,
                                    unfocusedBorderColor = colors.border.copy(0.6f),
                                    focusedContainerColor = colors.cardBg,
                                    unfocusedContainerColor = colors.cardBg,
                                    focusedTextColor = colors.text,
                                    unfocusedTextColor = colors.text
                                )
                            )

                            if (spokenAnswerText.trim().isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = {
                                        isGeneratingCritique = true
                                        coroutineScope.launch {
                                            val feedbackPrompt = """
                                                You are an elite recruiter. Rate my interview response below:
                                                QUESTION: "$question"
                                                IDEAL ANSWER: "$idealResponse"
                                                SPOKEN/WRITTEN RESPONSE: "$spokenAnswerText"
                                                
                                                Provide an objective, constructive critique (3 concise sentences max). Focus on strengths and clear improvements.
                                            """.trimIndent()
                                            val res = viewModel.callGeminiAPIDirect(feedbackPrompt)
                                            critiqueText = res ?: "Critique generation failed. Try again."
                                            isGeneratingCritique = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.primary,
                                        contentColor = if (colors.isDark) Color.Black else Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    if (isGeneratingCritique) {
                                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = if (colors.isDark) Color.Black else Color.White)
                                    } else {
                                        Text("SUBMIT FOR GEMINI CRITIQUE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Gemini Critique Feedback Box
                    if (critiqueText.isNotEmpty() || isGeneratingCritique) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.primary.copy(0.06f))
                                .border(1.dp, colors.primary.copy(0.2f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "GEMINI RECRUITER CRITIQUE",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isGeneratingCritique) "Analyzing response metrics..." else critiqueText,
                                    color = colors.text,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }

                    Text(
                        text = "IDEAL RESPONSE BLUEPRINT",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )

                    Text(
                        text = idealResponse,
                        color = colors.text.copy(0.85f),
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )

                    if (talkingPoints.length() > 0) {
                        Text(
                            text = "CRITICAL TALKING POINTS TO COVER",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (colors.isDark) Color(0xFFA78BFA) else Color(0x8B5CF6FF),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )

                        for (i in 0 until talkingPoints.length()) {
                            val pt = talkingPoints.optString(i, "")
                            var checked by remember { mutableStateOf(false) }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.text.copy(0.04f))
                                    .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                                    .clickable { checked = !checked }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { checked = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = colors.primary,
                                        uncheckedColor = colors.text.copy(0.2f)
                                    )
                                )
                                Text(
                                    text = pt,
                                    color = if (checked) colors.mutedText else colors.text,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveSlideDeckPreview(
    slides: JSONArray,
    colors: ClutchColors
) {
    var activeIndex by remember { mutableStateOf(0) }
    val total = slides.length()

    if (total == 0) return

    val slideObj = remember(activeIndex) {
        slides.optJSONObject(activeIndex) ?: JSONObject()
    }

    val keyword = remember(activeIndex) {
        slideObj.optString("imageSearchKeyword", "technology_analytics_abstract").lowercase()
    }

    // Map common keywords to high-quality specific Unsplash images
    val imageUrl = remember(keyword) {
        when {
            keyword.contains("physics") || keyword.contains("quantum") || keyword.contains("laser") -> 
                "https://images.unsplash.com/photo-1507668077129-56e32842fceb?auto=format&fit=crop&w=500&q=80"
            keyword.contains("analytics") || keyword.contains("data") || keyword.contains("chart") || keyword.contains("finance") -> 
                "https://images.unsplash.com/photo-1551288049-bebda4e38f71?auto=format&fit=crop&w=500&q=80"
            keyword.contains("medical") || keyword.contains("bio") || keyword.contains("health") || keyword.contains("clinical") -> 
                "https://images.unsplash.com/photo-1530026405186-ed1ea0ac7a63?auto=format&fit=crop&w=500&q=80"
            keyword.contains("ai") || keyword.contains("intelligence") || keyword.contains("robot") || keyword.contains("machine") -> 
                "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=500&q=80"
            keyword.contains("financial") || keyword.contains("money") || keyword.contains("stock") || keyword.contains("business") -> 
                "https://images.unsplash.com/photo-1611974789855-9c2a0a7236a3?auto=format&fit=crop&w=500&q=80"
            else -> 
                "https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=500&q=80" // Default technology
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PITCH DECK INTERACTIVE READER",
                fontSize = 11.sp,
                color = colors.mutedText,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "${activeIndex + 1} OF $total",
                fontSize = 11.sp,
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        // --- Visual Slide Frame (Simulating physical slide deck) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.cardBg)
                .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- Left Column: Slide Text Arguments (60%) ---
                Column(
                    modifier = Modifier.weight(1.2f),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "SLIDE ${slideObj.optInt("slideNumber", activeIndex + 1)} // ZERO HOUR DECK",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = colors.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = slideObj.optString("heading", "Heading Node"),
                            fontSize = 16.sp,
                            color = colors.text,
                            fontWeight = FontWeight.Black,
                            lineHeight = 20.sp
                        )
                        HorizontalDivider(color = colors.border.copy(0.3f))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        val bullets = slideObj.optJSONArray("bulletPoints") ?: JSONArray()
                        for (i in 0 until bullets.length().coerceAtMost(3)) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Text(text = "•", color = colors.primary, fontSize = 14.sp)
                                Text(
                                    text = bullets.optString(i, ""),
                                    color = colors.text.copy(0.85f),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Visual Layout Tip Footer Box
                    val tip = slideObj.optString("visualTip")
                    if (tip.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.bg.copy(0.5f))
                                .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "LAYOUT TIP: $tip",
                                    fontSize = 9.sp,
                                    color = colors.mutedText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // --- Right Column: Dynamic Coil Stock Image (40%) ---
                Box(
                    modifier = Modifier
                        .weight(0.8f)
                        .aspectRatio(0.8f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.bg)
                        .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Slide visual context for keyword $keyword",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    
                    // Small floating keyword chip on the image
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(0.75f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = keyword.replace("_", " ").uppercase(),
                            color = CyberMint,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // --- Navigation Deck Controls ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (activeIndex > 0) colors.cardBg else colors.bg.copy(0.3f))
                    .border(
                        1.dp,
                        if (activeIndex > 0) colors.border else Color.Transparent,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable(enabled = activeIndex > 0) { activeIndex-- }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "PREVIOUS SLIDE",
                    color = if (activeIndex > 0) colors.text else colors.mutedText.copy(0.3f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (activeIndex < total - 1) colors.cardBg else colors.bg.copy(0.3f))
                    .border(
                        1.dp,
                        if (activeIndex < total - 1) colors.border else Color.Transparent,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable(enabled = activeIndex < total - 1) { activeIndex++ }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "NEXT SLIDE",
                    color = if (activeIndex < total - 1) colors.text else colors.mutedText.copy(0.3f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ==========================================
// FORMATTED READABLE OUTPUT STRING PARSER
// ==========================================
private fun getReadableMaterialString(dossier: Dossier, json: JSONObject): String {
    val sb = java.lang.StringBuilder()
    sb.append("=== LAST HOUR/MINUTE PREPARATION STUDY BRIEF ===\n")
    sb.append("TOPIC: ${dossier.topic}\n")
    sb.append("MODE: ${dossier.mode}\n")
    sb.append("DEADLINE: ${dossier.deadlineText}\n\n")

    when (dossier.mode) {
        "Exam" -> {
            sb.append("SUMMARY:\n").append(json.optString("summary")).append("\n\n")
            val syllabus = json.optJSONArray("syllabus") ?: JSONArray()
            sb.append("SYLLABUS CORE NODES:\n")
            for (i in 0 until syllabus.length()) {
                val item = syllabus.optJSONObject(i) ?: JSONObject()
                sb.append("- ").append(item.optString("topic")).append(" [").append(item.optString("importance")).append("]\n")
                sb.append("  ").append(item.optString("content")).append("\n\n")
            }
            val flashcards = json.optJSONArray("flashcards") ?: JSONArray()
            sb.append("FLASHCARDS STUDY BLUEPRINTS:\n")
            for (i in 0 until flashcards.length()) {
                val fc = flashcards.optJSONObject(i) ?: JSONObject()
                sb.append("Q: ").append(fc.optString("question")).append("\n")
                sb.append("A: ").append(fc.optString("answer")).append("\n\n")
            }
        }
        "Interview" -> {
            sb.append("SUMMARY COMPANY CULTURE:\n").append(json.optString("companyBriefing")).append("\n\n")
            sb.append("STRATEGY BLUEPRINT:\n").append(json.optString("roleStrategy")).append("\n\n")
            val questions = json.optJSONArray("questions") ?: JSONArray()
            sb.append("MOCK TOUGH QUESTIONS:\n")
            for (i in 0 until questions.length()) {
                val q = questions.optJSONObject(i) ?: JSONObject()
                sb.append("Q: ").append(q.optString("question")).append("\n")
                sb.append("A: ").append(q.optString("idealResponse")).append("\n")
                val tps = q.optJSONArray("talkingPoints") ?: JSONArray()
                sb.append("Talking Points:\n")
                for (j in 0 until tps.length()) {
                    sb.append("  * ").append(tps.optString(j)).append("\n")
                }
                sb.append("\n")
            }
        }
        else -> {
            sb.append("TITLE: ").append(json.optString("title")).append("\n")
            sb.append("SUBTITLE: ").append(json.optString("subtitle")).append("\n\n")
            val slides = json.optJSONArray("slides") ?: JSONArray()
            sb.append("SLIDE DECK SEQUENCE BRIEF:\n")
            for (i in 0 until slides.length()) {
                val slide = slides.optJSONObject(i) ?: JSONObject()
                sb.append("SLIDE ").append(slide.optInt("slideNumber")).append(": ").append(slide.optString("heading")).append("\n")
                val bullets = slide.optJSONArray("bulletPoints") ?: JSONArray()
                for (j in 0 until bullets.length()) {
                    sb.append("  * ").append(bullets.optString(j)).append("\n")
                }
                sb.append("Layout design tip: ").append(slide.optString("visualTip")).append("\n\n")
            }
        }
    }
    return sb.toString()
}

@Composable
fun StudyProgressDashboardCard(
    dossiers: List<Dossier>,
    allAddons: List<StudyTopicAddon>,
    onDossierClick: (Dossier) -> Unit,
    onDeleteDossier: (Int) -> Unit,
    colors: ClutchColors
) {
    // Calculate progress stats
    val sessionProgressList = dossiers.map { dossier ->
        val (completed, total) = dossier.getCompletionProgress(allAddons)
        Triple(dossier, completed, total)
    }

    val totalTopics = sessionProgressList.sumOf { it.third }
    val completedTopics = sessionProgressList.sumOf { it.second }
    val overallPercentage = if (totalTopics > 0) completedTopics.toFloat() / totalTopics else 0f

    var isExpanded by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.cardBg)
            .border(1.dp, colors.border, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.primary.copy(0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "STUDY PROGRESS DASHBOARD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary,
                            letterSpacing = 1.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "SYLLABUS COMPLETION INSIGHTS",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = HighContrastWhite
                        )
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle Dashboard",
                        tint = colors.mutedText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    HorizontalDivider(color = colors.border.copy(0.5f))

                    // Circular ring + Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Custom Circular Progress Ring
                        CircularProgressRing(
                            percentage = overallPercentage,
                            color = colors.primary,
                            trackColor = colors.text.copy(0.1f),
                            modifier = Modifier
                                .size(110.dp)
                                .testTag("overall_circular_progress")
                        )

                        // General Statistics Card
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DashboardStatRow(
                                label = "Total Sessions",
                                value = "${dossiers.size}",
                                icon = Icons.Default.MenuBook,
                                colors = colors
                            )
                            DashboardStatRow(
                                label = "Topics Completed",
                                value = "$completedTopics / $totalTopics",
                                icon = Icons.Default.CheckCircle,
                                colors = colors
                            )
                            DashboardStatRow(
                                label = "Avg Completion",
                                value = "${(overallPercentage * 100).toInt()}%",
                                icon = Icons.Default.TrendingUp,
                                colors = colors
                            )
                        }
                    }

                    HorizontalDivider(color = colors.border.copy(0.5f))

                    // --- RECHARTS STYLE INTERACTIVE DASHBOARD ---
                    RechartsComboDashboard(
                        sessionProgressList = sessionProgressList,
                        colors = colors
                    )

                    HorizontalDivider(color = colors.border.copy(0.5f))

                    // Study Sessions Title
                    Text(
                        text = "PREPARATION SESSION BREAKDOWN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.mutedText,
                        letterSpacing = 1.2.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    // Summary cards list
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        sessionProgressList.forEach { (dossier, completed, total) ->
                            StudySessionSummaryCard(
                                dossier = dossier,
                                completed = completed,
                                total = total,
                                onClick = { onDossierClick(dossier) },
                                onDelete = { onDeleteDossier(dossier.id) },
                                colors = colors
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CircularProgressRing(
    percentage: Float,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 10.dp
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sizeMin = size.minDimension
            val strokeWidthPx = strokeWidth.toPx()
            val radius = (sizeMin - strokeWidthPx) / 2f
            val centerOffset = Offset(size.width / 2f, size.height / 2f)

            // Draw track ring
            drawCircle(
                color = trackColor,
                radius = radius,
                center = centerOffset,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokeWidthPx,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )

            // Draw progress arc
            val sweepAngle = percentage * 360f
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokeWidthPx,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(percentage * 100).toInt()}%",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = HighContrastWhite,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "READY",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = color.copy(alpha = 0.8f),
                letterSpacing = 1.5.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun DashboardStatRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    colors: ClutchColors
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.mutedText,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = colors.mutedText,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = value,
            fontSize = 13.sp,
            color = HighContrastWhite,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun StudySessionSummaryCard(
    dossier: Dossier,
    completed: Int,
    total: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    colors: ClutchColors
) {
    val completionRatio = if (total > 0) completed.toFloat() / total else 0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.text.copy(0.03f))
            .border(1.dp, colors.border.copy(0.5f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when (dossier.mode) {
                                        "Exam" -> Color(0xFF3B82F6).copy(0.12f)
                                        "Interview" -> Color(0xFF34D399).copy(0.12f)
                                        else -> Color(0xFF8B5CF6).copy(0.12f)
                                    }
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = dossier.mode.uppercase(),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (dossier.mode) {
                                    "Exam" -> Color(0xFF60A5FA)
                                    "Interview" -> Color(0xFF34D399)
                                    else -> Color(0xFFA78BFA)
                                }
                            )
                        }
                        Text(
                            text = dossier.deadlineText,
                            fontSize = 9.sp,
                            color = colors.mutedText,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dossier.topic,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = HighContrastWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$completed / $total",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "TOPICS",
                            fontSize = 8.sp,
                            color = colors.mutedText,
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    
                    IconButton(
                        onClick = { onDelete() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Preparation Session",
                            tint = RedCritical.copy(0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Progress Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(colors.text.copy(0.08f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(completionRatio)
                            .clip(CircleShape)
                            .background(
                                if (completionRatio == 1f) colors.primary else colors.primary.copy(0.7f)
                            )
                    )
                }
                Text(
                    text = "${(completionRatio * 100).toInt()}%",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = HighContrastWhite,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(32.dp)
                )
            }
        }
    }
}

@Composable
fun RechartsComboDashboard(
    sessionProgressList: List<Triple<Dossier, Int, Int>>,
    colors: ClutchColors
) {
    if (sessionProgressList.isEmpty()) return

    var selectedIndex by remember { mutableStateOf(0) }
    // Ensure selectedIndex is within bounds if the list changes
    LaunchedEffect(sessionProgressList) {
        if (selectedIndex >= sessionProgressList.size) {
            selectedIndex = (sessionProgressList.size - 1).coerceAtLeast(0)
        }
    }

    val activeItem = sessionProgressList.getOrNull(selectedIndex) ?: sessionProgressList.first()
    val activeDossier = activeItem.first
    val activeCompleted = activeItem.second
    val activeTotal = activeItem.third
    val activePending = activeTotal - activeCompleted
    val activeVelocity = if (activeTotal > 0) (activeCompleted.toFloat() / activeTotal * 100).toInt() else 0

    val velocityLabel = when {
        activeVelocity == 100 -> "MAXIMUM VELOCITY (100% DONE)"
        activeVelocity >= 75 -> "ACCELERATED VELOCITY (${activeVelocity}% DONE)"
        activeVelocity >= 40 -> "STEADY VELOCITY (${activeVelocity}% DONE)"
        activeVelocity > 0 -> "INITIAL VELOCITY (${activeVelocity}% DONE)"
        else -> "ZERO VELOCITY (0% DONE)"
    }

    val velocityColor = when {
        activeVelocity >= 75 -> Color(0xFF10B981) // Emerald Green
        activeVelocity >= 40 -> colors.primary
        activeVelocity > 0 -> Color(0xFFF59E0B) // Amber
        else -> Color.Gray
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.text.copy(0.02f))
            .border(1.dp, colors.border.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- 1. ACTIVE TOOLTIP CARD (RECHARTS STYLE) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colors.text.copy(0.04f))
                .border(1.dp, colors.border.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTIVE PREPARATION METRICS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary,
                        letterSpacing = 1.2.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(velocityColor.copy(0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = velocityLabel,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = velocityColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Text(
                    text = activeDossier.topic,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = HighContrastWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(colors.primary))
                        Text(
                            text = "$activeCompleted Covered",
                            fontSize = 11.sp,
                            color = colors.mutedText,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White.copy(0.2f)))
                        Text(
                            text = "$activePending Pending",
                            fontSize = 11.sp,
                            color = colors.mutedText,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // --- 2. COMBO BAR & LINE CHART ---
        val maxTopics = sessionProgressList.map { it.third }.maxOrNull()?.coerceAtLeast(1) ?: 5
        var chartWidth by remember { mutableStateOf(0) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Y-Axis labels (topic count grid)
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(text = "$maxTopics", fontSize = 10.sp, color = colors.mutedText, fontFamily = FontFamily.Monospace)
                Text(text = "${(maxTopics * 3) / 4}", fontSize = 10.sp, color = colors.mutedText, fontFamily = FontFamily.Monospace)
                Text(text = "${maxTopics / 2}", fontSize = 10.sp, color = colors.mutedText, fontFamily = FontFamily.Monospace)
                Text(text = "${maxTopics / 4}", fontSize = 10.sp, color = colors.mutedText, fontFamily = FontFamily.Monospace)
                Text(text = "0", fontSize = 10.sp, color = colors.mutedText, fontFamily = FontFamily.Monospace)
            }

            // Canvas Grid & Bars
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .onSizeChanged { chartWidth = it.width }
                    .pointerInput(sessionProgressList) {
                        detectTapGestures { offset ->
                            if (chartWidth > 0) {
                                val colWidth = chartWidth.toFloat() / sessionProgressList.size
                                val colIndex = (offset.x / colWidth).toInt().coerceIn(0, sessionProgressList.size - 1)
                                selectedIndex = colIndex
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val n = sessionProgressList.size
                    val colWidth = w / n

                    // Draw Horizontal Gridlines (Recharts style)
                    val gridLines = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
                    gridLines.forEach { ratio ->
                        val yLine = ratio * h
                        drawLine(
                            color = Color.White.copy(alpha = 0.06f),
                            start = Offset(0f, yLine),
                            end = Offset(w, yLine),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Draw column highlights, bars & velocity trend line nodes
                    val points = ArrayList<Offset>()

                    for (i in 0 until n) {
                        val item = sessionProgressList[i]
                        val completed = item.second
                        val total = item.third
                        val pending = total - completed

                        val centerX = i * colWidth + colWidth / 2f

                        // 1. Column Highlight
                        if (selectedIndex == i) {
                            drawRect(
                                color = colors.primary.copy(alpha = 0.08f),
                                topLeft = Offset(i * colWidth, 0f),
                                size = androidx.compose.ui.geometry.Size(colWidth, h)
                            )
                            // Selected border
                            drawLine(
                                color = colors.primary.copy(alpha = 0.3f),
                                start = Offset(i * colWidth, 0f),
                                end = Offset(i * colWidth, h),
                                strokeWidth = 1.dp.toPx()
                            )
                            drawLine(
                                color = colors.primary.copy(alpha = 0.3f),
                                start = Offset((i + 1) * colWidth, 0f),
                                end = Offset((i + 1) * colWidth, h),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // 2. Dual Bars (Covered vs Pending)
                        val barWidth = (colWidth * 0.25f).coerceIn(8.dp.toPx(), 20.dp.toPx())
                        val gap = 4.dp.toPx()

                        val leftBarX = centerX - barWidth - gap / 2f
                        val rightBarX = centerX + gap / 2f

                        val hCovered = (completed.toFloat() / maxTopics) * h
                        val hPending = (pending.toFloat() / maxTopics) * h

                        val yCovered = h - hCovered
                        val yPending = h - hPending

                        // Draw Covered Bar (Primary Accent)
                        if (completed > 0) {
                            drawRoundRect(
                                color = colors.primary,
                                topLeft = Offset(leftBarX, yCovered),
                                size = androidx.compose.ui.geometry.Size(barWidth, hCovered),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }

                        // Draw Pending Bar (Muted Sub-layer)
                        if (pending > 0) {
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.12f),
                                topLeft = Offset(rightBarX, yPending),
                                size = androidx.compose.ui.geometry.Size(barWidth, hPending),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }

                        // 3. Collect Velocity Trend Line Node
                        val completionPct = if (total > 0) completed.toFloat() / total else 0f
                        val nodeY = h - (completionPct * h)
                        points.add(Offset(centerX, nodeY))
                    }

                    // Draw Velocity Trend Line segments (Recharts glow style)
                    if (points.size > 1) {
                        for (i in 0 until points.size - 1) {
                            drawLine(
                                color = Color(0xFF06B6D4), // Cyan/Sky Blue velocity line
                                start = points[i],
                                end = points[i + 1],
                                strokeWidth = 2.5.dp.toPx()
                            )
                        }
                    }

                    // Draw glowing node points
                    points.forEachIndexed { idx, point ->
                        val isSel = selectedIndex == idx
                        drawCircle(
                            color = Color(0xFF06B6D4).copy(alpha = if (isSel) 0.4f else 0.2f),
                            radius = (if (isSel) 8.dp else 6.dp).toPx(),
                            center = point
                        )
                        drawCircle(
                            color = Color(0xFF06B6D4),
                            radius = (if (isSel) 4.5.dp else 3.dp).toPx(),
                            center = point
                        )
                    }
                }
            }
        }

        // --- 3. X-AXIS LABELS ROW ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 34.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val colCount = sessionProgressList.size
            sessionProgressList.forEachIndexed { i, triple ->
                val label = triple.first.topic.take(8).uppercase()
                Text(
                    text = label,
                    fontSize = 9.sp,
                    color = if (selectedIndex == i) colors.primary else colors.mutedText,
                    fontWeight = if (selectedIndex == i) FontWeight.Bold else FontWeight.Normal,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // --- 4. LEGEND ROW ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors.primary)
                    )
                    Text(text = "Covered", fontSize = 11.sp, color = colors.mutedText, fontWeight = FontWeight.Bold)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(0.12f))
                    )
                    Text(text = "Pending", fontSize = 11.sp, color = colors.mutedText, fontWeight = FontWeight.Bold)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF06B6D4))
                    )
                    Text(text = "Preparation Velocity", fontSize = 11.sp, color = colors.mutedText, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
