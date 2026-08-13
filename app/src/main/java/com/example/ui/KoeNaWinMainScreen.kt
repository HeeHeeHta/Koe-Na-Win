package com.example.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.app.Application
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ChantingDayState
import com.example.data.ChantingEngine
import com.example.viewmodel.KoeNaWinUiState
import com.example.viewmodel.KoeNaWinViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.graphics.asImageBitmap
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KoeNaWinMainScreen(
    viewModel: KoeNaWinViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showProfileDialog by remember { mutableStateOf(false) }
    var showDonatePage by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (showDonatePage) "ကိုးနဝင်း ပံ့ပိုးလှူဒါန်းရန်" else "📿 ကိုးနဝင်းပုတီးစိပ်နည်း",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    if (showDonatePage) {
                        IconButton(onClick = { showDonatePage = false }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back to Home",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    if (!showDonatePage) {
                        IconButton(onClick = { showProfileDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Developer Profile",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (showDonatePage) {
                DonatePage(onBack = { showDonatePage = false })
            } else {
                AnimatedContent(
                    targetState = uiState.isActive,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
                    },
                    label = "ScreenTransition"
                ) { isActive ->
                    if (isActive) {
                        ActiveDashboard(
                            uiState = uiState,
                            viewModel = viewModel,
                            context = context
                        )
                    } else {
                        SetupScreen(
                            onStartCycle = { date, stage, virtueIdx, markPrior ->
                                viewModel.setupNewCycle(date, stage, virtueIdx, markPrior)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showProfileDialog) {
        VibeCoderDetailsDialog(
            onDismiss = { showProfileDialog = false },
            onNavigateToDonate = { showDonatePage = true },
            context = context
        )
    }
}

@Composable
fun ActiveDashboard(
    uiState: KoeNaWinUiState,
    viewModel: KoeNaWinViewModel,
    context: Context
) {
    val scrollState = rememberScrollState()
    var showResetDialog by remember { mutableStateOf(false) }
    var selectedDayToShow by remember { mutableStateOf<ChantingDayState?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val todayState = uiState.todayState

        // 1. Current State Card (Large Highlight)
        if (todayState != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("today_stat_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val brmWeekday = ChantingEngine.getBurmeseWeekday(todayState.dateStr)
                    val engWeekday = ChantingEngine.getEnglishWeekday(todayState.dateStr)

                    if (todayState.isCycleFinished) {
                        Text(
                            text = "🎉 ကိုးနဝင်း ပြီးဆုံးပါပြီ",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "၈၁ ရက်မြောက် ကိုးနဝင်း ပုတီးစိပ်ခြင်း အောင်မြင်စွာ ပြီးဆုံးပါပြီ။ သာဓု သာဓု သာဓု။",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    } else if (todayState.isNotStarted) {
                        Text(
                            text = "⏳ စတင်မည့်ရက် မရောက်သေးပါ",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${todayState.dateStr} တွင် စတင်ပါမည်။",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        // Standard Active Chanting Day
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ယနေ့ စိပ်ရန်",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "ရက် ${todayState.globalDay} / 81",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = todayState.virtueName,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "${todayState.chantCount} ပတ်စိပ်ပါ",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "$brmWeekday ($engWeekday)",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (todayState.position == 5) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)),
                                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🥗 သတ်သတ်လွတ်စားရမည့်ရက်",
                                        color = MaterialTheme.colorScheme.tertiary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val isCompleted = todayState.dateStr in viewModel.completedDays.collectAsStateWithLifecycle().value.map { it.dateString }
                        Button(
                            onClick = { viewModel.toggleDayCompleted(todayState.dateStr) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("today_done_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCompleted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                                contentColor = if (isCompleted) Color.White else Color.Black
                            ),
                            border = if (isCompleted) BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant) else null
                        ) {
                            Icon(
                                imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Check,
                                contentDescription = "Done Status",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isCompleted) "စိပ်ပြီးပါပြီ" else "စိပ်ပြီးပါပြီ (✓ Done)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // 2. Statistics Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "စိပ်ပြီးရက်", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${uiState.completedCount} ရက်",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "လွတ်သွားရက်", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${uiState.missedCount} ရက်",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "တိုးတက်မှု", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${(uiState.progressPercent * 100).toInt()}%",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // 3. Simple Linear Progress Indicator
        LinearProgressIndicator(
            progress = { uiState.progressPercent },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        // 4. 9x9 Chanting Board (ကိုးနဝင်းပုတီးစိပ်ခုံ)
        Text(
            text = "ကိုးနဝင်း (81-Day) ပုတီးစိပ်ဘုတ်",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Info description
                Text(
                    text = "* အဆင့် (၉) ဆင့် ၊ တစ်ဆင့်လျှင် (၉) ရက် ၊ စုစုပေါင်း (၈၁) ရက် ရှိသည်။ အစိမ်းမှာ ပြီးမြောက်၊ အနီမှာ လွတ်သွားသော ရက်များ ဖြစ်သည်။ ခလုပ်များကို နှိပ်၍ အပြောင်းအလဲ လုပ်နိုင်သည်။",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Render 9 Stages
                for (stageNum in 1..9) {
                    val stageDays = uiState.calendarDays.filter { it.stage == stageNum }
                    if (stageDays.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Stage label
                            Text(
                                text = "အဆင့် $stageNum",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(52.dp)
                            )

                            // 9 Beads for this stage
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                stageDays.forEach { dayState ->
                                    val daysDiffFromToday = ChantingEngine.getDaysBetween(dayState.dateStr, ChantingEngine.getTodayDateString())
                                    val isToday = dayState.dateStr == ChantingEngine.getTodayDateString()
                                    val isMissed = !dayState.isCompleted && daysDiffFromToday > 0

                                    // Infinite scale pulse animation for Today bead
                                    val scale = if (isToday) {
                                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                        infiniteTransition.animateFloat(
                                            initialValue = 0.9f,
                                            targetValue = 1.2f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(800, easing = FastOutSlowInEasing),
                                                repeatMode = RepeatMode.Reverse
                                            ),
                                            label = "scale"
                                        ).value
                                    } else 1.0f

                                    // Color styling representing state
                                    val beadColor = when {
                                        dayState.isCompleted -> MaterialTheme.colorScheme.tertiary
                                        isToday -> MaterialTheme.colorScheme.primary
                                        isMissed -> MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .pulseScale(scale)
                                            .clip(CircleShape)
                                            .background(beadColor)
                                            .border(
                                                width = if (isToday) 2.dp else 1.dp,
                                                color = if (isToday) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                selectedDayToShow = dayState
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (dayState.isCompleted) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Completed",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        } else {
                                            Text(
                                                text = "${dayState.position}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (dayState.isCompleted || isMissed || isToday) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
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

        // 5. Notification & Time Config Card
        Text(
            text = "သတိပေးချက် ချိန်ညှိမှုများ",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                var notifyEnabled by remember(uiState.config) {
                    mutableStateOf(uiState.config?.notificationEnabled ?: false)
                }
                var notifyTime by remember(uiState.config) {
                    mutableStateOf(uiState.config?.notificationTime ?: "05:00")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "နေ့စဉ် သတိပေးချက် (Reminder)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "နေ့စဉ် ပုတီးစိပ်ရန် အချိန်ကျလျှင် အသိပေးစနစ်",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = notifyEnabled,
                        onCheckedChange = { isChecked ->
                            notifyEnabled = isChecked
                            viewModel.updateNotificationSettings(isChecked, notifyTime)
                        },
                        modifier = Modifier.testTag("notification_switch")
                    )
                }

                if (notifyEnabled) {
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val timeParts = notifyTime.split(":")
                                val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 5
                                val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

                                TimePickerDialog(
                                    context,
                                    { _, selectedHour, selectedMinute ->
                                        val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                                        notifyTime = formattedTime
                                        viewModel.updateNotificationSettings(notifyEnabled, formattedTime)
                                    },
                                    hour,
                                    minute,
                                    true
                                ).show()
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "အသိပေးချက် ချိန်ညှိရန်",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = notifyTime,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Time",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // 6. Widget Loading Tutorial Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "ဝက်ဂျက် (Widget) တင်ရန် နည်းလမ်း",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ဖုန်း၏ Home Screen ဗလာနေရာတွင် ဖိနှိပ်ထားပြီး Widgets -> 'ကိုးနဝင်းပုတီးစိပ်နည်း' ကိုရွေးချယ်ကာ 4x2 Size ဝက်ဂျက်တင်ပေးပါ။ ဝက်ဂျက်မှာ ယနေ့စိပ်ရမည့်အရာကို အချိန်နှင့်တပြေးညီ ဖော်ပြပေးမည်ဖြစ်ပြီး '✓ ပြီးပြီ' ခလုပ်ကို ဝက်ဂျက်ပေါ်၌ပင် နှိပ်နိုင်သည်။",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // 7. Reset Button Section
        Button(
            onClick = { showResetDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("reset_cycle_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "ယခုစိပ်နေသော ကိုးနဝင်းကို ဖျက်သိမ်းမည်", fontWeight = FontWeight.SemiBold)
        }
    }

    // Toggle day detail showing dialog
    if (selectedDayToShow != null) {
        val currDay = selectedDayToShow!!
        val isCompleted = currDay.dateStr in viewModel.completedDays.collectAsStateWithLifecycle().value.map { it.dateString }

        AlertDialog(
            onDismissRequest = { selectedDayToShow = null },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.toggleDayCompleted(currDay.dateStr)
                        selectedDayToShow = null
                    }
                ) {
                    Text(if (isCompleted) "ပျက်ကွက် ဟုမှတ်မည်" else "စိပ်ပြီးပါပြီ ဟုမှတ်တမ်းတင်မည်")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedDayToShow = null }) {
                    Text("ပိတ်ပါ")
                }
            },
            title = {
                Text("ရက်စွဲ - ${currDay.dateStr}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val stageMm = currDay.stage.toMyanmarDigits()
                    val positionMm = currDay.position.toMyanmarDigits()
                    val chantCountMm = currDay.chantCount.toMyanmarDigits()
                    val globalDayMm = currDay.globalDay.toMyanmarDigits()

                    Text("• စိပ်ရက် - Day $globalDayMm", fontSize = 14.sp)
                    Text("• အဆင့် - $stageMm , $positionMm ရက် မြောက်နေ့", fontSize = 14.sp)
                    Text("• စိပ်ရမည့်ဂုဏ်တော် - ${currDay.virtueName}", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("• အပတ်ရေ - $chantCountMm ပတ်", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    
                    if (currDay.position == 5) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🥗 သတ်သတ်လွတ်စားရမည့်ရက်",
                                color = MaterialTheme.colorScheme.tertiary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isCompleted) "✓ ပြီးမြောက်ပြီးကြောင်း မှတ်သားထားပြီးပါပြီ" else "❌ စိပ်ရန် ကျန်ရှိနေသေးပါသည် (သို့မဟုတ်) လွတ်သွားပါသည်",
                        color = if (isCompleted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetCycle()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("ဟုတ်ကဲ့၊ ဖျက်မည်")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("မလုပ်တော့ပါ")
                }
            },
            title = {
                Text("ပုတီးစိပ်ခြင်းကို ဖျက်သိမ်းမလား။", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("လက်ရှိ ကိုးနဝင်း ၈၁ ရက် ပုတီးစိပ်မှတ်တမ်းအားလုံးကို လုံးဝဖျက်သိမ်းပြီး အသစ်ထပ်မံ စတင်မည်ဖြစ်ကြောင်း သေချာပါသလား။")
            }
        )
    }
}

@Composable
fun SetupScreen(
    onStartCycle: (String, Int, Int, Boolean) -> Unit
) {
    val context = LocalContext.current
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedStage by remember { mutableIntStateOf(1) }
    var selectedVirtueIndex by remember { mutableIntStateOf(2) } // default Start at သမ္မာသမ္ဗုဒ္ဓေါ (2)
    var showPriorDaysConfirmDialog by remember { mutableStateOf(false) }
    var showValidationErrorDialog by remember { mutableStateOf(false) }

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val dateString = selectedDate.format(formatter)

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📿 ကိုးနဝင်းပုတီးစိပ်နည်း ၈၁ ရက်",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ကိုးနဝင်း ဆိုသည်မှာ ဗုဒ္ဓဂုဏ်တော် ၉ ပါးအား အဆင့် ၉ ဆင့် ၊ တစ်ဆင့်လျှင် ၉ ရက် ၊ စုစုပေါင်း ၈၁ ရက်တိုင်တိုင် စဉ်ဆက်မပြတ်အဓိဋ္ဌာန်ပြု ပုတီးစိပ်ဆောင်ရွက်ရခြင်း ဖြစ်ပါသည်။",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Text(
            text = "ပုတီးစိပ် စတင်ပြုလုပ်ရန် ချိန်ညှိမှု",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Starting Date Picker Button
                Column {
                    Text(
                        text = "၁။ စတင်မည့် ရက်စွဲ",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = {
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                                },
                                selectedDate.year,
                                selectedDate.monthValue - 1,
                                selectedDate.dayOfMonth
                            ).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("date_picker_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Date",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = dateString,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                // 2. Starting Stage Selector
                Column {
                    Text(
                        text = "၂။ လက်ရှိ စတင်မည့်အဆင့် (Stage)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Simple grid for stages
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (r in 0..2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (c in 1..3) {
                                    val stageNum = r * 3 + c
                                    val isSelected = selectedStage == stageNum
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) Color.White else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { selectedStage = stageNum }
                                            .testTag("stage_button_$stageNum"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "အဆင့် $stageNum",
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Virtue Selector (based on Stage)
                Column {
                    Text(
                        text = "၃။ လက်ရှိ စိပ်ဆဲ/စတင်မည့် ဂုဏ်တော်",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "* သင်ရွေးချယ်ထားသော အဆင့် $selectedStage ၏ ဂုဏ်တော်များထဲမှ ယနေ့စိပ်မည့် ဂုဏ်တော်ကို ရွေးချယ်ပါ",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val sequence = ChantingEngine.STAGE_SEQUENCES[selectedStage] ?: emptyList()
                    
                    // Display list of 9 virtues in sequence order
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        sequence.forEachIndexed { positionIdx, virtueIdx ->
                            val virtueName = ChantingEngine.VIRTUE_MAP[virtueIdx] ?: ""
                            val isSelected = selectedVirtueIndex == virtueIdx

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedVirtueIndex = virtueIdx }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .testTag("virtue_item_$virtueIdx"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${positionIdx + 1}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.Black else Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = virtueName,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Text(
                                    text = "$virtueIdx ပတ်",
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Action Button: Start Cycle
        Button(
            onClick = {
                val isValid = ChantingEngine.validateCycleConfig(dateString, selectedStage, selectedVirtueIndex)
                if (!isValid) {
                    showValidationErrorDialog = true
                } else {
                    val position = ChantingEngine.findPositionForVirtueInStage(selectedStage, selectedVirtueIndex)
                    val anchorGlobalDayIndex = (selectedStage - 1) * 9 + (position - 1)
                    if (anchorGlobalDayIndex > 0) {
                        showPriorDaysConfirmDialog = true
                    } else {
                        onStartCycle(dateString, selectedStage, selectedVirtueIndex, false)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("start_cycle_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Start",
                tint = Color.Black
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ကိုးနဝင်းပုတီးစိပ် စတင်စောင့်ထိန်းမည်",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        if (showPriorDaysConfirmDialog) {
            val position = ChantingEngine.findPositionForVirtueInStage(selectedStage, selectedVirtueIndex)
            val anchorGlobalDayIndex = (selectedStage - 1) * 9 + (position - 1)

            AlertDialog(
                onDismissRequest = { showPriorDaysConfirmDialog = false },
                title = {
                    Text(
                        text = "ရက်ဟောင်းများ သတ်မှတ်ချက်",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                text = {
                    Text(
                        text = "ယခုစတင်မည့် အဆင့် $selectedStage မတိုင်မီ လွတ်သွားသော ရက်ဟောင်းများ ($anchorGlobalDayIndex ရက်) အားလုံးကို \n\nပုတီးစိပ်ပြီးသား (Completed) အဖြစ် တခါတည်း သတ်မှတ်ပေးမလား သို့မဟုတ် တရက်ချင်းကိုယ်တိုင် သတ်မှတ်မလား။",
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showPriorDaysConfirmDialog = false
                            onStartCycle(dateString, selectedStage, selectedVirtueIndex, true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("ပြီးသားဟုသတ်မှတ်မည်", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showPriorDaysConfirmDialog = false
                            onStartCycle(dateString, selectedStage, selectedVirtueIndex, false)
                        }
                    ) {
                        Text("ကိုယ်တိုင်စိပ်မည်", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }

        if (showValidationErrorDialog) {
            AlertDialog(
                onDismissRequest = { showValidationErrorDialog = false },
                title = {
                    Text(
                        text = "ခွင့်မပြုပါ (မသင့်တော်ပါ)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                },
                text = {
                    Text(
                        text = "ရွေးချယ်ထားသော အဆင့်၊ ဂုဏ်တော် နှင့် နေ့ရက် မကိုက်ညီပါ။ ပြန်လည်စစ်ဆေးပြီး ရွေးချယ်ပါ။",
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showValidationErrorDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("ပြန်လည်ပြင်ဆင်မည်", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

// Extension to scale components easily for the Pulse animation
fun Modifier.pulseScale(scale: Float): Modifier = this.then(
    Modifier.scale(scale)
)

@Composable
fun VibeCoderDetailsDialog(
    onDismiss: () -> Unit,
    onNavigateToDonate: () -> Unit,
    context: Context = LocalContext.current
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Developer",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Vibe Coder Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Name Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("ဖန်တီးသူ (Developer)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Ko Mun (ရွှေထားဝယ်မိုဘိုင်း)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                // Contact Methods
                // Email Item
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Email Address", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("haymunaung24@gmail.com", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(
                        onClick = {
                            val clipService = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipService.setPrimaryClip(android.content.ClipData.newPlainText("Developer Email", "haymunaung24@gmail.com"))
                            Toast.makeText(context, "Email ကူးယူပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Copy Email", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }

                Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                // Phone Item with Copy & Call
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Phone Number", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("+959422202319", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    
                    // Call Button
                    IconButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+959422202319"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "ဖုန်းခေါ်ဆို၍မရပါ", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = "Call", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Copy Phone
                    IconButton(
                        onClick = {
                            val clipService = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipService.setPrimaryClip(android.content.ClipData.newPlainText("Developer Phone", "+959422202319"))
                            Toast.makeText(context, "ဖုန်းနံပါတ် ကူးယူပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Copy Phone", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }

                Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                // Mobile Shop Link
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.app.goo.gl/LuaSqtPuQYo6QvHC8"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "မြေပုံဖွင့်၍မရပါ", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = "Shop", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mobile Shop (Google Maps)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("ရွှေထားဝယ်မိုဘိုင်း ဆိုင်လိပ်စာ", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Navigate", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }

                Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                // Facebook Link
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/share/1NL9iE7r4w/"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Facebook ဖွင့်၍မရပါ", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = "Facebook", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Facebook Profile", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Ko Mun ၏ Facebook စာမျက်နှာ", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Open Link", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Navigation Link: Donate Page (Highlight button)
                Button(
                    onClick = {
                        onDismiss()
                        onNavigateToDonate()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Donate",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("လှူဒါန်းရန် (Donate)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("ပိတ်ပါ", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun DonatePage(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Generate KPay QR bitmap when initialized
    val kpayData = "hQZLQlpQYXlhQE8C8FACEFECMTFXFglCIgIxnSYGEBAfnwgEAQGfJAEwF819e81b8b17a"
    val qrBitmap = remember(kpayData) {
        QrUtils.generateQrCode(kpayData, size = 450)
    }

    val waveDisplay = "9422202319"
    val waveQrBitmap = remember(waveDisplay) {
        QrUtils.generateQrCode(waveDisplay, size = 450)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
                    )
                )
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Core spiritual golden header card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary) // Golden Border
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📿 ကိုးနဝင်း ပုတီးစိပ်နည်း App အား\nပံ့ပိုးလှူဒါန်းရန်",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "ဤ App ကို အသုံးဝင်သည်ဟု ယူဆပါက နောက်ထပ် Update များနှင့် Feature အသစ်များ vibe coder မှဖန်တီးနိုင်ရန် မိမိဆန္ဒအလျောက် လှူဒါန်းနိုင်ပါသည်။",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }

        // Section header for payment methods
        Text(
            text = "ပံ့ပိုးလှူဒါန်းနိုင်သော စနစ်များ (Payment Methods)",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.Start)
        )

        // 1. KBZPay QR Code Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "KPay QR",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0066CC)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                if (qrBitmap != null) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(3.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier.size(200.dp)
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "KBZPay Donation QR Code",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "QR Code အား စကန်ဖတ်၍ လှူဒါန်းနိုင်ပါသည်",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .background(Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("QR Code ပြင်ဆင်နေသည်...", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Actions: Save QR Image, Share QR, Copy QR Data
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Save QR Image Button
                    Button(
                        onClick = {
                            if (qrBitmap != null) {
                                QrUtils.saveQrToGallery(context, qrBitmap, "koenawin_kbzpay_qr")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("သိမ်းဆည်းမည်", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Share QR Button
                    Button(
                        onClick = {
                            if (qrBitmap != null) {
                                QrUtils.shareQrImage(context, qrBitmap, "koenawin_kbzpay_qr")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("မျှဝေမည်", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. WavePay Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "WavePay QR",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFC107)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                if (waveQrBitmap != null) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(3.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier.size(200.dp)
                    ) {
                        Image(
                            bitmap = waveQrBitmap.asImageBitmap(),
                            contentDescription = "WavePay Donation QR Code",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "QR Code အား စကန်ဖတ်၍ လှူဒါန်းနိုင်ပါသည်",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .background(Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("QR Code ပြင်ဆင်နေသည်...", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Displays readable Phone Number
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "လှူဒါန်းနိုင်မည့်ဖုန်းနံပါတ်",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = waveDisplay,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions: Copy Number & Share Number
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Copy Phone Number Button
                    Button(
                        onClick = {
                            val clipService = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipService.setPrimaryClip(android.content.ClipData.newPlainText("WavePay Number", waveDisplay))
                            Toast.makeText(context, "WavePay ဖုန်းနံပါတ် ကူးယူပြီးပါပြီ။", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Copy Number",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("နံပါတ် ကော်ပီယူမည်", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Share Phone Number Button
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "ကိုးနဝင်း ပုတီးစိပ်နည်း App ပံ့ပိုးလှူဒါန်းရန် WavePay ဖုန်းနံပါတ်: $waveDisplay")
                            }
                            context.startActivity(Intent.createChooser(intent, "ဖုန်းနံပါတ်မျှဝေရန်"))
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Number",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("နံပါတ် မျှဝေမည်", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "သာဓု သာဓု သာဓု 🙏",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun Int.toMyanmarDigits(): String {
    val myanmarDigits = listOf('၀', '၁', '၂', '၃', '၄', '၅', '၆', '၇', '၈', '၉')
    return this.toString().map { char ->
        if (char.isDigit()) myanmarDigits[char - '0'] else char
    }.joinToString("")
}
