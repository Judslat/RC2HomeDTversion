package uk.flymyphantom.rc2launcher

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.app.AlertDialog
import android.net.Uri
class AllAppsActivity : Activity() {
    private val hiddenAppsPrefs by lazy {
        getSharedPreferences("RC2HomeHiddenApps", MODE_PRIVATE)
    }

    private fun hideApp(packageName: String) {
        val hidden = hiddenAppsPrefs
            .getStringSet("hiddenPackages", emptySet())
            .orEmpty()
            .toMutableSet()

        hidden.add(packageName)

        hiddenAppsPrefs.edit()
            .putStringSet("hiddenPackages", hidden)
            .apply()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_apps)
        window.statusBarColor = android.graphics.Color.rgb(30, 90, 160)

        // Restore the independently saved All Apps wallpaper

        // Restore the independently saved All Apps wallpaper
        val appsWallpaperImage =
            findViewById<ImageView>(R.id.appsWallpaperImage)

        val prefs = getSharedPreferences("RC2Home", MODE_PRIVATE)

        val builtInWallpaper = prefs.getInt(
            "appsWallpaperResource",
            0
        )

        if (builtInWallpaper != 0) {
            try {
                appsWallpaperImage.setImageResource(builtInWallpaper)
            } catch (e: Exception) {
                appsWallpaperImage.setImageResource(R.drawable.carbon_fibre)
            }
        } else {
            val savedAppsWallpaper = prefs.getString(
                "appsWallpaperUri",
                null
            )

            if (savedAppsWallpaper != null) {
                try {
                    appsWallpaperImage.setImageURI(null)
                    appsWallpaperImage.setImageURI(
                        Uri.parse(savedAppsWallpaper)
                    )
                } catch (e: Exception) {
                    appsWallpaperImage.setImageResource(R.drawable.carbon_fibre)
                }
            } else {
                // No saved preference: use Carbon Fibre
                appsWallpaperImage.setImageResource(R.drawable.carbon_fibre)
            }
        }




        findViewById<TextView>(R.id.backButton).setOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.restoreAppsButton)
            .setOnClickListener {

                val hiddenPackages = hiddenAppsPrefs
                    .getStringSet("hiddenPackages", emptySet())
                    .orEmpty()
                    .toList()
                    .sorted()

                if (hiddenPackages.isEmpty()) {
                    Toast.makeText(
                        this,
                        "No hidden apps",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {

                    val appNames = hiddenPackages.map { packageName ->
                        try {
                            val appInfo = packageManager.getApplicationInfo(
                                packageName, 0
                            )
                            packageManager.getApplicationLabel(appInfo).toString()
                        } catch (e: Exception) {
                            packageName
                        }
                    }

                    AlertDialog.Builder(this)
                        .setTitle("Restore Hidden Apps")
                        .setItems(appNames.toTypedArray()) { _, which ->

                            val selectedPackage = hiddenPackages[which]

                            AlertDialog.Builder(this)
                                .setTitle("Restore ${appNames[which]}?")
                                .setMessage(
                                    "This application will reappear in All Apps."
                                )
                                .setPositiveButton("Restore") { _, _ ->

                                    val updated = hiddenAppsPrefs
                                        .getStringSet(
                                            "hiddenPackages",
                                            emptySet()
                                        )
                                        .orEmpty()
                                        .toMutableSet()

                                    updated.remove(selectedPackage)

                                    hiddenAppsPrefs.edit()
                                        .putStringSet("hiddenPackages", updated)
                                        .apply()

                                    recreate()
                                }
                                .setNegativeButton("Cancel", null)
                                .show()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }


        val appsGrid = findViewById<GridLayout>(R.id.appsGrid)



        // Find installed apps that have launcher activities
        val queryIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val hiddenPackages = hiddenAppsPrefs
            .getStringSet("hiddenPackages", emptySet())
            .orEmpty()

        val apps = packageManager.queryIntentActivities(
            queryIntent, 0
        )
            .filter {
                it.activityInfo.packageName !in hiddenPackages
            }
            .distinctBy {
                it.activityInfo.packageName
            }
            .sortedBy {
                it.loadLabel(packageManager).toString().lowercase()
            }

        val density = resources.displayMetrics.density

        fun dp(value: Int): Int = (value * density).toInt()

        // Create a shortcut for every installed app
        for (app in apps) {

            val tile = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(8), dp(12), dp(8), dp(12))
                isClickable = true
                isFocusable = true
            }

            val icon = ImageView(this).apply {
                setImageDrawable(app.loadIcon(packageManager))
                layoutParams = LinearLayout.LayoutParams(
                    dp(64), dp(64)
                )
                scaleType = ImageView.ScaleType.FIT_CENTER
            }

            val label = TextView(this).apply {
                text = app.loadLabel(packageManager)
                setTextColor(android.graphics.Color.WHITE)
                textSize = 15f
                gravity = Gravity.CENTER
                maxLines = 2
            }

            tile.addView(icon)
            tile.addView(label)

            val gridParams = GridLayout.LayoutParams().apply {
                width = 0
                height = dp(145)
                columnSpec = GridLayout.spec(
                    GridLayout.UNDEFINED, 1f
                )
            }

            appsGrid.addView(tile, gridParams)

            // Long-press an app icon to open its options
            tile.setOnLongClickListener {

                val appName = app.loadLabel(packageManager).toString()
                val packageName = app.activityInfo.packageName

                val options = arrayOf(
                    "App Info",
                    "Hide"
                )

                AlertDialog.Builder(this)
                    .setTitle(appName)
                    .setItems(options) { _, which ->

                        when (which) {

                            0 -> {
                                // Open Android's own App Info screen
                                try {
                                    val intent = Intent(
                                        android.provider.Settings
                                            .ACTION_APPLICATION_DETAILS_SETTINGS
                                    ).apply {
                                        data = Uri.parse("package:$packageName")
                                    }

                                    startActivity(intent)

                                } catch (e: Exception) {
                                    Toast.makeText(
                                        this,
                                        "App Info unavailable",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }




                            1 -> {
                                AlertDialog.Builder(this)
                                    .setTitle("Hide $appName?")
                                    .setMessage(
                                        "This app will disappear from All Apps. " +
                                                "It will not be uninstalled."
                                    )
                                    .setPositiveButton("Hide") { _, _ ->
                                        hideApp(packageName)

                                        // Refresh the screen to remove the icon
                                        recreate()
                                    }
                                    .setNegativeButton("Cancel", null)
                                    .show()
                            }
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()

                true
            }


            tile.setOnClickListener {
                try {
                    val launchIntent =
                        packageManager.getLaunchIntentForPackage(
                            app.activityInfo.packageName
                        )

                    if (launchIntent != null) {
                        launchIntent.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                        )
                        startActivity(launchIntent)
                    } else {
                        Toast.makeText(
                            this,
                            "Unable to launch app",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "Unable to launch app",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
