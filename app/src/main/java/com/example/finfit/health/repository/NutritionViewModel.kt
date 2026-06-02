package com.example.finfit.health.repository

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.finfit.health.model.FoodMealEntity
import com.example.finfit.health.model.HealthEntity
import com.example.finfit.health.model.HealthUiState
import com.example.finfit.health.model.MealItemEntity
import com.example.finfit.health.model.toUiState
import com.example.finfit.health.model.vision.VisionAiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

sealed class NutritionPeriod {
    data object Day : NutritionPeriod()
    data object Week : NutritionPeriod()
}

data class NutritionUiState(
    val todaySummary: HealthUiState = HealthUiState(),
    val meals: List<FoodMealEntity> = emptyList(),
    val period: NutritionPeriod = NutritionPeriod.Day,
    val chartData: List<ChartPoint> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedMeal: FoodMealEntity? = null,
    val selectedMealItems: List<MealItemEntity> = emptyList()
)

data class ChartPoint(
    val label: String,
    val value: Float,
    val isHighlighted: Boolean = false,
    val metadata: String? = null, // Meal name or details
    val time: String? = null      // Specific time string
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
class NutritionViewModel(application: Application) : AndroidViewModel(application) {
    private val healthDao = HealthDatabase.getDatabase(application).healthDao()
    private val mealRepository = MealRepository()
    
    private val _period = MutableStateFlow<NutritionPeriod>(NutritionPeriod.Day)
    private val today = getCurrentDate()
    
    // 1. Data Sources (Reactive)
    private val mealsFlow = mealRepository.observeMealsByDate(today)
    private val summaryCloudFlow = mealRepository.observeDailySummary(today)
    private val summaryLocalFlow = healthDao.observeHealthByDate(today)
    
    // Grouping summary flows to keep combine arguments <= 5
    private val combinedSummaryFlow = combine(summaryCloudFlow, summaryLocalFlow) { cloud, local -> 
        cloud to local
    }

    private val historyFlow = _period.flatMapLatest { p ->
        if (p == NutritionPeriod.Day) flowOf(emptyList())
        else {
            val startDate = calculateStartDate(p)
            healthDao.observeHealthHistoryRange(startDate, today)
        }
    }

    // 2. Selection State
    private val _selection = MutableStateFlow<Pair<FoodMealEntity?, List<MealItemEntity>>>(null to emptyList())

    // 3. Unified UI State (Combined & Optimized)
    val uiState: StateFlow<NutritionUiState> = combine(
        mealsFlow, 
        combinedSummaryFlow, 
        historyFlow, 
        _period,
        _selection
    ) { meals, summaryPair, history, p, selection ->
        val cloud = summaryPair.first
        val local = summaryPair.second
        
        // --- A. Sync Summary ---
        val baseUiState = (local ?: HealthEntity(date = today)).toUiState()
        val finalSummary = if (cloud != null) {
            baseUiState.copy(
                caloriesIn = (cloud["caloriesIn"] as? Number)?.toInt() ?: baseUiState.caloriesIn,
                carbs = (cloud["carbs"] as? Number)?.toInt() ?: baseUiState.carbs,
                protein = (cloud["protein"] as? Number)?.toInt() ?: baseUiState.protein,
                fat = (cloud["fat"] as? Number)?.toInt() ?: baseUiState.fat
            )
        } else baseUiState

        // --- B. Generate Chart Data Atomically ---
        val chartPoints = when(p) {
            NutritionPeriod.Day -> generateHourlyPoints(meals)
            else -> generateWeeklyPoints(history, finalSummary)
        }

        NutritionUiState(
            todaySummary = finalSummary,
            meals = meals,
            period = p,
            chartData = chartPoints,
            selectedMeal = selection.first,
            selectedMealItems = selection.second,
            isLoading = false
        )
    }.debounce(150) // Synchronize multiple Firestore emissions for summary and meals
     .flowOn(Dispatchers.Default)
     .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NutritionUiState(isLoading = true)
    )

    private var selectionJob: kotlinx.coroutines.Job? = null

    fun selectMeal(mealId: String) {
        selectionJob?.cancel()
        selectionJob = viewModelScope.launch {
            mealRepository.observeMealDetails(mealId).collectLatest { (meal, items) ->
                _selection.value = meal to items
            }
        }
    }

    fun dismissMeal() {
        selectionJob?.cancel()
        _selection.value = null to emptyList()
    }

    private fun generateHourlyPoints(meals: List<FoodMealEntity>): List<ChartPoint> {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        // Accumulate calories per hour slot (0..23)
        val hourlyCalories = IntArray(24) { 0 }
        val mealMetadata = mutableMapOf<Int, String>()

        meals.forEach { meal ->
            val cal = Calendar.getInstance().apply { timeInMillis = meal.createdAt }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            if (hour in 0..23) {
                hourlyCalories[hour] += meal.totalCalories
                mealMetadata[hour] = meal.mealName
            }
        }

        // Build cumulative running total for the line chart
        var cumulative = 0
        val cumulativeData = IntArray(24)
        for (h in 0..23) {
            cumulative += hourlyCalories[h]
            cumulativeData[h] = cumulative
        }

        return (0..23).map { h ->
            // Show label every 4 hours: 00h, 04h, 08h, 12h, 16h, 20h
            val label = if (h % 4 == 0) String.format("%02dh", h) else ""
            val totalAtHour = cumulativeData[h]
            val mealName = mealMetadata[h]

            ChartPoint(
                label = label,
                value = totalAtHour.toFloat(),
                isHighlighted = h == currentHour,
                // Tooltip shows calories at that exact hour, or cumulative total
                metadata = if (hourlyCalories[h] > 0)
                    "${mealName ?: "Bữa ăn"} · +${hourlyCalories[h]} kcal"
                else if (totalAtHour > 0)
                    "Tổng tích lũy: $totalAtHour kcal"
                else null,
                time = String.format("%02d:00", h)
            )
        }
    }

    private fun generateHistoryPoints(history: List<HealthEntity>, p: NutritionPeriod): List<ChartPoint> {
        return generateWeeklyPoints(history)
    }

    private fun generateWeeklyPoints(history: List<HealthEntity>, todaySummary: HealthUiState? = null): List<ChartPoint> {
        val days = 7
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -(days - 1))
        
        val historyMap = history.associateBy { it.date }
        val points = mutableListOf<ChartPoint>()
        
        repeat(days) {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
            
            // Priority: Live Summary (for today) > History Map (from DB) > Default 0
            val pointValue = if (dateStr == today && todaySummary != null) {
                todaySummary.caloriesIn.toFloat()
            } else {
                historyMap[dateStr]?.caloriesIn?.toFloat() ?: 0f
            }
            
            points.add(ChartPoint(
                label = formatLabel(dateStr, NutritionPeriod.Week),
                value = pointValue,
                isHighlighted = dateStr == today
            ))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return points
    }

    private fun calculateStartDate(p: NutritionPeriod): String {
        val cal = Calendar.getInstance()
        when (p) {
            NutritionPeriod.Week -> cal.add(Calendar.DAY_OF_YEAR, -6)
            else -> {}
        }
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }

    private fun formatLabel(dateStr: String, period: NutritionPeriod): String {
        return try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr) ?: return dateStr
            when (period) {
                NutritionPeriod.Week -> SimpleDateFormat("EEE", Locale.getDefault()).format(date)
                else -> dateStr
            }
        } catch (e: Exception) {
            dateStr
        }
    }

    private fun getCurrentDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun setPeriod(p: NutritionPeriod) {
        _period.value = p
    }
}
