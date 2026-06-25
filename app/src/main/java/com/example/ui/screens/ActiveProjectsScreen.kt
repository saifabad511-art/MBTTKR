package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Project
import com.example.ui.MubtakirViewModel
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveProjectsScreen(
    viewModel: MubtakirViewModel,
    modifier: Modifier = Modifier
) {
    val activeProjects by viewModel.activeProjects.collectAsState()
    val isGeneratingImage by viewModel.isGeneratingImage.collectAsState()

    var projectWithActiveReminderPicker by remember { mutableStateOf<Project?>(null) }
    var isAddManualProjectDialogOpen by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        if (activeProjects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                        modifier = Modifier.size(96.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Timeline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "لا توجد مشاريع قيد التنفيذ حالياً",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "اذهب إلى شاشة توليد الأفكار، أو انقر على الزر العائم في الأسفل لإضافة مشروعك الخاص لتبدأ بتتبعه يومياً!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Screen Title
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "مشاريعي قيد التنفيذ",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "تتبع تقدمك اليومي وحدد مواعيد التذكير بالعمل",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Badge(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = "${activeProjects.size} مشاريع",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Projects List
                items(activeProjects, key = { it.id }) { project ->
                    ActiveProjectCard(
                        project = project,
                        isGeneratingImage = isGeneratingImage,
                        onStepToggle = { stepIndex, isChecked ->
                            viewModel.updateProjectProgress(project, stepIndex, isChecked)
                        },
                        onReminderClick = {
                            projectWithActiveReminderPicker = project
                        },
                        onDelete = {
                            viewModel.deleteProject(project)
                        },
                        onGenerateImage = {
                            viewModel.generateImageForProject(project)
                        }
                    )
                }
            }
        }

        // Beautiful FAB for manual project addition
        FloatingActionButton(
            onClick = { isAddManualProjectDialogOpen = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("fab_add_project"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "إضافة مشروع جديد يدوياً"
            )
        }
    }

    // Custom Elegant Dialog for selecting Reminder time
    if (projectWithActiveReminderPicker != null) {
        val targetProject = projectWithActiveReminderPicker!!
        var selectedHour by remember { mutableStateOf(18) }
        var selectedMinute by remember { mutableStateOf(0) }

        // Parse existing reminder time if possible
        LaunchedEffect(targetProject) {
            targetProject.reminderTime?.let { timeStr ->
                val parts = timeStr.split(":")
                if (parts.size == 2) {
                    selectedHour = parts[0].toIntOrNull() ?: 18
                    selectedMinute = parts[1].toIntOrNull() ?: 0
                }
            }
        }

        AlertDialog(
            onDismissRequest = { projectWithActiveReminderPicker = null },
            confirmButton = {
                Button(
                    onClick = {
                        val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                        viewModel.updateReminderTime(targetProject, formattedTime)
                        projectWithActiveReminderPicker = null
                    },
                    modifier = Modifier.testTag("confirm_reminder_button")
                ) {
                    Text("حفظ وتفعيل")
                }
            },
            dismissButton = {
                TextButton(onClick = { projectWithActiveReminderPicker = null }) {
                    Text("إلغاء")
                }
            },
            title = {
                Text(
                    text = "تحديد وقت تذكير العمل",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "اختر الموعد اليومي الذي ترغب بأن يقوم التطبيق بتذكيرك للعمل فيه على مشروع:\n\"${targetProject.title}\"",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Right
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Simple Hour & Minute spinners styled beautifully
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Hour spinner
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("الساعة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { selectedHour = (selectedHour + 1) % 24 }) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "زيادة")
                                }
                                Text(
                                    text = String.format("%02d", selectedHour),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.width(40.dp),
                                    textAlign = TextAlign.Center
                                )
                                IconButton(onClick = { selectedHour = (selectedHour + 23) % 24 }) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "تقليل")
                                }
                            }
                        }

                        Text(":", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)

                        // Minute spinner
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("الدقيقة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { selectedMinute = (selectedMinute + 5) % 60 }) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "زيادة")
                                }
                                Text(
                                    text = String.format("%02d", selectedMinute),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.width(40.dp),
                                    textAlign = TextAlign.Center
                                )
                                IconButton(onClick = { selectedMinute = (selectedMinute + 55) % 60 }) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "تقليل")
                                }
                            }
                        }
                    }
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Custom Elegant Dialog for adding Active manual project
    if (isAddManualProjectDialogOpen) {
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var techInput by remember { mutableStateOf("") }
        var typeInput by remember { mutableStateOf("INVENT") } // "DO", "INVENT", "DESIGN"
        var difficultyInput by remember { mutableStateOf(3) }
        var stepsList by remember { mutableStateOf(listOf("دراسة متطلبات الفكرة", "تصميم الواجهات وبناء منطق العمل", "الاختبار الشامل والتأكيد")) }
        var newStepText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { isAddManualProjectDialogOpen = false },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank() && description.isNotBlank()) {
                            viewModel.insertActiveManualProject(
                                title = title.trim(),
                                description = description.trim(),
                                type = typeInput,
                                technologies = if (techInput.isNotBlank()) techInput else "أخرى",
                                difficulty = difficultyInput,
                                stepsList = stepsList
                            )
                            isAddManualProjectDialogOpen = false
                        }
                    },
                    modifier = Modifier.testTag("save_active_project_confirm")
                ) {
                    Text("إضافة المشروع")
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddManualProjectDialogOpen = false }) {
                    Text("إلغاء")
                }
            },
            title = {
                Text(
                    text = "ابتكار وإضافة مشروع نشط جديد 🚀",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("اسم المشروع/الفكرة") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("وصف فكرة المشروع") },
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = techInput,
                            onValueChange = { techInput = it },
                            label = { Text("التقنيات (مفصولة بفاصلة)") },
                            placeholder = { Text("مثال: Kotlin, Jetpack Compose, Room") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Category selection
                    item {
                        Text("تصنيف الابتكار:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("DO" to "مهمة عمل", "INVENT" to "اختراع تقني", "DESIGN" to "تصميم واجهات").forEach { (typeCode, label) ->
                                val isSelected = typeInput == typeCode
                                ElevatedFilterChip(
                                    selected = isSelected,
                                    onClick = { typeInput = typeCode },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    // Difficulty selector
                    item {
                        Text("مستوى الصعوبة المتوقع:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

                    // Steps and milestones
                    item {
                        Text("مراحل وخطوات التنفيذ اليومية:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    items(stepsList) { step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = step, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "حذف خطوة",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable {
                                        stepsList = stepsList.filter { it != step }
                                    }
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newStepText,
                                onValueChange = { newStepText = it },
                                label = { Text("إضافة خطوة جديدة") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    if (newStepText.isNotBlank()) {
                                        stepsList = stepsList + newStepText.trim()
                                        newStepText = ""
                                    }
                                },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                    .size(40.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "إضافة خطوة", tint = MaterialTheme.colorScheme.onPrimaryContainer)
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
fun ActiveProjectCard(
    project: Project,
    isGeneratingImage: Boolean,
    onStepToggle: (Int, Boolean) -> Unit,
    onReminderClick: () -> Unit,
    onDelete: () -> Unit,
    onGenerateImage: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("project_card_${project.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Type tag, reminder status & delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badges Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Type Badge
                    val (typeLabel, badgeColor) = when (project.type) {
                        "DO" -> Pair("مهمة اليوم", MaterialTheme.colorScheme.primary)
                        "INVENT" -> Pair("اختراع", MaterialTheme.colorScheme.secondary)
                        "DESIGN" -> Pair("تصميم", MaterialTheme.colorScheme.tertiary)
                        else -> Pair("مشروع", MaterialTheme.colorScheme.outline)
                    }
                    Box(
                        modifier = Modifier
                            .background(badgeColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = typeLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }

                    // Difficulty Badge
                    val diffColor = when (project.difficulty) {
                        1 -> Color(0xFF4CAF50)
                        2 -> Color(0xFF8BC34A)
                        3 -> Color(0xFFFF9800)
                        4 -> Color(0xFFF44336)
                        5 -> Color(0xFF9C27B0)
                        else -> MaterialTheme.colorScheme.outline
                    }
                    val diffText = when (project.difficulty) {
                        1 -> "سهل"
                        2 -> "ميسّر"
                        3 -> "متوسط"
                        4 -> "صعب"
                        5 -> "معقد"
                        else -> "متوسط"
                    }
                    Box(
                        modifier = Modifier
                            .background(diffColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = diffText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = diffColor
                        )
                    }
                }

                // Delete Action & Reminder setting
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Custom Reminder status
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onReminderClick() }
                            .background(
                                if (project.reminderTime != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                else Color.Transparent
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "تنبيه العمل",
                            tint = if (project.reminderTime != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = project.reminderTime ?: "تنبيه",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (project.reminderTime != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp).testTag("delete_project_button_${project.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف المشروع",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // --- Project Image / Generate Image Panel ---
            if (project.imageUrl != null) {
                Spacer(modifier = Modifier.height(8.dp))
                AsyncImage(
                    model = project.imageUrl,
                    contentDescription = "صورة المشروع",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                if (isGeneratingImage) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text(
                                text = "جاري توليد الصورة الفنية...",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
                            .clickable { onGenerateImage() }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "توليد صورة تعبيرية للمشروع (Imagen)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = project.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Description
            Text(
                text = project.description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp,
                maxLines = if (isExpanded) 10 else 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar and percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "نسبة التقدم المنجز:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${project.progress}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { project.progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Expand Steps collapse trigger button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { isExpanded = !isExpanded },
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Checklist,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "خطوات الإنجاز اليومي وتتبع التقدم",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Expanded checklist steps
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val stepsList = project.steps.split("|")
                    val completedIndices = if (project.completedSteps.isBlank()) {
                        emptyList()
                    } else {
                        project.completedSteps.split("|")
                    }

                    stepsList.forEachIndexed { index, step ->
                        val isChecked = completedIndices.contains(index.toString())
                        
                        // Motivating scale, color, and alpha animations on checking steps
                        val scale by animateFloatAsState(
                            targetValue = if (isChecked) 0.97f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                        val opacity by animateFloatAsState(
                            targetValue = if (isChecked) 0.55f else 1f,
                            animationSpec = tween(durationMillis = 250)
                        )
                        val backgroundColor by animateColorAsState(
                            targetValue = if (isChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            else Color.Transparent,
                            animationSpec = tween(durationMillis = 250)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(scale)
                                .clip(RoundedCornerShape(10.dp))
                                .background(backgroundColor)
                                .clickable { onStepToggle(index, !isChecked) }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { onStepToggle(index, it) },
                                modifier = Modifier
                                    .testTag("step_checkbox_${project.id}_$index")
                                    .scale(if (isChecked) 1.05f else 1.0f)
                            )

                            Text(
                                text = step,
                                fontSize = 12.sp,
                                color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .scale(if (isChecked) 0.99f else 1f),
                                lineHeight = 16.sp,
                                textDecoration = if (isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                            )
                        }
                    }
                }
            }
        }
    }
}
