package com.example.finfit.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finfit.data.local.UserProfileData

// ─── Palette ─────────────────────────────────────────────────────────────────

private val Teal   = Color(0xFF26B5A0)
private val Purple = Color(0xFF8B5CF6)
private val Blue   = Color(0xFF3B82F6)
private val Orange = Color(0xFFF97316)
private val Pink   = Color(0xFFEC4899)

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun EditProfileScreen(
    email: String,
    initial: UserProfileData,
    onBack: () -> Unit,
    onSave: (UserProfileData) -> Unit
) {
    // ── Draft state ──────────────────────────────────────────────
    var name         by remember { mutableStateOf(initial.displayName) }
    var ageText      by remember { mutableStateOf(if (initial.age > 0) initial.age.toString() else "") }
    var gender       by remember { mutableStateOf(initial.gender) }
    var heightText   by remember { mutableStateOf(if (initial.heightCm > 0) initial.heightCm.toString() else "") }
    var weightText   by remember { mutableStateOf(if (initial.weightKg > 0f) initial.weightKg.toInt().toString() else "") }
    var activity     by remember { mutableStateOf(initial.activityLevel) }
    var incomeText   by remember { mutableStateOf(if (initial.monthlyIncome > 0) initial.monthlyIncome.toLong().toString() else "") }
    var occupation   by remember { mutableStateOf(initial.occupation) }
    var finGoal      by remember { mutableStateOf(initial.financialGoal) }
    var dietary      by remember { mutableStateOf(initial.dietaryPref) }
    var sleepText    by remember { mutableStateOf(initial.sleepGoalHours.toString()) }
    var waterText    by remember { mutableStateOf(initial.waterGoalLiters.toString()) }

    val scrollState = rememberScrollState()
    val displayLetter = (name.ifBlank { email }).take(1).uppercase()
    val bmi = run {
        val h = heightText.toIntOrNull() ?: 0
        val w = weightText.toIntOrNull() ?: 0
        if (h > 0 && w > 0) w.toFloat() / ((h / 100f) * (h / 100f)) else null
    }

    Scaffold(
        topBar = {
            ProfileTopBar(onBack = onBack, onSave = {
                onSave(UserProfileData(
                    displayName   = name.trim(),
                    age           = ageText.toIntOrNull() ?: 0,
                    gender        = gender,
                    heightCm      = heightText.toIntOrNull() ?: 0,
                    weightKg      = weightText.toFloatOrNull() ?: 0f,
                    activityLevel = activity,
                    monthlyIncome = incomeText.toDoubleOrNull() ?: 0.0,
                    occupation    = occupation.trim(),
                    financialGoal = finGoal,
                    dietaryPref   = dietary,
                    sleepGoalHours= sleepText.toIntOrNull() ?: 8,
                    waterGoalLiters = waterText.toFloatOrNull() ?: 2.0f
                ))
            })
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ── Avatar Header ──────────────────────────────────────
            AvatarHeader(letter = displayLetter, email = email, bmi = bmi)

            Spacer(modifier = Modifier.height(28.dp))

            // ── Section 1: Cơ bản ─────────────────────────────────
            ProfileSectionCard(
                icon = Icons.Default.Person,
                iconColor = Blue,
                title = "Thông tin cơ bản"
            ) {
                ProfileTextField(label = "Tên hiển thị", value = name, onValueChange = { name = it },
                    placeholder = "Nhập tên của bạn", icon = Icons.Default.Badge)
                ProfileTextField(label = "Tuổi", value = ageText, onValueChange = { ageText = it },
                    placeholder = "VD: 25", icon = Icons.Default.Cake,
                    keyboardType = KeyboardType.Number)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Giới tính", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp))
                ChipGroup(
                    options = listOf("male" to "👨 Nam", "female" to "👩 Nữ", "other" to "⚧ Khác"),
                    selected = gender, accent = Blue, onSelect = { gender = it }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Section 2: Thể chất ───────────────────────────────
            ProfileSectionCard(
                icon = Icons.Default.FitnessCenter,
                iconColor = Teal,
                title = "Thể chất & Sức khoẻ"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileTextField(
                        label = "Chiều cao", value = heightText,
                        onValueChange = { heightText = it },
                        placeholder = "170", suffix = "cm",
                        icon = Icons.Default.Height,
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                    ProfileTextField(
                        label = "Cân nặng", value = weightText,
                        onValueChange = { weightText = it },
                        placeholder = "65", suffix = "kg",
                        icon = Icons.Default.MonitorWeight,
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (bmi != null) {
                    BmiChip(bmi = bmi)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Mức độ vận động", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp))
                ChipGroup(
                    options = listOf(
                        "sedentary" to "🪑 Ít",
                        "light"     to "🚶 Nhẹ",
                        "moderate"  to "🏃 Vừa",
                        "active"    to "💪 Cao",
                        "very_active" to "🏋️ Rất cao"
                    ),
                    selected = activity, accent = Teal, onSelect = { activity = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Chế độ ăn", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp))
                ChipGroup(
                    options = listOf(
                        "normal"     to "🍖 Thường",
                        "vegetarian" to "🥗 Chay",
                        "vegan"      to "🌱 Thuần chay"
                    ),
                    selected = dietary, accent = Teal, onSelect = { dietary = it }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileTextField(
                        label = "Mục tiêu giấc ngủ", value = sleepText,
                        onValueChange = { sleepText = it },
                        placeholder = "8", suffix = "giờ",
                        icon = Icons.Default.Bedtime,
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                    ProfileTextField(
                        label = "Uống nước mỗi ngày", value = waterText,
                        onValueChange = { waterText = it },
                        placeholder = "2.0", suffix = "lít",
                        icon = Icons.Default.LocalDrink,
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Section 3: Tài chính ──────────────────────────────
            ProfileSectionCard(
                icon = Icons.Default.AccountBalance,
                iconColor = Purple,
                title = "Tài chính"
            ) {
                ProfileTextField(
                    label = "Thu nhập hàng tháng", value = incomeText,
                    onValueChange = { incomeText = it },
                    placeholder = "10,000,000", suffix = "₫",
                    icon = Icons.Default.AttachMoney,
                    keyboardType = KeyboardType.Number
                )
                ProfileTextField(
                    label = "Nghề nghiệp", value = occupation,
                    onValueChange = { occupation = it },
                    placeholder = "VD: Nhân viên văn phòng",
                    icon = Icons.Default.Work
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Mục tiêu tài chính", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp))
                ChipGroup(
                    options = listOf(
                        "save"      to "💰 Tiết kiệm",
                        "invest"    to "📈 Đầu tư",
                        "buy_home"  to "🏠 Mua nhà/xe",
                        "pay_debt"  to "💳 Trả nợ",
                        "emergency" to "🛡️ Quỹ dự phòng"
                    ),
                    selected = finGoal, accent = Purple, onSelect = { finGoal = it }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Save Button ───────────────────────────────────────
            Button(
                onClick = {
                    onSave(UserProfileData(
                        displayName    = name.trim(),
                        age            = ageText.toIntOrNull() ?: 0,
                        gender         = gender,
                        heightCm       = heightText.toIntOrNull() ?: 0,
                        weightKg       = weightText.toFloatOrNull() ?: 0f,
                        activityLevel  = activity,
                        monthlyIncome  = incomeText.toDoubleOrNull() ?: 0.0,
                        occupation     = occupation.trim(),
                        financialGoal  = finGoal,
                        dietaryPref    = dietary,
                        sleepGoalHours = sleepText.toIntOrNull() ?: 8,
                        waterGoalLiters= waterText.toFloatOrNull() ?: 2.0f
                    ))
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(listOf(Blue, Purple)),
                            RoundedCornerShape(18.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Save, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lưu hồ sơ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

// ─── Sub-composables ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileTopBar(onBack: () -> Unit, onSave: () -> Unit) {
    TopAppBar(
        title = { Text("Hồ sơ cá nhân", fontWeight = FontWeight.Black) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
            }
        },
        actions = {
            TextButton(onClick = onSave) {
                Text("Lưu", color = Blue, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
private fun AvatarHeader(letter: String, email: String, bmi: Float?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFFEC4899))))
            .shadow(8.dp, RoundedCornerShape(28.dp))
            .padding(24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
                    .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(letter, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Hồ sơ của bạn", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                Text(email.substringBefore("@"), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                if (bmi != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "BMI ${String.format("%.1f", bmi)} · ${bmiLabel(bmi)}",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun BmiChip(bmi: Float) {
    val (label, color) = bmiInfo(bmi)
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(Icons.Default.Monitor, null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("BMI ${String.format("%.1f", bmi)} – $label", fontSize = 12.sp, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ProfileSectionCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = iconColor, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(title, fontWeight = FontWeight.Black, fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun ProfileTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    suffix: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(bottom = 12.dp)) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) },
            leadingIcon = { Icon(icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)) },
            suffix = if (suffix.isNotEmpty()) ({ Text(suffix, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }) else null,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipGroup(
    options: List<Pair<String, String>>, // key to label
    selected: String,
    accent: Color,
    onSelect: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 12.dp)
    ) {
        options.forEach { (key, label) ->
            val isSelected = key == selected
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) accent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                animationSpec = tween(200), label = "chip_bg"
            )
            val borderColor by animateColorAsState(
                targetValue = if (isSelected) accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                animationSpec = tween(200), label = "chip_border"
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(bgColor)
                    .border(1.5.dp, borderColor, RoundedCornerShape(20.dp))
                    .clickable { onSelect(key) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(label, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun bmiLabel(bmi: Float) = when {
    bmi < 18.5f -> "Thiếu cân"
    bmi < 25f   -> "Bình thường"
    bmi < 30f   -> "Thừa cân"
    else        -> "Béo phì"
}

private fun bmiInfo(bmi: Float): Pair<String, Color> = when {
    bmi < 18.5f -> "Thiếu cân"  to Color(0xFF3B82F6)
    bmi < 25f   -> "Bình thường" to Color(0xFF22C55E)
    bmi < 30f   -> "Thừa cân"   to Color(0xFFF97316)
    else        -> "Béo phì"    to Color(0xFFEF4444)
}
