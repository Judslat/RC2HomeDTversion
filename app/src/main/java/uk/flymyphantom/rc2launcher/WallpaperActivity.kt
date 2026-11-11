package uk.flymyphantom.rc2launcher

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast

class WallpaperActivity : Activity() {

    private val wallpaperRequestCode = 2001

    private val preferences by lazy {
        getSharedPreferences("RC2Home", MODE_PRIVATE)
    }

    private lateinit var targetGroup: RadioGroup

    private data class Wallpaper(
        val name: String,
        val resourceId: Int
    )

    private val wallpapers = listOf(
        Wallpaper("Blue Sky", R.drawable.blue_sky),
        Wallpaper("Sunset", R.drawable.sunset),
        Wallpaper("Dark Waves", R.drawable.dark_waves),
        Wallpaper("Mountains", R.drawable.mountains),
        Wallpaper("Carbon Fibre", R.drawable.carbon_fibre),
        Wallpaper("Storm Clouds", R.drawable.storm_clouds)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallpaper)

        targetGroup = findViewById(R.id.wallpaperTargetGroup)
        targetGroup.check(R.id.homeWallpaperOption)

        findViewById<TextView>(R.id.wallpaperBackButton)
            .setOnClickListener {
                finish()
            }

        // Populate wallpaper thumbnails
        val wallpaperGrid = findViewById<GridView>(R.id.wallpaperGrid)

        wallpaperGrid.adapter = object : BaseAdapter() {

            override fun getCount(): Int = wallpapers.size

            override fun getItem(position: Int): Any =
                wallpapers[position]

            override fun getItemId(position: Int): Long =
                position.toLong()

            override fun getView(
                position: Int,
                convertView: View?,
                parent: ViewGroup?
            ): View {

                val wallpaper = wallpapers[position]

                val density = resources.displayMetrics.density
                fun dp(value: Int) = (value * density).toInt()

                val tile = LinearLayout(this@WallpaperActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(dp(8), dp(8), dp(8), dp(8))
                }

                val image = ImageView(this@WallpaperActivity).apply {
                    setImageResource(wallpaper.resourceId)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(120)
                    )
                }

                val label = TextView(this@WallpaperActivity).apply {
                    text = wallpaper.name
                    setTextColor(android.graphics.Color.WHITE)
                    textSize = 16f
                    gravity = Gravity.CENTER
                    setPadding(0, dp(8), 0, 0)
                }

                tile.addView(image)
                tile.addView(label)

                return tile
            }
        }

        wallpaperGrid.setOnItemClickListener { _, _, position, _ ->
            val wallpaper = wallpapers[position]
            val isApps =
                targetGroup.checkedRadioButtonId == R.id.appsWallpaperOption

            val screenName = if (isApps) "All Apps" else "Home Screen"

            AlertDialog.Builder(this)
                .setTitle("Apply ${wallpaper.name}?")
                .setMessage("Set this as your $screenName wallpaper?")
                .setPositiveButton("Apply") { _, _ ->

                    val preferenceKey = if (isApps) {
                        "appsWallpaperResource"
                    } else {
                        "homeWallpaperResource"
                    }

                    preferences.edit()
                        .putInt(preferenceKey, wallpaper.resourceId)
                        .apply()

                    Toast.makeText(
                        this,
                        "$screenName wallpaper saved!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Browse personal pictures
        findViewById<Button>(R.id.browseWallpaperButton)
            .setOnClickListener {

                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "image/*"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                }

                try {
                    startActivityForResult(intent, wallpaperRequestCode)
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "No compatible picture picker installed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    @Deprecated("Uses Android framework activity result API")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != wallpaperRequestCode ||
            resultCode != RESULT_OK
        ) return

        val uri: Uri = data?.data ?: return

        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            val isApps =
                targetGroup.checkedRadioButtonId == R.id.appsWallpaperOption

            val preferenceKey = if (isApps) {
                "appsWallpaperUri"
            } else {
                "wallpaperUri"
            }

            val resourceKey = if (isApps) {
                "appsWallpaperResource"
            } else {
                "homeWallpaperResource"
            }

            preferences.edit()
                .putString(preferenceKey, uri.toString())
                .remove(resourceKey)
                .apply()

            Toast.makeText(
                this,
                "Wallpaper saved!",
                Toast.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Unable to save wallpaper",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
