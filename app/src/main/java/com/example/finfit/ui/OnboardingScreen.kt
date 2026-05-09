package com.example.finfit.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// ─── Data ────────────────────────────────────────────────────────────────────

private data class OnboardingPage(
    val title: String,
    val subtitle: String,
    val accentColor: Color,
    val type: PageType
)

private enum class PageType {
    PROBLEM, ADD_EXPENSE, SCAN_RECEIPT, SMART_ANALYSIS, CONTROL
}

private val onboardingPages = listOf(
    OnboardingPage(
        title = "Tiền Biến Mất\nTrước Khi Bạn Nhận Ra?",
        subtitle = "Bạn quên theo dõi chi phí, hóa đơn ngày càng nhiều và bạn không bao giờ biết chắc tiền của mình thực sự đi về đâu.",
        accentColor = Color(0xFFFF6B6B),
        type = PageType.PROBLEM
    ),
    OnboardingPage(
        title = "Thêm Chi Tiêu\nDễ Dàng",
        subtitle = "Ghi lại chi tiêu bằng văn bản hoặc giọng nói – nhanh, đơn giản và đầy thú vị với trợ lý AI thông minh.",
        accentColor = Color(0xFF6C63FF),
        type = PageType.ADD_EXPENSE
    ),
    OnboardingPage(
        title = "Quét Hóa Đơn\nNgay Lập Tức",
        subtitle = "Chụp ảnh hóa đơn của bạn và để FinFit tự động phân loại mọi chi tiêu cho bạn.",
        accentColor = Color(0xFF26B5A0),
        type = PageType.SCAN_RECEIPT
    ),
    OnboardingPage(
        title = "Nhận Phân Tích\nThông Minh",
        subtitle = "Phân tích thói quen chi tiêu của bạn và nhận lời khuyên ngân sách cá nhân hóa từ AI.",
        accentColor = Color(0xFFF4A261),
        type = PageType.SMART_ANALYSIS
    ),
    OnboardingPage(
        title = "Kiểm Soát Tiền\nCủa Bạn Ngay Hôm Nay",
        subtitle = "Theo dõi chi phí trong vài giây, xem tiền của bạn được chi vào đâu và kiểm soát ngân sách mà không bị căng thẳng.",
        accentColor = Color(0xFF2ECC71),
        type = PageType.CONTROL
    )
)

// ─── Main Screen ─────────────────────────────────────────────────────────────

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState { onboardingPages.size }
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == onboardingPages.size - 1

    val currentPage = onboardingPages[pagerState.currentPage]

    val bgColor by animateColorAsState(
        targetValue = currentPage.accentColor.copy(alpha = 0.06f),
        animationSpec = tween(600),
        label = "bg"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FF))
    ) {
        // Subtle tinted background behind illustration area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .background(bgColor)
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Skip button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (!isLastPage) {
                    TextButton(onClick = onFinished) {
                        Text(
                            text = "Bỏ qua",
                            color = Color(0xFF8E9AB0),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                OnboardingPageContent(page = onboardingPages[page])
            }

            // Bottom navigation area
            OnboardingBottomBar(
                pageCount = onboardingPages.size,
                currentPage = pagerState.currentPage,
                accentColor = currentPage.accentColor,
                isLastPage = isLastPage,
                onNext = {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                },
                onDone = onFinished
            )
        }
    }
}

// ─── Page Content ─────────────────────────────────────────────────────────────

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Illustration
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            PageIllustration(page = page)
        }

        // Text block
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text(
                text = page.title,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF1A2340),
                textAlign = TextAlign.Center,
                lineHeight = 34.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = page.subtitle,
                fontSize = 15.sp,
                color = Color(0xFF6B7897),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

// ─── Illustrations ────────────────────────────────────────────────────────────

@Composable
private fun PageIllustration(page: OnboardingPage) {
    when (page.type) {
        PageType.PROBLEM -> ProblemIllustration(page.accentColor)
        PageType.ADD_EXPENSE -> AddExpenseIllustration(page.accentColor)
        PageType.SCAN_RECEIPT -> ScanReceiptIllustration(page.accentColor)
        PageType.SMART_ANALYSIS -> SmartAnalysisIllustration(page.accentColor)
        PageType.CONTROL -> ControlIllustration(page.accentColor)
    }
}

@Composable
private fun ProblemIllustration(accent: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val floatY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -12f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "floatY"
    )

    Box(contentAlignment = Alignment.Center) {
        // Central circle
        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(y = floatY.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(accent.copy(alpha = 0.18f), accent.copy(alpha = 0.04f))
                    )
                )
                .border(2.dp, accent.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("😟", fontSize = 80.sp)
        }

        // Floating question marks — [x, y, scale]
        val qMarks = listOf(
            floatArrayOf(-90f, -40f, 0.8f),
            floatArrayOf( 80f, -60f, 0.6f),
            floatArrayOf(-70f,  50f, 0.55f),
            floatArrayOf( 90f,  40f, 0.75f),
        )
        qMarks.forEach { pos ->
            Text(
                text = "❓",
                fontSize = 28.sp,
                modifier = Modifier
                    .offset(x = pos[0].dp, y = (pos[1] + floatY * 0.5f).dp)
                    .scale(pos[2])
                    .alpha(0.7f)
            )
        }
    }
}

