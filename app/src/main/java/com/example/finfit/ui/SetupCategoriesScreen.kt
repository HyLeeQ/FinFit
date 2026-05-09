package com.example.finfit.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Default category data ────────────────────────────────────────────────────

data class SuggestedCategory(val name: String, val emoji: String)

val DEFAULT_EXPENSE_CATS = listOf(
    SuggestedCategory("Thức ăn & Đồ uống", "🍔"),
    SuggestedCategory("Nhà ở",             "🏠"),
    SuggestedCategory("Mua sắm",           "👜"),
    SuggestedCategory("Giao thông",        "🚗"),
    SuggestedCategory("Du lịch",           "✈️"),
    SuggestedCategory("Giải trí",          "🎮"),
    SuggestedCategory("Sức khỏe",          "💊"),
    SuggestedCategory("Thực phẩm",         "🥬"),
    SuggestedCategory("Thú cưng",          "🐾"),
    SuggestedCategory("Giáo dục",          "🎓"),
    SuggestedCategory("Điện tử",           "📱"),
    SuggestedCategory("Làm đẹp",           "💄"),
    SuggestedCategory("Thể thao",          "⚽"),
)

val DEFAULT_INCOME_CATS = listOf(
    SuggestedCategory("Lương",        "💵"),
    SuggestedCategory("Đầu tư",      "📈"),
    SuggestedCategory("Tiền thưởng", "🎁"),
    SuggestedCategory("Kinh doanh",  "💼"),
)

// ─── Emoji picker groups ──────────────────────────────────────────────────────

val EMOJI_GROUPS = listOf(
    "Giao thông & Du lịch" to listOf("☂️","🚲","🚌","⛽","🏨","📍","🅿️","🧳","🚕","✈️","🚗","🚧"),
    "Sức khỏe & Chăm sóc" to listOf("💊","🏥","🩺","🧘","💆","🛡️","🏋️","🌿","💉","🦷","👗","💄"),
    "Nhà & Hóa đơn"        to listOf("🏠","🧊","📃","🧹","💡","🛋️","🌱","📡","🔧","🗑️","🚿","🔑"),
    "Đồ ăn & Đồ uống"      to listOf("🍔","🍜","🍕","☕","🥗","🍱","🥤","🎂","🛒","🛍️","🍣","🥐"),
    "Tài chính & Công việc" to listOf("💵","📈","🎁","💼","💰","🏦","💳","📊","📑","💹","🤝","🖥️"),
    "Mua sắm & Giải trí"   to listOf("👜","🎮","🎬","🎵","📚","🎯","🎪","🛍️","👟","🕹️","🎨","📷"),
    "Gia đình & Giáo dục"  to listOf("👨‍👩‍👧","🎓","📖","✏️","🏫","🧸","🐾","⚽","🎻","🏀","🧩","🎠"),
)

