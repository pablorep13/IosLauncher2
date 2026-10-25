package com.example.launchertest

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.random.Random

/**
 * Генерирует обои прямо в коде.
 * Стекло Liquid Glass преломляет только детали: на ровном градиенте эффекта не видно,
 * поэтому добавлены цветные пятна, тонкие линии и точки.
 */
object Wallpaper {

    fun create(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.shader = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            intArrayOf(
                Color.parseColor("#0A0A24"),
                Color.parseColor("#2A1250"),
                Color.parseColor("#0B3D4F")
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        val blobs = listOf(
            Triple(0.18f, 0.16f, "#FF4F8B"),
            Triple(0.88f, 0.30f, "#3FC7FF"),
            Triple(0.30f, 0.60f, "#8A5CFF"),
            Triple(0.85f, 0.88f, "#FFB347"),
            Triple(0.10f, 0.92f, "#2EE6A6")
        )
        for ((fx, fy, hex) in blobs) {
            val cx = width * fx
            val cy = height * fy
            val radius = width * 0.8f
            val base = Color.parseColor(hex)
            val inner = Color.argb(200, Color.red(base), Color.green(base), Color.blue(base))
            paint.shader = RadialGradient(
                cx, cy, radius,
                intArrayOf(inner, Color.TRANSPARENT),
                null,
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, radius, paint)
        }
        paint.shader = null

        val random = Random(42)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            color = Color.argb(60, 255, 255, 255)
        }
        repeat(28) {
            val x = random.nextFloat() * width
            val y = random.nextFloat() * height
            val length = width * (0.25f + random.nextFloat() * 0.5f)
            canvas.drawLine(x, y, x + length, y - length * 0.6f, linePaint)
        }

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(120, 255, 255, 255)
        }
        repeat(180) {
            canvas.drawCircle(
                random.nextFloat() * width,
                random.nextFloat() * height,
                1f + random.nextFloat() * 2.5f,
                dotPaint
            )
        }
        return bitmap
    }
}