@Composable
private fun AddExpenseIllustration(accent: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.97f, targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Phone frame mockup
        Box(
            modifier = Modifier
                .scale(scale)
                .width(240.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE8ECF4), RoundedCornerShape(20.dp))
                .shadow(8.dp, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // User message bubble
                Box(modifier = Modifier.align(Alignment.End)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp))
                            .background(accent.copy(alpha = 0.15f))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("bữa tối 80k tối qua", fontSize = 13.sp, color = Color(0xFF1A2340), fontWeight = FontWeight.Medium)
                    }
                }
                // AI reply
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier.size(28.dp).clip(CircleShape).background(accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                            .background(Color(0xFFF1F3FA))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("Đã ghi! Ăn uống 80.000đ 🍜", fontSize = 12.sp, color = Color(0xFF1A2340))
                    }
                }
                // Transaction card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.08f))
                        .border(1.dp, accent.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🍜", fontSize = 22.sp)
                            Column {
                                Text("Ăn uống", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A2340))
                                Text("bữa tối", fontSize = 10.sp, color = Color(0xFF8E9AB0))
                            }
                        }
                        Text("80.000đ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accent)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanReceiptIllustration(accent: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 130f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
        label = "scanY"
    )

    Box(contentAlignment = Alignment.Center) {
        // Receipt mockup
        Box(
            modifier = Modifier
                .width(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE8ECF4), RoundedCornerShape(12.dp))
                .shadow(6.dp, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("🏪 Siêu Thị ABC", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A2340), modifier = Modifier.fillMaxWidth())
                Divider(color = Color(0xFFE8ECF4))
                listOf("Sữa tươi" to "32.000đ", "Bánh mì" to "15.000đ", "Rau xanh" to "28.000đ").forEach { (item, price) ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(item, fontSize = 10.sp, color = Color(0xFF6B7897))
                        Text(price, fontSize = 10.sp, color = Color(0xFF1A2340), fontWeight = FontWeight.Medium)
                    }
                }
                Divider(color = Color(0xFFE8ECF4))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tổng", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A2340))
                    Text("75.000đ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accent)
                }
            }
        }

        // Scan line
        Box(
            modifier = Modifier
                .width(180.dp)
                .height(2.dp)
                .offset(y = (scanY - 65).dp)
                .background(
                    Brush.horizontalGradient(listOf(Color.Transparent, accent, accent, Color.Transparent))
                )
                .alpha(0.8f)
        )

    }
}

@Composable
private fun SmartAnalysisIllustration(accent: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "chart")
    val barScale by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "bar"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Donut chart mockup
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(
                    Brush.sweepGradient(
                        listOf(accent, Color(0xFFF4A261), Color(0xFF6C63FF), Color(0xFF26B5A0), accent)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF8F9FF)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📊", fontSize = 28.sp)
                    Text("AI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accent)
                }
            }
        }

        // Legend chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                Triple("Ăn uống", accent, "42%"),
                Triple("Nhà ở", Color(0xFF6C63FF), "30%"),
                Triple("Khác", Color(0xFFF4A261), "28%"),
            ).forEach { (label, color, pct) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(color.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("$label $pct", fontSize = 10.sp, color = Color(0xFF1A2340), fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun ControlIllustration(accent: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "float2")
    val floatY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -10f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "float2Y"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .offset(y = floatY.dp)
                .size(150.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(accent.copy(alpha = 0.22f), accent.copy(alpha = 0.04f))
                    )
                )
                .border(2.dp, accent.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("🎯", fontSize = 72.sp)
        }

        // Stats row
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(
                Triple("💰", "Tiết kiệm", "+2.4tr"),
                Triple("📉", "Chi tiêu", "-1.2tr"),
                Triple("🏆", "Mục tiêu", "80%"),
            ).forEach { (emoji, label, value) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE8ECF4), RoundedCornerShape(14.dp))
                        .shadow(4.dp, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(emoji, fontSize = 20.sp)
                    Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accent)
                    Text(label, fontSize = 9.sp, color = Color(0xFF8E9AB0))
                }
            }
        }
    }
}

// ─── Bottom Bar ───────────────────────────────────────────────────────────────

@Composable
private fun OnboardingBottomBar(
    pageCount: Int,
    currentPage: Int,
    accentColor: Color,
    isLastPage: Boolean,
    onNext: () -> Unit,
    onDone: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 32.dp, vertical = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Dot indicators
        DotsIndicator(
            total = pageCount,
            current = currentPage,
            accent = accentColor
        )

        // Next / Done FAB
        val fabColor by animateColorAsState(
            targetValue = accentColor,
            animationSpec = tween(500),
            label = "fabColor"
        )

        FloatingActionButton(
            onClick = if (isLastPage) onDone else onNext,
            containerColor = fabColor,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .size(58.dp)
                .shadow(8.dp, CircleShape)
        ) {
            Icon(
                imageVector = if (isLastPage) Icons.Default.Check else Icons.Default.ArrowForward,
                contentDescription = if (isLastPage) "Bắt đầu" else "Tiếp theo",
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun DotsIndicator(total: Int, current: Int, accent: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { index ->
            val isSelected = index == current
            val width by animateDpAsState(
                targetValue = if (isSelected) 28.dp else 8.dp,
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                label = "dotWidth"
            )
            val color by animateColorAsState(
                targetValue = if (isSelected) accent else Color(0xFFCDD5E0),
                animationSpec = tween(300),
                label = "dotColor"
            )
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
