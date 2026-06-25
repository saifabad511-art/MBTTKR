package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Project
import com.example.ui.MubtakirViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    viewModel: MubtakirViewModel,
    modifier: Modifier = Modifier
) {
    val completedProjects by viewModel.completedProjects.collectAsState()
    val allProjects by viewModel.allProjects.collectAsState()
    val activeCount by viewModel.activeCount.collectAsState()
    val completedCount by viewModel.completedCount.collectAsState()

    var selectedTechFilter by remember { mutableStateOf<String?>(null) }
    var isAddManualProjectDialogOpen by remember { mutableStateOf(false) }

    // Aggregate technologies from completed projects to display filter chips
    val allTechnologies = remember(completedProjects) {
        completedProjects.flatMap { project ->
            project.technologies.split(",").map { it.trim() }
        }.filter { it.isNotBlank() }.distinct()
    }

    // Calculate average completion time of projects (completedAt - createdAt)
    val averageCompletionTimeText = remember(completedProjects) {
        val durationsInMs = completedProjects.mapNotNull { project ->
            if (project.completedAt != null && project.completedAt > project.createdAt) {
                project.completedAt - project.createdAt
            } else null
        }
        if (durationsInMs.isEmpty()) {
            "لا ينطبق"
        } else {
            val averageMs = durationsInMs.average()
            val totalSeconds = (averageMs / 1000).toLong()
            val totalMinutes = totalSeconds / 60
            val totalHours = totalMinutes / 60
            val totalDays = totalHours / 24

            when {
                totalDays > 0 -> {
                    val remainingHours = totalHours % 24
                    if (remainingHours > 0) {
                        "${totalDays}ي ${remainingHours}س"
                    } else {
                        "$totalDays أيام"
                    }
                }
                totalHours > 0 -> {
                    val remainingMinutes = totalMinutes % 60
                    if (remainingMinutes > 0) {
                        "${totalHours}س ${remainingMinutes}د"
                    } else {
                        "$totalHours ساعة"
                    }
                }
                totalMinutes > 0 -> {
                    "$totalMinutes دقيقة"
                }
                else -> {
                    "ثوانٍ معدودة"
                }
            }
        }
    }

    // Filter projects based on selected tech chip
    val filteredCompletedProjects = remember(completedProjects, selectedTechFilter) {
        val filter = selectedTechFilter
        if (filter == null) {
            completedProjects
        } else {
            completedProjects.filter { project ->
                project.technologies.split(",").any { it.trim().equals(filter, ignoreCase = true) }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- STATS DASHBOARD HEADER (لوحة الإحصائيات التراكمية) ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "لوحة إحصائيات الإنجاز",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "سجل الإبداعات والابتكارات التراكمية لديك",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Row 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Total Projects Started
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircleOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${allProjects.size}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "بدء التنفيذ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Completed projects
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$completedCount",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "مكتملة",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Row 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Active projects
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timeline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$activeCount",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "قيد التنفيذ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Average completion time
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = averageCompletionTimeText,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "متوسط الإكمال",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // --- Tech distribution list ---
                    if (allTechnologies.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "أكثر التقنيات استخداماً في مشاريعك:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // Show visual horizontal bar representation for tech frequencies
                        val techFrequencies = remember(allProjects) {
                            allProjects.flatMap { project ->
                                project.technologies.split(",").map { it.trim() }
                            }.filter { it.isNotBlank() }
                             .groupingBy { it }
                             .eachCount()
                             .toList()
                             .sortedByDescending { it.second }
                             .take(4)
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            techFrequencies.forEach { (techName, count) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = techName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(90.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(8.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        val fraction = count.toFloat() / (techFrequencies.firstOrNull()?.second ?: 1).toFloat()
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(fraction)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.horizontalGradient(
                                                        colors = listOf(
                                                            MaterialTheme.colorScheme.primary,
                                                            MaterialTheme.colorScheme.primaryContainer
                                                        )
                                                    )
                                                )
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "$count",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- BUTTON TO ADD MANUAL PAST PROJECT (تسجيل مشروع سابق يدوياً) ---
        item {
            Button(
                onClick = { isAddManualProjectDialogOpen = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("add_manual_project_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.NoteAdd, contentDescription = null)
                    Text(
                        text = "تسجيل مشروع سابق يدوياً",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // --- FILTER SECTION BY TECHNOLOGY ---
        if (allTechnologies.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "تصنيف وتصفية المشاريع حسب التقنية:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedTechFilter == null,
                                onClick = { selectedTechFilter = null },
                                label = { Text("الكل", fontSize = 11.sp) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        items(allTechnologies) { tech ->
                            val isSelected = selectedTechFilter == tech
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTechFilter = if (isSelected) null else tech },
                                label = { Text(tech, fontSize = 11.sp) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("filter_chip_$tech")
                            )
                        }
                    }
                }
            }
        }

        // --- COMPLETED PAST PROJECTS LIST ---
        item {
            Text(
                text = "المشاريع السابقة والأرشيف",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (filteredCompletedProjects.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (selectedTechFilter != null) "لا توجد مشاريع مصفاة بهذه التقنية" else "الأرشيف فارغ حالياً",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredCompletedProjects, key = { it.id }) { project ->
                CompletedProjectCard(
                    project = project,
                    onDelete = { viewModel.deleteProject(project) },
                    onRestore = { viewModel.restoreProjectToActive(project) }
                )
            }
        }
    }

    // --- DIALOG FOR ADDING MANUAL PAST PROJECT ---
    if (isAddManualProjectDialogOpen) {
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var techInput by remember { mutableStateOf("") }
        var typeInput by remember { mutableStateOf("INVENT") } // "DO", "INVENT", "DESIGN"
        var difficultyInput by remember { mutableStateOf(3) }

        AlertDialog(
            onDismissRequest = { isAddManualProjectDialogOpen = false },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank() && description.isNotBlank()) {
                            viewModel.insertManualProject(
                                title = title.trim(),
                                description = description.trim(),
                                type = typeInput,
                                technologies = if (techInput.isNotBlank()) techInput else "أخرى",
                                difficulty = difficultyInput
                            )
                            isAddManualProjectDialogOpen = false
                        }
                    },
                    modifier = Modifier.testTag("save_manual_project")
                ) {
                    Text("حفظ المشروع")
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddManualProjectDialogOpen = false }) {
                    Text("إلغاء")
                }
            },
            title = {
                Text(
                    text = "تسجيل مشروع سابق منجز",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("اسم المشروع/الفكرة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("وصف المشروع والنتائج") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = techInput,
                        onValueChange = { techInput = it },
                        label = { Text("التقنيات المستخدمة (مفصولة بفاصلة)") },
                        placeholder = { Text("مثال: Kotlin, Room, Figma") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Type selection
                    Text("تصنيف المشروع:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("DO" to "مهمة", "INVENT" to "اختراع", "DESIGN" to "تصميم").forEach { (typeCode, label) ->
                            val isSelected = typeInput == typeCode
                            ElevatedFilterChip(
                                selected = isSelected,
                                onClick = { typeInput = typeCode },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Difficulty selector
                    Text("مستوى الصعوبة المتجشم:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        (1..5).forEach { rate ->
                            val isSelected = difficultyInput == rate
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { difficultyInput = rate },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$rate",
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun CompletedProjectCard(
    project: Project,
    onDelete: () -> Unit,
    onRestore: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }
    val formattedDate = remember(project.completedAt) {
        project.completedAt?.let { formatter.format(Date(it)) } ?: ""
    }

    // Determine visual category container styles based on project type
    val (icon, containerBg, iconColor) = when (project.type) {
        "INVENT" -> Triple(Icons.Default.Lightbulb, Color(0xFFFFD8E4), Color(0xFF31111D))
        "DESIGN" -> Triple(Icons.Default.Brush, Color(0xFFC2E7FF), Color(0xFF001D35))
        else -> Triple(Icons.Default.FlashOn, Color(0xFFEADDFF), Color(0xFF21005D))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("completed_project_card_${project.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category Visual Icon Container
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(containerBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Project Details Center Block
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = project.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Tiny Difficulty Dot / Badge
                    val diffColor = when (project.difficulty) {
                        1 -> Color(0xFF4CAF50)
                        2 -> Color(0xFF8BC34A)
                        3 -> Color(0xFFFF9800)
                        4 -> Color(0xFFF44336)
                        5 -> Color(0xFF9C27B0)
                        else -> MaterialTheme.colorScheme.outline
                    }
                    val diffText = when (project.difficulty) {
                        1 -> "سهل جداً"
                        2 -> "سهل"
                        3 -> "متوسط"
                        4 -> "صعب"
                        5 -> "صعب جداً"
                        else -> "متوسط"
                    }
                    Box(
                        modifier = Modifier
                            .background(diffColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = diffText,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = diffColor
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                
                // Tech list and completion date text
                val formattedTechs = remember(project.technologies) {
                    project.technologies.split(",").map { it.trim() }.joinToString(" • ")
                }
                Text(
                    text = if (formattedTechs.isNotBlank()) "$formattedTechs • مكتمل في $formattedDate" else "مكتمل في $formattedDate",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Right Status and Delete Options Block
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onRestore,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("restore_completed_button_${project.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = "إرجاع المشروع نشط",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("delete_completed_button_${project.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف سجل المشروع",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

