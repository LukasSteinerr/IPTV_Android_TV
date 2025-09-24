package com.example.tv_app.model

data class Cast(
    val name: String,
    val profilePath: String?,
    val character: String?
) {
    companion object {
        fun fromJson(json: org.json.JSONObject): Cast {
            return Cast(
                name = json.getString("name"),
                profilePath = json.optString("profile_path"),
                character = json.optString("character")
            )
        }
    }
}