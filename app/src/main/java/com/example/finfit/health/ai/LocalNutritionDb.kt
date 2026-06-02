package com.example.finfit.health.ai

import com.example.finfit.health.model.vision.DishInfo
import com.example.finfit.health.model.vision.DishNutritionResult
import com.example.finfit.health.model.vision.IngredientInfo
import com.example.finfit.health.model.vision.Macros

object LocalNutritionDb {

    val db = mapOf(
        "pho_vietnam" to LocalFoodProfile(
            name = "Phở Việt Nam",
            calories = 350f,
            protein = 15f,
            carbs = 55f,
            fat = 10f,
            healthScore = 8.5f,
            ingredients = listOf("Bánh phở", "Thịt bò", "Nước dùng xương", "Hành lá", "Rau thơm"),
            notes = listOf("Giàu carbohydrate phức hợp từ bánh phở", "Cung cấp protein dồi dào từ thịt bò", "Hạn chế hút nhiều nước dùng nếu bạn đang ăn kiêng muối")
        ),
        "bread_vietnam" to LocalFoodProfile(
            name = "Bánh Mì Việt Nam",
            calories = 290f,
            protein = 9f,
            carbs = 48f,
            fat = 8f,
            healthScore = 7.5f,
            ingredients = listOf("Vỏ bánh mì", "Pate", "Chả lụa", "Thịt nguội", "Dưa leo", "Đồ chua", "Ngò rí"),
            notes = listOf("Bữa sáng tiện lợi, giàu năng lượng", "Nên hạn chế pate và bơ nếu muốn cắt giảm chất béo bão hòa")
        ),
        "goi_cuon" to LocalFoodProfile(
            name = "Gỏi Cuốn",
            calories = 180f, // portion of 3 rolls
            protein = 10f,
            carbs = 32f,
            fat = 2f,
            healthScore = 9.5f,
            ingredients = listOf("Bánh tráng", "Tôm", "Thịt ba chỉ", "Bún", "Hẹ", "Rau sống"),
            notes = listOf("Món ăn cực kỳ lành mạnh, rất ít chất béo", "Nên dùng kèm nước tương hoặc mắm nêm vừa phải")
        ),
        "rice" to LocalFoodProfile(
            name = "Cơm Trắng",
            calories = 260f, // 1 standard bowl ~ 200g
            protein = 5.2f,
            carbs = 56.4f,
            fat = 0.4f,
            healthScore = 8.0f,
            ingredients = listOf("Gạo tẻ", "Nước"),
            notes = listOf("Nguồn cung cấp tinh bột chính cho cơ thể", "Có thể thay thế bằng cơm gạo lứt để tăng hàm lượng chất xơ")
        ),
        "burger" to LocalFoodProfile(
            name = "Bánh Hamburger",
            calories = 360f,
            protein = 18f,
            carbs = 35f,
            fat = 17f,
            healthScore = 5.5f,
            ingredients = listOf("Bánh mì tròn", "Thịt bò băm", "Phô mai miếng", "Xà lách", "Cà chua", "Sốt mayonnaise"),
            notes = listOf("Hàm lượng chất béo bão hòa cao", "Thích hợp cho bữa ăn xả (cheat meal)")
        ),
        "pizza" to LocalFoodProfile(
            name = "Bánh Pizza",
            calories = 550f, // portion size ~ 2 slices
            protein = 24f,
            carbs = 68f,
            fat = 20f,
            healthScore = 6.0f,
            ingredients = listOf("Đế bánh pizza", "Sốt cà chua", "Phô mai Mozzarella", "Thịt nguội/Xúc xích"),
            notes = listOf("Giàu năng lượng và natri", "Nên ăn kèm nhiều rau xanh để cân bằng dinh dưỡng")
        ),
        "apple" to LocalFoodProfile(
            name = "Quả Táo",
            calories = 52f,
            protein = 0.3f,
            carbs = 13.8f,
            fat = 0.2f,
            healthScore = 9.8f,
            ingredients = listOf("Táo tươi"),
            notes = listOf("Nguồn vitamin C và chất xơ pectin dồi dào", "Giúp hỗ trợ giảm cân và làm sạch hệ tiêu hóa")
        ),
        "banana" to LocalFoodProfile(
            name = "Quả Chuối",
            calories = 89f,
            protein = 1.1f,
            carbs = 22.8f,
            fat = 0.3f,
            healthScore = 9.5f,
            ingredients = listOf("Chuối chín"),
            notes = listOf("Cung cấp Kali dồi dào, giúp ngăn ngừa chuột rút cơ bắp", "Lý tưởng cho bữa phụ trước khi tập luyện thể thao")
        ),
        "avocado" to LocalFoodProfile(
            name = "Quả Bơ",
            calories = 160f,
            protein = 2f,
            carbs = 8.5f,
            fat = 14.7f,
            healthScore = 9.2f,
            ingredients = listOf("Bơ sáp tươi"),
            notes = listOf("Chứa nhiều chất béo đơn không bão hòa rất tốt cho tim mạch", "Giàu chất xơ và hỗ trợ giảm cholesterol xấu")
        ),
        "orange" to LocalFoodProfile(
            name = "Quả Cam",
            calories = 47f,
            protein = 0.9f,
            carbs = 11.8f,
            fat = 0.1f,
            healthScore = 9.6f,
            ingredients = listOf("Cam tươi"),
            notes = listOf("Cực kỳ giàu Vitamin C giúp tăng cường sức đề kháng", "Nên ăn cả múi thay vì chỉ uống nước ép để bổ sung chất xơ")
        ),
        "egg" to LocalFoodProfile(
            name = "Trứng",
            calories = 78f, // 1 whole boiled egg
            protein = 6.3f,
            carbs = 0.6f,
            fat = 5.3f,
            healthScore = 9.0f,
            ingredients = listOf("Trứng"),
            notes = listOf("Nguồn protein hoàn chỉnh chất lượng cao", "Chứa nhiều choline tốt cho não bộ và sự phát triển tế bào")
        ),
        "fried_egg" to LocalFoodProfile(
            name = "Trứng Chiên",
            calories = 95f,
            protein = 6.3f,
            carbs = 0.6f,
            fat = 7.5f,
            healthScore = 8.2f,
            ingredients = listOf("Trứng", "Dầu ăn"),
            notes = listOf("Tiện lợi, bổ dưỡng", "Hạn chế dùng quá nhiều dầu hoặc bơ khi chiên")
        ),
        "chicken" to LocalFoodProfile(
            name = "Thịt Gà",
            calories = 165f, // 100g ức gà
            protein = 31f,
            carbs = 0f,
            fat = 3.6f,
            healthScore = 9.5f,
            ingredients = listOf("Thịt ức gà"),
            notes = listOf("Lựa chọn hàng đầu cho việc xây dựng cơ bắp", "Hàm lượng chất béo cực kỳ thấp và giàu đạm")
        ),
        "fried_chicken" to LocalFoodProfile(
            name = "Gà Rán",
            calories = 250f, // 100g đùi gà rán
            protein = 19f,
            carbs = 9f,
            fat = 15f,
            healthScore = 5.0f,
            ingredients = listOf("Đùi/Cánh gà", "Bột chiên giòn", "Dầu chiên"),
            notes = listOf("Món ăn nhiều calo rỗng và chất béo bão hòa từ dầu chiên", "Không nên ăn thường xuyên nếu đang trong chế độ giảm cân")
        ),
        "coffee" to LocalFoodProfile(
            name = "Cà Phê Đen",
            calories = 2f,
            protein = 0.1f,
            carbs = 0f,
            fat = 0f,
            healthScore = 9.0f,
            ingredients = listOf("Cà phê hạt nguyên chất", "Nước"),
            notes = listOf("Giúp tăng sự tỉnh táo và thúc đẩy quá trình trao đổi chất", "Không nên uống vào buổi tối muộn để tránh mất ngủ")
        ),
        "milk_coffee" to LocalFoodProfile(
            name = "Cà Phê Sữa",
            calories = 140f,
            protein = 3f,
            carbs = 24f,
            fat = 4f,
            healthScore = 7.0f,
            ingredients = listOf("Cà phê hạt", "Sữa đặc có đường", "Đá viên"),
            notes = listOf("Hương vị đậm đà thơm ngon đặc trưng Việt Nam", "Chứa nhiều đường từ sữa đặc, nên điều chỉnh lượng sữa phù hợp")
        ),
        "tofu" to LocalFoodProfile(
            name = "Đậu Hũ (Đậu Phụ)",
            calories = 76f, // 100g
            protein = 8f,
            carbs = 1.9f,
            fat = 4.8f,
            healthScore = 9.2f,
            ingredients = listOf("Đậu nành nguyên chất"),
            notes = listOf("Nguồn đạm thực vật tuyệt vời cho người ăn chay", "Dễ tiêu hóa, giàu canxi tốt cho hệ xương")
        ),
        "salad" to LocalFoodProfile(
            name = "Xa Lát Rau Trộn (Salad)",
            calories = 120f,
            protein = 1.5f,
            carbs = 8f,
            fat = 9f,
            healthScore = 9.0f,
            ingredients = listOf("Xà lách", "Cà chua", "Dưa leo", "Củ hành", "Dầu ô liu", "Sốt trộn"),
            notes = listOf("Cực kỳ giàu chất xơ, vitamin và chất chống oxy hóa", "Nên kiểm soát lượng sốt trộn kem béo để tránh tăng calo vô ý")
        ),
        "shrimp" to LocalFoodProfile(
            name = "Thịt Tôm",
            calories = 99f, // 100g tôm hấp
            protein = 24f,
            carbs = 0.2f,
            fat = 0.3f,
            healthScore = 9.4f,
            ingredients = listOf("Tôm"),
            notes = listOf("Hàm lượng đạm cao vượt trội, hầu như không chứa chất béo", "Giàu selen và vitamin B12")
        ),
        "pork" to LocalFoodProfile(
            name = "Thịt Heo (Thịt Lợn)",
            calories = 242f, // 100g thịt nạc heo
            protein = 26f,
            carbs = 0f,
            fat = 15f,
            healthScore = 8.0f,
            ingredients = listOf("Thịt heo"),
            notes = listOf("Cung cấp hàm lượng Protein dồi dào và các khoáng chất thiết yếu", "Nên bỏ bớt phần mỡ lợn để giảm calo không cần thiết")
        ),
        "beef" to LocalFoodProfile(
            name = "Thịt Bò",
            calories = 250f, // 100g thịt thăn bò
            protein = 26f,
            carbs = 0f,
            fat = 15f,
            healthScore = 8.5f,
            ingredients = listOf("Thịt thăn bò"),
            notes = listOf("Cung cấp sắt heme dễ hấp thụ tốt cho máu", "Giàu kẽm và protein hỗ trợ hệ miễn dịch")
        ),
        "salmon" to LocalFoodProfile(
            name = "Cá Hồi",
            calories = 208f, // 100g phi-lê cá hồi
            protein = 20f,
            carbs = 0f,
            fat = 13f,
            healthScore = 9.6f,
            ingredients = listOf("Cá hồi phi lê"),
            notes = listOf("Cực kỳ giàu Axit béo Omega-3 giúp bảo vệ tim mạch và trí não", "Nguồn đạm chất lượng rất cao")
        ),
        "potato" to LocalFoodProfile(
            name = "Khoai Tây Hấp/Luộc",
            calories = 87f, // 100g
            protein = 2f,
            carbs = 20.1f,
            fat = 0.1f,
            healthScore = 8.5f,
            ingredients = listOf("Khoai tây nguyên củ"),
            notes = listOf("Giàu kali và chất xơ, tạo cảm giác no lâu", "Nên chế biến luộc hoặc hấp, tránh chiên rán nhiều dầu mỡ")
        ),
        "sweet_potato" to LocalFoodProfile(
            name = "Khoai Lang Luộc",
            calories = 86f, // 100g
            protein = 1.6f,
            carbs = 20.1f,
            fat = 0.1f,
            healthScore = 9.0f,
            ingredients = listOf("Khoai lang vàng/mật"),
            notes = listOf("Chỉ số đường huyết (GI) thấp, lý tưởng cho người giảm cân hoặc tiểu đường", "Cực kỳ giàu beta-carotene tốt cho mắt")
        ),
        "noodle" to LocalFoodProfile(
            name = "Mì Ăn Liền",
            calories = 380f,
            protein = 8f,
            carbs = 53f,
            fat = 15f,
            healthScore = 4.5f,
            ingredients = listOf("Mì ăn liền", "Gói sốt/gia vị"),
            notes = listOf("Chứa nhiều muối natri và chất béo chuyển hóa", "Không nên ăn thường xuyên, nên bổ sung rau xanh và thịt khi ăn")
        ),
        "soda" to LocalFoodProfile(
            name = "Nước Ngọt (Soda)",
            calories = 140f, // 1 lon 330ml
            protein = 0f,
            carbs = 39f,
            fat = 0f,
            healthScore = 3.0f,
            ingredients = listOf("Nước bão hòa CO2", "Đường kính", "Chất tạo ngọt", "Hương liệu"),
            notes = listOf("Chứa hoàn toàn calo rỗng từ đường tinh luyện", "Gây tăng đường huyết nhanh, tăng nguy cơ béo phì và tiểu đường")
        ),
        "yogurt" to LocalFoodProfile(
            name = "Sữa Chua ít đường",
            calories = 80f, // 100g
            protein = 3.5f,
            carbs = 12f,
            fat = 2f,
            healthScore = 9.0f,
            ingredients = listOf("Sữa tươi lên men tự nhiên", "Lợi khuẩn Probiotics"),
            notes = listOf("Bổ sung hàng tỷ lợi khuẩn giúp bảo vệ hệ tiêu hóa khỏe mạnh", "Giàu canxi giúp phát triển chiều cao và bảo vệ xương khớp")
        )
    )

    fun getNutritionForLabel(label: String, confidence: Float): DishNutritionResult? {
        val key = label.lowercase().trim().replace(" ", "_")
        val profile = db[key] ?: return null
        
        return DishNutritionResult(
            dishName = profile.name,
            dishConfidence = confidence,
            possibleDishes = listOf(DishInfo(profile.name, confidence)),
            ingredients = profile.ingredients.map { IngredientInfo(it, 0.95f) },
            estimatedCalories = profile.calories,
            macros = Macros(
                proteinG = profile.protein,
                carbsG = profile.carbs,
                fatG = profile.fat
            ),
            healthScore = profile.healthScore,
            analysisNotes = profile.notes
        )
    }
}

data class LocalFoodProfile(
    val name: String,
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val healthScore: Float,
    val ingredients: List<String>,
    val notes: List<String>
)
