package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ChatMessage
import com.example.ui.MubtakirViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: MubtakirViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()
    val chatSessions by viewModel.chatSessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    var isHistoryOpen by remember { mutableStateOf(false) }
    
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    
    // Auto-scroll to the bottom when messages change
    LaunchedEffect(chatMessages.size, isChatLoading) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main Chat Column containing Header, Messages and Input Panel
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Chat Header with History Toggle and New Chat Button
            Card(
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // History Toggle Button
                    IconButton(
                        onClick = { isHistoryOpen = !isHistoryOpen },
                        modifier = Modifier.testTag("chat_history_toggle")
                    ) {
                        BadgedBox(
                            badge = {
                                if (chatSessions.isNotEmpty()) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text(chatSessions.size.toString(), fontSize = 9.sp)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forum,
                                contentDescription = "سجل المحادثات",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Middle Branding Info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SupportAgent,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "مُساعد الابتكار الذكي",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "صمم تطبيقات ومواقع ويب آلياً",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // New Chat Button
                    IconButton(
                        onClick = {
                            viewModel.startNewChat()
                            Toast.makeText(context, "تم بدء محادثة جديدة! ✨", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("new_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddComment,
                            contentDescription = "محادثة جديدة",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(chatMessages) { message ->
                    ChatBubbleItem(
                        message = message,
                        onAddProjectClick = { parsedCode, lang ->
                            viewModel.insertActiveManualProject(
                                title = "مشروع من المساعد الذكي",
                                description = "مشروع برمجي تم تصميمه وتوليده آلياً بواسطة المساعد الذكي لمبتكر.",
                                type = if (lang.lowercase() == "html") "DESIGN" else "DO",
                                technologies = if (lang.isNotEmpty()) lang else "Kotlin, Compose",
                                difficulty = 3,
                                stepsList = listOf(
                                    "دراسة ومراجعة الكود البرمجي المولد في المحادثة",
                                    "تجهيز بيئة التطوير ونسخ الكود المولد محلياً",
                                    "تعديل التصاميم وإضافة لمستك الإبداعية الخاصة",
                                    "تشغيل واختبار التطبيق أو الموقع البرمجي بنجاح"
                                )
                            )
                            Toast.makeText(context, "تم إضافة الكود كمشروع نشط في قائمة مشاريعك! 🚀", Toast.LENGTH_LONG).show()
                        }
                    )
                }

                if (isChatLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "جاري التفكير والابتكار...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Message Input Panel
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("اكتب سؤالك أو اطلب كود، تطبيق، موقع...", fontSize = 13.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .testTag("chat_input_text"),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (inputText.isNotBlank() && !isChatLoading) {
                                viewModel.sendChatMessage(inputText)
                                inputText = ""
                            }
                        })
                    )

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank() && !isChatLoading) {
                                viewModel.sendChatMessage(inputText)
                                inputText = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (inputText.isNotBlank() && !isChatLoading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            )
                            .testTag("chat_send_button"),
                        enabled = inputText.isNotBlank() && !isChatLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "إرسال",
                            tint = if (inputText.isNotBlank() && !isChatLoading) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Conversations Sliding List (Overlay - directly inside root Box scope)
        AnimatedVisibility(
            visible = isHistoryOpen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            // Dimmed semi-transparent background Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable { isHistoryOpen = false }
            ) {
                // Sidebar Dialog content Card (Align Start to support RTL properly)
                Card(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(290.dp)
                        .align(Alignment.CenterStart)
                        .clickable(enabled = false) {}, // Intercept click propagation
                    shape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 24.dp, bottomEnd = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Sidebar Header
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
                                    imageVector = Icons.Default.Forum,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "سجل المحادثات 💬",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = { isHistoryOpen = false }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إغلاق",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        // Quick Action Button: New Conversation
                        Button(
                            onClick = {
                                viewModel.startNewChat()
                                isHistoryOpen = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("drawer_new_chat"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("بدء محادثة جديدة ✨", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Conversations List content
                        if (chatSessions.isEmpty()) {
                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChatBubbleOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Text(
                                        text = "سجل محادثاتك فارغ.\nابدأ بطرح فكرتك البرمجية!",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(chatSessions) { session ->
                                        val isSelected = currentSessionId == session.id
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.selectChatSession(session)
                                                    isHistoryOpen = false
                                                }
                                                .testTag("session_item_${session.id}"),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) {
                                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                                }
                                            ),
                                            border = if (isSelected) {
                                                BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                                            } else {
                                                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Icon(
                                                    imageVector = if (isSelected) Icons.Default.ChatBubble else Icons.Default.ChatBubbleOutline,
                                                    contentDescription = null,
                                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                
                                                Spacer(modifier = Modifier.width(8.dp))
                                                
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = session.title,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        textAlign = TextAlign.Start
                                                    )
                                                    
                                                    val formattedDate = remember(session.createdAt) {
                                                        val sdf = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault())
                                                        sdf.format(java.util.Date(session.createdAt))
                                                    }
                                                    Text(
                                                        text = formattedDate,
                                                        fontSize = 9.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                        textAlign = TextAlign.Start
                                                    )
                                                }

                                                IconButton(
                                                    onClick = {
                                                        viewModel.deleteChatSession(session)
                                                        Toast.makeText(context, "تم حذف المحادثة!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(28.dp).testTag("delete_session_${session.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "حذف المحادثة",
                                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                        modifier = Modifier.size(16.dp)
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
    }
}

@Composable
fun ChatBubbleItem(
    message: ChatMessage,
    onAddProjectClick: (String, String) -> Unit
) {
    val isUser = message.isUser
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    }
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val bubbleShape = if (isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 310.dp)
                .background(bubbleColor, bubbleShape)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                if (isUser) {
                    Text(
                        text = message.text,
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 20.sp
                    )
                } else {
                    // Parse Markdown Code blocks dynamically for AI responses
                    val textParts = remember(message.text) { message.text.split("```") }
                    textParts.forEachIndexed { index, part ->
                        if (index % 2 == 1) {
                            // Code Block
                            val lines = part.trim().split("\n")
                            val language = lines.firstOrNull()?.trim() ?: ""
                            val code = if (language.isNotEmpty() && (
                                        language == "html" || language == "kotlin" || 
                                        language == "javascript" || language == "xml" || 
                                        language == "json" || language == "css"
                                    )) {
                                lines.drop(1).joinToString("\n")
                            } else {
                                part
                            }
                            CodeViewerCard(
                                code = code,
                                language = language,
                                onAddProjectClick = onAddProjectClick
                            )
                        } else {
                            // Standard Text block
                            if (part.isNotBlank()) {
                                Text(
                                    text = part.trim(),
                                    color = textColor,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.padding(vertical = 4.dp)
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
fun CodeViewerCard(
    code: String,
    language: String,
    onAddProjectClick: (String, String) -> Unit
) {
    val context = LocalContext.current
    val cleanLanguage = language.ifBlank { "كود برمجي" }.uppercase()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), // Deep editor background
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF38BDF8), CircleShape)
                    )
                    Text(
                        text = cleanLanguage,
                        fontSize = 11.sp,
                        color = Color(0xFF38BDF8), // Code editor cyan accent
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Scrollable code container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = code.trim(),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFE2E8F0),
                    lineHeight = 16.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Sleek Interactive Footer Actions
            Divider(color = Color(0xFF334155), thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Copy Action Chip
                AssistChip(
                    onClick = {
                        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Mubtakir Code", code)
                        clipboardManager.setPrimaryClip(clip)
                        Toast.makeText(context, "تم نسخ الكود بنجاح!", Toast.LENGTH_SHORT).show()
                    },
                    label = { Text("نسخ", fontSize = 10.sp, color = Color.White) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(12.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color(0xFF1E293B)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                )

                // ZIP Download Action Chip
                AssistChip(
                    onClick = {
                        com.example.util.ZipExporter.exportCodeAsZip(context, code, cleanLanguage)
                    },
                    label = { Text("تنزيل ZIP 📦", fontSize = 10.sp, color = Color(0xFF38BDF8)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(12.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color(0xFF1E293B)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f))
                )

                Spacer(modifier = Modifier.weight(1f))

                // Convert to Project Action Chip
                AssistChip(
                    onClick = {
                        onAddProjectClick(code, language)
                    },
                    label = { Text("مشروع", fontSize = 10.sp, color = Color(0xFF34D399)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(12.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color(0xFF065F46)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF047857))
                )
            }
        }
    }
}
