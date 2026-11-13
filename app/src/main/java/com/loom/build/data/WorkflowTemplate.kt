package com.loom.build.data

import android.content.Context

/** 注入到目标项目的构建流水线。内容来自 assets/build-apk.yml，避免字符串转义问题。 */
object WorkflowTemplate {

    fun load(context: Context): String =
        context.assets.open("build-apk.yml").bufferedReader().use { it.readText() }
}
