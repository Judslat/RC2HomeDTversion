
package uk.flymyphantom.rc2launcher

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast


class MainActivity : Activity() {

    private lateinit var wallpaperImage: ImageView

    private val wallpaperRequestCode = 1001
    private var djiFlyApp: ResolveInfo? = null

    // Check whether RC2Home Gesture is enabled
    private fun isGestureServiceEnabled(): Boolean {

        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val expectedService = ComponentName(
            this,
            RC2GestureService::class.java
        )

        return enabledServices.split(':').any { service ->
            ComponentName.unflattenFromString(service) == expectedService
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        window.statusBarColor =
            android.graphics.Color.rgb(30, 90, 160)

        wallpaperImage = findViewById(R.id.wallpaperImage)

        // Restore saved wallpaper
        val savedUri = getSharedPreferences(
            "RC2Home",
            MODE_PRIVATE
        ).getString("wallpaperUri", null)

        if (savedUri != null) {
            try {
                wallpaperImage.setImageURI(Uri.parse(savedUri))
            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    "Saved wallpaper unavailable",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Find installed DJI Fly application
        val searchIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        djiFlyApp = packageManager.queryIntentActivities(
            searchIntent,
            0
        ).firstOrNull {
            it.loadLabel(packageManager).toString()
                .contains("DJI Fly", ignoreCase = true)
        }

        // Display actual DJI Fly icon
        val djiIcon = findViewById<ImageView>(R.id.djiFlyIcon)

        djiFlyApp?.let { app ->
            djiIcon.setImageDrawable(
                app.loadIcon(packageManager)
            )
        }

        // Launch DJI Fly
        findViewById<LinearLayout>(R.id.djiFlyButton)
            .setOnClickListener {

                val app = djiFlyApp

                if (app != null) {
                    try {
                        val intent =
                            packageManager.getLaunchIntentForPackage(
                                app.activityInfo.packageName
                            )

                        if (intent != null) {
                            intent.addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                            )
                            startActivity(intent)
                        } else {
                            Toast.makeText(
                                this,
                                "DJI Fly launch intent unavailable",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                    } catch (e: Exception) {
                        Toast.makeText(
                            this,
                            "Unable to open DJI Fly",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } else {
                    Toast.makeText(
                        this,
                        "DJI Fly not installed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        // Open RC2Home Wallpaper Gallery
        findViewById<LinearLayout>(R.id.settingsButton)
            .setOnClickListener {
                startActivity(
                    Intent(this, WallpaperActivity::class.java)
                )
            }

        // Open All Apps screen
        findViewById<LinearLayout>(R.id.allAppsButton)
            .setOnClickListener {
                startActivity(
                    Intent(this, AllAppsActivity::class.java)
                )
            }

        // Open Accessibility settings only when gesture is disabled
        if (!isGestureServiceEnabled()) {
            try {
                startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                )
            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    "Unable to open Accessibility settings",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    } // END of onCreate()



    // Handle selected wallpaper
    @Deprecated("Uses Android framework activity result API")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == wallpaperRequestCode &&
            resultCode == RESULT_OK
        ) {
            val uri = data?.data ?: return

            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                getSharedPreferences(
                    "RC2Home",
                    MODE_PRIVATE
                )
                    .edit()
                    .putString("wallpaperUri", uri.toString())
                    .apply()

                wallpaperImage.setImageURI(uri)

            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    "Unable to load wallpaper",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val prefs = getSharedPreferences(
            "RC2Home",
            MODE_PRIVATE
        )

        // Check whether the user selected a built-in wallpaper
        val builtInWallpaper = prefs.getInt(
            "homeWallpaperResource",
            0
        )

        if (builtInWallpaper != 0) {

            // Display the selected built-in wallpaper
            try {
                wallpaperImage.setImageResource(builtInWallpaper)
            } catch (e: Exception) {
                wallpaperImage.setImageResource(
                    R.drawable.dark_waves
                )
            }

        } else {

            // Check whether the user selected their own photograph
            val savedUri = prefs.getString(
                "wallpaperUri",
                null
            )

            if (savedUri != null) {
                try {
                    wallpaperImage.setImageURI(null)
                    wallpaperImage.setImageURI(
                        Uri.parse(savedUri)
                    )
                } catch (e: Exception) {
                    wallpaperImage.setImageResource(
                        R.drawable.dark_waves
                    )
                }
            } else {

                // Fresh installation: use Dark Waves by default
                wallpaperImage.setImageResource(
                    R.drawable.dark_waves
                )
            }
        }
    }
}
