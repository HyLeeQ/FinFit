package com.example.finfit.data.local

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class UserProfileData(
    val displayName: String = "",
    val age: Int = 0,
    val gender: String = "",            // "male" | "female" | "other"
    val heightCm: Int = 0,
    val weightKg: Float = 0f,
    val activityLevel: String = "moderate", // sedentary|light|moderate|active|very_active
    val monthlyIncome: Double = 0.0,
    val occupation: String = "",
    val financialGoal: String = "",     // save|invest|buy_home|pay_debt|emergency
    val dietaryPref: String = "normal", // normal|vegetarian|vegan
    val sleepGoalHours: Int = 8,
    val waterGoalLiters: Float = 2.0f
)

class SetupPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("setup_prefs", Context.MODE_PRIVATE)

    // ── Currency ──────────────────────────────────────────────
    fun getCurrency(): String = prefs.getString("currency", "VND") ?: "VND"
    fun setCurrency(code: String) = prefs.edit().putString("currency", code).apply()

    // ── Expense categories (selected names) ───────────────────
    fun getEnabledExpenseCategories(): Set<String> {
        val json = prefs.getString("expense_cats", null) ?: return emptySet()
        return try { jsonArrayToSet(json) } catch (e: Exception) { emptySet() }
    }
    fun setEnabledExpenseCategories(names: Set<String>) =
        prefs.edit().putString("expense_cats", JSONArray(names.toList()).toString()).apply()

    // ── Income categories (selected names) ────────────────────
    fun getEnabledIncomeCategories(): Set<String> {
        val json = prefs.getString("income_cats", null) ?: return emptySet()
        return try { jsonArrayToSet(json) } catch (e: Exception) { emptySet() }
    }
    fun setEnabledIncomeCategories(names: Set<String>) =
        prefs.edit().putString("income_cats", JSONArray(names.toList()).toString()).apply()

    // ── Custom categories ─────────────────────────────────────
    fun getCustomExpenseCategories(): List<Pair<String, String>> =
        loadCustomCats("custom_expense_cats")
    fun saveCustomExpenseCategories(cats: List<Pair<String, String>>) =
        saveCustomCats("custom_expense_cats", cats)

    fun getCustomIncomeCategories(): List<Pair<String, String>> =
        loadCustomCats("custom_income_cats")
    fun saveCustomIncomeCategories(cats: List<Pair<String, String>>) =
        saveCustomCats("custom_income_cats", cats)

    // ── User Profile ──────────────────────────────────────────
    fun getUserProfile(): UserProfileData {
        val json = prefs.getString("user_profile", null) ?: return UserProfileData()
        return try {
            val o = JSONObject(json)
            UserProfileData(
                displayName   = o.optString("displayName"),
                age           = o.optInt("age"),
                gender        = o.optString("gender"),
                heightCm      = o.optInt("heightCm"),
                weightKg      = o.optDouble("weightKg", 0.0).toFloat(),
                activityLevel = o.optString("activityLevel", "moderate"),
                monthlyIncome = o.optDouble("monthlyIncome"),
                occupation    = o.optString("occupation"),
                financialGoal = o.optString("financialGoal"),
                dietaryPref   = o.optString("dietaryPref", "normal"),
                sleepGoalHours= o.optInt("sleepGoalHours", 8),
                waterGoalLiters = o.optDouble("waterGoalLiters", 2.0).toFloat()
            )
        } catch (e: Exception) { UserProfileData() }
    }

    fun saveUserProfile(p: UserProfileData) {
        val o = JSONObject().apply {
            put("displayName",   p.displayName)
            put("age",           p.age)
            put("gender",        p.gender)
            put("heightCm",      p.heightCm)
            put("weightKg",      p.weightKg.toDouble())
            put("activityLevel", p.activityLevel)
            put("monthlyIncome", p.monthlyIncome)
            put("occupation",    p.occupation)
            put("financialGoal", p.financialGoal)
            put("dietaryPref",   p.dietaryPref)
            put("sleepGoalHours",p.sleepGoalHours)
            put("waterGoalLiters", p.waterGoalLiters.toDouble())
        }
        prefs.edit().putString("user_profile", o.toString()).apply()
    }

    // ── Helpers ───────────────────────────────────────────────
    private fun jsonArrayToSet(json: String): Set<String> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { arr.getString(it) }.toSet()
    }

    private fun loadCustomCats(key: String): List<Pair<String, String>> {
        val json = prefs.getString(key, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                o.getString("name") to o.getString("emoji")
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun saveCustomCats(key: String, cats: List<Pair<String, String>>) {
        val arr = JSONArray()
        cats.forEach { (name, emoji) ->
            arr.put(JSONObject().apply { put("name", name); put("emoji", emoji) })
        }
        prefs.edit().putString(key, arr.toString()).apply()
    }
}