// ─── Main Screen ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SetupCategoriesScreen(
    onBack: () -> Unit,
    onFinish: (
        expenseSelected: Set<String>,
        incomeSelected: Set<String>,
        customExpense: List<Pair<String,String>>,
        customIncome: List<Pair<String,String>>
    ) -> Unit
) {
    val accent = Color(0xFF26B5A0)

    // Selections — all enabled by default
    val expenseSelected = remember { mutableStateListOf(*DEFAULT_EXPENSE_CATS.map { it.name }.toTypedArray()) }
    val incomeSelected  = remember { mutableStateListOf(*DEFAULT_INCOME_CATS.map  { it.name }.toTypedArray()) }
    val customExpense   = remember { mutableStateListOf<Pair<String,String>>() }
    val customIncome    = remember { mutableStateListOf<Pair<String,String>>() }

    // Bottom sheet states
    var showCreateSheet  by remember { mutableStateOf(false) }
    var createForIncome  by remember { mutableStateOf(false) }
    var showEmojiPicker  by remember { mutableStateOf(false) }
    var draftName        by remember { mutableStateOf("") }
    var draftEmoji       by remember { mutableStateOf("✨") }

    if (showEmojiPicker) {
        EmojiPickerSheet(
            onDismiss = { showEmojiPicker = false },
            onEmojiSelected = { emoji ->
                draftEmoji = emoji
                showEmojiPicker = false
            }
        )
    }

    if (showCreateSheet) {
        CreateCategorySheet(
            draftName = draftName,
            draftEmoji = draftEmoji,
            accent = accent,
            onNameChange = { draftName = it },
            onPickEmoji = { showEmojiPicker = true },
            onDismiss = {
                showCreateSheet = false
                draftName = ""; draftEmoji = "✨"
            },
            onCreate = {
                if (draftName.isNotBlank()) {
                    val pair = draftName.trim() to draftEmoji
                    if (createForIncome) customIncome.add(pair)
                    else customExpense.add(pair)
                }
                showCreateSheet = false
                draftName = ""; draftEmoji = "✨"
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FF))
            .statusBarsPadding()
    ) {
        // Top bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Quay lại", tint = Color(0xFF1A2340))
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Text(
                "Chọn danh mục hoặc\ntạo danh mục tùy chỉnh",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF1A2340),
                lineHeight = 32.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Expense categories ────────────────────────────────────
            SectionHeader("Đề xuất chi phí", "💡 Bạn có thể thêm các danh mục phụ sau này")

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DEFAULT_EXPENSE_CATS.forEach { cat ->
                    CategoryChip(
                        emoji = cat.emoji,
                        name = cat.name,
                        selected = cat.name in expenseSelected,
                        accent = accent,
                        onClick = {
                            if (cat.name in expenseSelected) expenseSelected.remove(cat.name)
                            else expenseSelected.add(cat.name)
                        }
                    )
                }
                customExpense.forEach { (name, emoji) ->
                    CategoryChip(
                        emoji = emoji, name = name,
                        selected = true, accent = accent,
                        onClick = { customExpense.removeIf { it.first == name } }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            AddNewButton(accent) {
                createForIncome = false
                showCreateSheet = true
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Income categories ─────────────────────────────────────
            SectionHeader("Đề xuất thu nhập", null)

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DEFAULT_INCOME_CATS.forEach { cat ->
                    CategoryChip(
                        emoji = cat.emoji, name = cat.name,
                        selected = cat.name in incomeSelected,
                        accent = accent,
                        onClick = {
                            if (cat.name in incomeSelected) incomeSelected.remove(cat.name)
                            else incomeSelected.add(cat.name)
                        }
                    )
                }
                customIncome.forEach { (name, emoji) ->
                    CategoryChip(
                        emoji = emoji, name = name,
                        selected = true, accent = accent,
                        onClick = { customIncome.removeIf { it.first == name } }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            AddNewButton(accent) {
                createForIncome = true
                showCreateSheet = true
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Continue button
        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp).navigationBarsPadding()) {
            Button(
                onClick = {
                    onFinish(
                        expenseSelected.toSet(),
                        incomeSelected.toSet(),
                        customExpense.toList(),
                        customIncome.toList()
                    )
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text("Tiếp tục", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// ─── Sub-composables ──────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, hint: String?) {
    Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A2340))
    if (hint != null) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(hint, fontSize = 12.sp, color = Color(0xFF8E9AB0), lineHeight = 17.sp)
    }
}

@Composable
private fun CategoryChip(
    emoji: String,
    name: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) accent else Color(0xFFDDE3F0),
        animationSpec = tween(200), label = "border"
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.08f) else Color.White,
        animationSpec = tween(200), label = "bg"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(emoji, fontSize = 18.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1A2340))
    }
}

@Composable
private fun AddNewButton(accent: Color, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF1A2340))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Thêm mới", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

// ─── Create Category Bottom Sheet ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateCategorySheet(
    draftName: String,
    draftEmoji: String,
    accent: Color,
    onNameChange: (String) -> Unit,
    onPickEmoji: () -> Unit,
    onDismiss: () -> Unit,
    onCreate: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            Text(
                "Tạo tên danh mục và biểu tượng của bạn",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A2340),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Emoji picker button
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.12f))
                        .border(1.5.dp, accent.copy(alpha = 0.4f), CircleShape)
                        .clickable(onClick = onPickEmoji),
                    contentAlignment = Alignment.Center
                ) {
                    Text(draftEmoji, fontSize = 24.sp)
                }

                // Name field
                OutlinedTextField(
                    value = draftName,
                    onValueChange = onNameChange,
                    placeholder = { Text("Tiêu đề", color = Color(0xFFB0BAD0)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = accent,
                        focusedBorderColor = accent,
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onCreate,
                enabled = draftName.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text("Tạo danh mục", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ─── Emoji Picker Bottom Sheet ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmojiPickerSheet(
    onDismiss: () -> Unit,
    onEmojiSelected: (String) -> Unit
) {
    val tealBg = Color(0xFFE6F7F5)
    val tealFg = Color(0xFF26B5A0)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier.navigationBarsPadding()
        ) {
            EMOJI_GROUPS.forEach { (groupName, emojis) ->
                item {
                    Text(
                        groupName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A2340),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(emojis) { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(tealBg)
                                    .clickable { onEmojiSelected(emoji) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 26.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
