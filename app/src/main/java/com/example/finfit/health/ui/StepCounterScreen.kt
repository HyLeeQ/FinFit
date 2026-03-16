package com.example.finfit.health.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.finfit.health.repository.StepCounterManager

@Composable
fun StepCounterScreen(userEmail: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val stepManager = remember { StepCounterManager(context) }
    val todaySteps by stepManager.todaySteps.collectAsState()

    var hasPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasPermission = isGranted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }

    DisposableEffect(hasPermission) {
        if (hasPermission) {
            stepManager.startListening()
        }
        onDispose {
            stepManager.stopListening()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        HealthHeaderSection(
            title = "Đếm bước chân",
            userEmail = userEmail,
            showBackButton = true,
            onBackClick = onBack
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (hasPermission) {
                    Text(
                        text = "Steps Today",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "$todaySteps steps",
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "Vui lòng cấp quyền theo dõi vận động để sử dụng tính năng này",
                        modifier = Modifier.padding(32.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
