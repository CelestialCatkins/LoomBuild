package com.loom.build.data

import android.content.Context
import android.content.SharedPreferences

/** 本地存储。令牌放在应用私有目录：未 root 的其他应用无法读取。 */
class Prefs(context: Context) {

    private val sp: SharedPreferences =
        context.getSharedPreferences("loom_build", Context.MODE_PRIVATE)

    var token: String
        get() = sp.getString("token", "").orEmpty()
        set(value) = sp.edit().putString("token", value).apply()

    var login: String
        get() = sp.getString("login", "").orEmpty()
        set(value) = sp.edit().putString("login", value).apply()

    var repo: String
        get() = sp.getString("repo", "").orEmpty()
        set(value) = sp.edit().putString("repo", value).apply()

    var lastFolder: String
        get() = sp.getString("folder", "").orEmpty()
        set(value) = sp.edit().putString("folder", value).apply()

    fun clearAuth() {
        sp.edit().remove("token").remove("login").apply()
    }
}
