package com.example.launchertest

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.liquidglass.GlassMaterial
import com.example.liquidglass.LiquidGlassView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity() {

    private lateinit var root: FrameLayout
    private lateinit var grid: RecyclerView
    private lateinit var clockGlass: LiquidGlassView
    private lateinit var clockText: TextView
    private lateinit var dateText: TextView
    private lateinit var searchGlass: LiquidGlassView
    private lateinit var searchInput: EditText
    private lateinit var dockGlass: LiquidGlassView
    private lateinit var dockRow: LinearLayout
    private lateinit var appAdapter: AppGridAdapter

    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    private val clockTicker = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 60_000L - System.currentTimeMillis() % 60_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)

        buildUi()
        setContentView(root)
        loadApps()
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(clockTicker)
        handler.post(clockTicker)
    }

    override fun onPause() {
        handler.removeCallbacks(clockTicker)
        super.onPause()
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // Домашний экран: назад некуда, ничего не делаем.
    }

    private fun buildUi() {
        val metrics = resources.displayMetrics

        root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                applyInsets(bars.top, bars.bottom)
                insets
            }
        }

        // 1. Обои: именно то, что стекло будет преломлять
        root.addView(
            ImageView(this).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageBitmap(Wallpaper.create(metrics.widthPixels / 2, metrics.heightPixels / 2))
            },
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        )

        // 2. Сетка приложений, прокручивается под стеклом
        val sidePadding = dp(12f)
        val columns = 4
        val itemWidth = (metrics.widthPixels - sidePadding * 2) / columns
        appAdapter = AppGridAdapter(
            itemWidthPx = itemWidth,
            iconSizePx = dp(60f),
            onClick = { app -> launchApp(app) },
            onLongClick = { app -> showAppInfo(app) }
        )
        grid = RecyclerView(this).apply {
            layoutManager = GridLayoutManager(this@MainActivity, columns)
            adapter = appAdapter
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
            setPadding(sidePadding, 0, sidePadding, 0)
        }
        root.addView(
            grid,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        )

        // 3. Стеклянная карточка с часами сверху
        clockText = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 66f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            includeFontPadding = false
        }
        dateText = TextView(this).apply {
            setTextColor(Color.argb(235, 255, 255, 255))
            textSize = 16f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        val clockContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(26f), 0, dp(26f), 0)
            addView(clockText)
            addView(dateText)
        }
        clockGlass = makeGlass(cornerRadiusDp = 36f)
        clockGlass.addView(
            clockContent,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        )
        root.addView(
            clockGlass,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(156f), Gravity.TOP).apply {
                leftMargin = dp(16f)
                rightMargin = dp(16f)
            }
        )

        // 4. Стеклянная строка поиска над доком
        searchInput = EditText(this).apply {
            background = null
            hint = "Поиск"
            setHintTextColor(Color.argb(170, 255, 255, 255))
            setTextColor(Color.WHITE)
            textSize = 17f
            isSingleLine = true
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(22f), 0, dp(22f), 0)
            doAfterTextChanged { text ->
                if (::appAdapter.isInitialized) appAdapter.filter(text?.toString().orEmpty())
            }
        }
        searchGlass = makeGlass(cornerRadiusDp = 26f)
        searchGlass.addView(
            searchInput,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        )
        root.addView(
            searchGlass,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(52f), Gravity.BOTTOM).apply {
                leftMargin = dp(28f)
                rightMargin = dp(28f)
            }
        )

        // 5. Стеклянный док снизу
        dockRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12f), 0, dp(12f), 0)
        }
        dockGlass = makeGlass(cornerRadiusDp = 34f)
        dockGlass.addView(
            dockRow,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        )
        root.addView(
            dockGlass,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(96f), Gravity.BOTTOM).apply {
                leftMargin = dp(16f)
                rightMargin = dp(16f)
            }
        )
    }

    /** Общие настройки всех стеклянных элементов. */
    private fun makeGlass(cornerRadiusDp: Float): LiquidGlassView {
        return LiquidGlassView(this).apply {
            material = GlassMaterial.REGULAR
            cornerRadius = dp(cornerRadiusDp).toFloat()
            refractionHeight = dp(18f).toFloat()
            bevelWidth = dp(28f).toFloat()
            dispersionStrength = 0.12f
            enablePressEffect = false
            // Сетка прокручивается под стеклом, поэтому фон нужно обновлять каждый кадр
            enableDynamicBackground = true
        }
    }

    /** Отступы под статус-бар и навигационную панель. */
    private fun applyInsets(top: Int, bottom: Int) {
        (clockGlass.layoutParams as FrameLayout.LayoutParams).topMargin = top + dp(10f)
        (dockGlass.layoutParams as FrameLayout.LayoutParams).bottomMargin = bottom + dp(14f)
        (searchGlass.layoutParams as FrameLayout.LayoutParams).bottomMargin =
            bottom + dp(14f) + dp(96f) + dp(12f)
        grid.setPadding(
            dp(12f),
            top + dp(10f) + dp(156f) + dp(16f),
            dp(12f),
            bottom + dp(14f) + dp(96f) + dp(12f) + dp(52f) + dp(24f)
        )
        root.requestLayout()
    }

    private fun updateClock() {
        val now = Date()
        val locale = Locale.getDefault()
        clockText.text = SimpleDateFormat("HH:mm", locale).format(now)
        dateText.text = SimpleDateFormat("EEEE, d MMMM", locale).format(now)
    }

    private fun loadApps() {
        executor.execute {
            val apps = AppRepository.loadLaunchableApps(this)
            val dock = AppRepository.pickDockApps(apps)
            runOnUiThread {
                appAdapter.submit(apps)
                renderDock(dock)
            }
        }
    }

    private fun renderDock(apps: List<AppItem>) {
        dockRow.removeAllViews()
        apps.forEach { app ->
            val slot = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                setOnClickListener { launchApp(app) }
            }
            val icon = IconView(this).apply {
                iconDrawable = app.icon
                cornerRadiusPx = dp(15f).toFloat()
            }
            slot.addView(icon, FrameLayout.LayoutParams(dp(62f), dp(62f), Gravity.CENTER))
            dockRow.addView(slot)
        }
    }

    private fun launchApp(app: AppItem) {
        packageManager.getLaunchIntentForPackage(app.packageName)?.let { startActivity(it) }
    }

    private fun showAppInfo(app: AppItem) {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}"))
        )
    }
}
