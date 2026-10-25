package com.netwlax.app

import android.content.Context
import org.json.JSONArray
import java.net.URL

data class Title(
    val id: String, val name: String, val year: String, val desc: String,
    val poster: String, val backdrop: String, val url: String, val category: String
)

/** Catalog = JSON array of titles. Bundled sample by default; point it at your own licensed catalog URL from the settings icon. */
object Catalog {
    fun source(ctx: Context): String = ctx.getSharedPreferences("p", 0).getString("src", "") ?: ""
    fun setSource(ctx: Context, s: String) = ctx.getSharedPreferences("p", 0).edit().putString("src", s).apply()

    private fun bundled(ctx: Context) = ctx.assets.open("catalog.json").bufferedReader().readText()

    fun load(ctx: Context): List<Title> {
        val src = source(ctx)
        val json = try {
            if (src.isBlank()) bundled(ctx) else URL(src).openConnection().apply {
                connectTimeout = 8000; readTimeout = 8000
            }.getInputStream().bufferedReader().readText()
        } catch (e: Exception) { bundled(ctx) }
        val a = JSONArray(json)
        return List(a.length()) {
            val o = a.getJSONObject(it)
            Title(o.optString("id", it.toString()), o.getString("name"), o.optString("year"), o.optString("desc"),
                o.optString("poster"), o.optString("backdrop"), o.getString("url"), o.optString("category", "Popular"))
        }
    }
}
