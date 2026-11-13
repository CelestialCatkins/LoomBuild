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

    /** 已有仓库模式的仓库全名（owner/name）。 */
    var existingRepo: String
        get() = sp.getString("existing", "").orEmpty()
        set(value) = sp.edit().putString("existing", value).apply()

    /** 0 = 本地项目，1 = 已有仓库。 */
    var mode: Int
        get() = sp.getInt("mode", 0)
        set(value) = sp.edit().putInt("mode", value).apply()

    /** 构建历史（JSON 数组字符串）。 */
    var history: String
        get() = sp.getString("history", "[]").orEmpty()
        set(value) = sp.edit().putString("history", value).apply()

    fun clearAuth() {
        sp.edit().remove("token").remove("login").apply()
    }
}
