package com.example.finfit.health.repository

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finfit.health.ui.getCategoryColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

data class HealthNewsArticle(
    val id: String,
    val title: String,
    val content: String,
    val imageUrl: String,
    val category: String,
    val categoryColor: Color,
    val timeAgo: String,
    val isFeatured: Boolean,
    val excerpt: String
)

sealed class NewsUiState {
    object Loading : NewsUiState()
    data class Success(val articles: List<HealthNewsArticle>) : NewsUiState()
    data class Error(val message: String) : NewsUiState()
}

class HealthNewsViewModel : ViewModel() {
    // Using singleton repository
    private val repository = HealthNewsRepository

    private val _uiState = MutableStateFlow<NewsUiState>(NewsUiState.Loading)
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        fetchNews()
    }

    fun fetchNews(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (forceRefresh) {
                _isRefreshing.value = true
            } else {
                _uiState.value = NewsUiState.Loading
            }
            
            try {
                val records = repository.getNewsRecords(forceRefresh).sortedByDescending { it.createdTime }
                val articles = records.map { record ->
                    val fields = record.fields
                    
                    val imageUrl = fields.media?.firstOrNull()?.url ?: ""
                    
                    HealthNewsArticle(
                        id = record.id,
                        title = fields.title ?: "Untitled",
                        content = fields.content?.replace("\\r\\n", "\n")?.replace("\\n", "\n") ?: "",
                        imageUrl = imageUrl,
                        category = fields.category ?: "Khác",
                        categoryColor = getCategoryColor(fields.category ?: ""),
                        timeAgo = formatDate(record.createdTime),
                        isFeatured = fields.isFeatured ?: false,
                        excerpt = fields.excerpt ?: ""
                    )
                }
                _uiState.value = NewsUiState.Success(articles)
            } catch (e: Exception) {
                _uiState.value = NewsUiState.Error(e.message ?: "Unknown error")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun getArticleById(id: String): HealthNewsArticle? {
        val state = _uiState.value
        if (state is NewsUiState.Success) {
            return state.articles.find { it.id == id }
        }
        return null
    }

    private fun formatDate(dateString: String): String {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val date = format.parse(dateString)
            if (date != null) {
                val diff = System.currentTimeMillis() - date.time
                val hours = diff / (1000 * 60 * 60)
                if (hours < 24) {
                    "$hours giờ trước"
                } else {
                    "${hours / 24} ngày trước"
                }
            } else {
                "Gần đây"
            }
        } catch (e: Exception) {
            "Gần đây"
        }
    }
}
