package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.ui.screens.ActiveProjectsScreen
import com.example.ui.screens.ArchiveScreen
import com.example.ui.screens.InnovationHubScreen
import com.example.ui.components.WelcomeMotivationDialog
import com.example.ui.components.ConfettiCelebration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MubtakirApp(
    viewModel: MubtakirViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showReminderDialog by rememberSaveable { mutableStateOf(true) }
    val context = LocalContext.current

    val isSyncing by viewModel.isSyncing.collectAsState()
    val cloudSyncEnabled by viewModel.cloudSyncEnabled.collectAsState()
    val confettiTrigger by viewModel.confettiTrigger.collectAsState()

    if (showReminderDialog) {
        WelcomeMotivationDialog(
            onDismiss = { showReminderDialog = false },
            onAction = {
                showReminderDialog = false
                selectedTab = 1 // Switch to projects tab
            },
            onSendNotification = {
                com.example.util.NotificationHelper.sendDailyGoalsNotification(context)
                showReminderDialog = false
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("إعادة تهيئة التطبيق ومسح البيانات 🧼") },
            text = { Text("هل أنت متأكد من رغبتك في حذف كافة المشاريع، سجل المحادثات، والبيانات والبدء من النسخة الأصلية؟ لا يمكن التراجع عن هذا الإجراء.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetAllData()
                        showResetDialog = false
                    }
                ) {
                    Text("نعم، احذف وابدأ جديداً", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Detect if the screen dimensions match a tablet/iPad (width >= 600dp)
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    // Force RTL layout direction for Arabic visual flow
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isTablet) {
            // High fidelity Tablet / iPad layout: Side Navigation Rail + Centered bounds content
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier.testTag("side_navigation_rail"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    header = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "مُبتكِر الذكي",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                ) {
                    Spacer(modifier = Modifier.weight(1f))

                    NavigationRailItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = { Text("ساحة الابتكار", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "ساحة الابتكار الذكي"
                            )
                        },
                        modifier = Modifier.testTag("tab_generator")
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    NavigationRailItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        label = { Text("مشاريعي", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Timeline,
                                contentDescription = "مشاريعي"
                            )
                        },
                        modifier = Modifier.testTag("tab_active_projects")
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    NavigationRailItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        label = { Text("الأرشيف", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "الأرشيف"
                            )
                        },
                        modifier = Modifier.testTag("tab_archive")
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Theme toggle & Sync indicators inside Rail footer
                    val isDarkModeOpt by viewModel.isDarkMode.collectAsState()
                    val systemInDark = isSystemInDarkTheme()
                    val isDark = isDarkModeOpt ?: systemInDark

                    IconButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "إعادة تهيئة التطبيق",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }

                    IconButton(
                        onClick = { viewModel.toggleTheme(systemInDark) },
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "تغيير المظهر",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .padding(bottom = 24.dp)
                            .clip(CircleShape)
                            .clickable(enabled = cloudSyncEnabled && !isSyncing) {
                                viewModel.triggerCloudSync()
                            }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = if (cloudSyncEnabled) Icons.Default.CloudQueue else Icons.Default.CloudOff,
                                contentDescription = "المزامنة السحابية",
                                tint = if (cloudSyncEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Main Content Frame (constrained width to avoid stretched line items)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Scaffold(
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = {
                                    Text(
                                        text = when (selectedTab) {
                                            0 -> "ساحة الابتكار والمساعد الذكي"
                                            1 -> "المشاريع قيد التنفيذ والخطوات البرمجية"
                                            else -> "الأرشيف ولوحة التكريم"
                                        },
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp
                                    )
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            // Centered layout block for a elegant iPad/Tablet UI
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .widthIn(max = 850.dp)
                            ) {
                                when (selectedTab) {
                                    0 -> InnovationHubScreen(viewModel = viewModel)
                                    1 -> ActiveProjectsScreen(viewModel = viewModel)
                                    2 -> ArchiveScreen(viewModel = viewModel)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Traditional Mobile phone Layout
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "مُبتكِر الذكي",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        actions = {
                            val isDarkModeOpt by viewModel.isDarkMode.collectAsState()
                            val systemInDark = isSystemInDarkTheme()
                            val isDark = isDarkModeOpt ?: systemInDark

                            IconButton(
                                onClick = { showResetDialog = true },
                                modifier = Modifier.testTag("app_reset_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = "إعادة تهيئة التطبيق",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }

                            IconButton(
                                onClick = { viewModel.toggleTheme(systemInDark) },
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "تغيير المظهر",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Cloud sync indicator icon in Top Bar
                            Box(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .clip(CircleShape)
                                    .clickable(enabled = cloudSyncEnabled && !isSyncing) {
                                        viewModel.triggerCloudSync()
                                    }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (cloudSyncEnabled) Icons.Default.CloudQueue else Icons.Default.CloudOff,
                                            contentDescription = "مزامنة السحابية",
                                            tint = if (cloudSyncEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        if (cloudSyncEnabled) {
                                            // Pulsing small success dot
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF4CAF50))
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(
                        modifier = Modifier.testTag("bottom_nav_bar")
                    ) {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            label = { Text("ساحة الابتكار", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "ساحة الابتكار الذكي"
                                )
                            },
                            modifier = Modifier.testTag("tab_generator")
                        )

                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            label = { Text("مشاريعي", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Timeline,
                                    contentDescription = "مشاريعي"
                                )
                            },
                            modifier = Modifier.testTag("tab_active_projects")
                        )

                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            label = { Text("الأرشيف", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "الأرشيف"
                                )
                            },
                            modifier = Modifier.testTag("tab_archive")
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    when (selectedTab) {
                        0 -> InnovationHubScreen(viewModel = viewModel)
                        1 -> ActiveProjectsScreen(viewModel = viewModel)
                        2 -> ArchiveScreen(viewModel = viewModel)
                    }
                }
            }
        }
        
        ConfettiCelebration(trigger = confettiTrigger)
    }
}
}
