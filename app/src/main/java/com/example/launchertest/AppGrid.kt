package com.example.launchertest

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.Collator
import java.util.Locale
import kotlin.math.roundToInt

fun Context.dp(value: Float): Int = (value * resources.displayMetrics.density).roundToInt()

data class AppItem(
    val label: String,
    val packageName: String,
    val icon: Drawable
)

object AppRepository {

    fun loadLaunchableApps(context: Context): List<AppItem> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val collator = Collator.getInstance(Locale.getDefault())
        return pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != context.packageName }
            .distinctBy { it.activityInfo.packageName }
            .map { info ->
                AppItem(
                    label = info.loadLabel(pm).toString(),
                    packageName = info.activityInfo.packageName,
                    icon = info.loadIcon(pm)
                )
            }
            .sortedWith(Comparator { a, b -> collator.compare(a.label, b.label) })
    }

    /** Док: телефон, сообщения, камера, браузер. Если нужных нет, берём первые приложения. */
    fun pickDockApps(all: List<AppItem>, count: Int = 4): List<AppItem> {
        val groups = listOf(
            listOf("телефон", "phone", "dialer"),
            listOf("сообщ", "messag", "sms"),
            listOf("камер", "camera"),
            listOf("браузер", "интернет", "chrome", "browser", "internet")
        )
        val picked = mutableListOf<AppItem>()
        for (keywords in groups) {
            val match = all.firstOrNull { app ->
                app !in picked && keywords.any { key ->
                    app.label.contains(key, ignoreCase = true) ||
                        app.packageName.contains(key, ignoreCase = true)
                }
            }
            if (match != null) picked.add(match)
        }
        for (app in all) {
            if (picked.size >= count) break
            if (app !in picked) picked.add(app)
        }
        return picked.take(count)
    }
}

/**
 * Иконка в форме скруглённого квадрата.
 * Адаптивные иконки имеют поля, поэтому рисуем их чуть больше и обрезаем по контуру.
 */
class IconView(context: Context) : View(context) {

    var iconDrawable: Drawable? = null
        set(value) {
            field = value
            invalidate()
        }

    var cornerRadiusPx: Float = 0f

    private val clipRect = RectF()
    private val clipPath = Path()

    override fun onDraw(canvas: Canvas) {
        val drawable = iconDrawable ?: return
        clipRect.set(0f, 0f, width.toFloat(), height.toFloat())
        clipPath.reset()
        clipPath.addRoundRect(clipRect, cornerRadiusPx, cornerRadiusPx, Path.Direction.CW)
        val saved = canvas.save()
        canvas.clipPath(clipPath)
        val overflow = (width * 0.18f).roundToInt()
        drawable.setBounds(-overflow, -overflow, width + overflow, height + overflow)
        drawable.draw(canvas)
        canvas.restoreToCount(saved)
    }
}

class AppGridAdapter(
    private val itemWidthPx: Int,
    private val iconSizePx: Int,
    private val onClick: (AppItem) -> Unit,
    private val onLongClick: (AppItem) -> Unit
) : RecyclerView.Adapter<AppGridAdapter.Holder>() {

    class Holder(
        val container: View,
        val icon: IconView,
        val label: TextView
    ) : RecyclerView.ViewHolder(container)

    private var allApps: List<AppItem> = emptyList()
    private var shownApps: List<AppItem> = emptyList()

    fun submit(apps: List<AppItem>) {
        allApps = apps
        shownApps = apps
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        val q = query.trim()
        shownApps = if (q.isEmpty()) allApps else allApps.filter { it.label.contains(q, ignoreCase = true) }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val context = parent.context
        val icon = IconView(context).apply {
            layoutParams = LinearLayout.LayoutParams(iconSizePx, iconSizePx)
            cornerRadiusPx = context.dp(14f).toFloat()
        }
        val label = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                itemWidthPx - context.dp(4f),
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = context.dp(6f) }
            gravity = Gravity.CENTER_HORIZONTAL
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setTextColor(Color.WHITE)
            textSize = 11.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setShadowLayer(
                context.dp(3f).toFloat(),
                0f,
                context.dp(1f).toFloat(),
                Color.argb(150, 0, 0, 0)
            )
        }
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = RecyclerView.LayoutParams(itemWidthPx, ViewGroup.LayoutParams.WRAP_CONTENT)
            setPadding(0, context.dp(8f), 0, context.dp(12f))
            addView(icon)
            addView(label)
        }
        return Holder(container, icon, label)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val app = shownApps[position]
        holder.icon.iconDrawable = app.icon
        holder.label.text = app.label
        holder.container.setOnClickListener { onClick(app) }
        holder.container.setOnLongClickListener {
            onLongClick(app)
            true
        }
        holder.container.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN ->
                    view.animate().scaleX(0.92f).scaleY(0.92f).setDuration(110).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    view.animate().scaleX(1f).scaleY(1f).setDuration(160).start()
            }
            false
        }
    }

    override fun getItemCount(): Int = shownApps.size
}
