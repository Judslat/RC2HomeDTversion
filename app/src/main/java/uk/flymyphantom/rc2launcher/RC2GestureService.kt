
package uk.flymyphantom.rc2launcher

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.SystemClock
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import kotlin.math.abs

class RC2GestureService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var gestureView: View? = null

    private var startX = 0f
    private var startY = 0f
    private var startTime = 0L

    // Gesture settings
    private val triggerWidthDp = 25f
    private val triggerHeightDp = 45f
    private val swipeDistanceDp = 5f
    private val verticalToleranceDp = 60f
    private val maxSwipeTimeMs = 1000L

    // Detect DJI Fly homepage using Accessibility text

    private fun isDjiFlyHomeScreen(): Boolean {

        val root = rootInActiveWindow ?: return false

        // Never activate the gesture inside RC2Home
        if (root.packageName?.toString() == packageName) {
            return false
        }

        // Only recognise the DJI Fly application
        val activePackage = root.packageName?.toString()

        if (activePackage != "dji.go.v5" &&
            activePackage != "dji.go.v6") {
            return false
        }

        val homeScreenLabels = listOf(
            "GO FLY",
            "CONNECT TO AIRCRAFT"
        )

        return homeScreenLabels.any { label ->
            root.findAccessibilityNodeInfosByText(label).any { node ->
                node.text?.toString()?.contains(
                    label,
                    ignoreCase = true
                ) == true ||
                        node.contentDescription?.toString()?.contains(
                            label,
                            ignoreCase = true
                        ) == true
            }
        }
    }


    override fun onServiceConnected() {
        super.onServiceConnected()
        createGestureOverlay()
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        ).toInt()
    }

    private fun createGestureOverlay() {

        if (gestureView != null) return

        windowManager =
            getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val view = View(this)

        // Invisible gesture area
        view.setBackgroundColor(Color.TRANSPARENT)

        val params = WindowManager.LayoutParams(
            dpToPx(triggerWidthDp),
            dpToPx(triggerHeightDp),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.RIGHT

        view.setOnTouchListener { _, event ->

            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {

                    startX = event.rawX
                    startY = event.rawY
                    startTime = SystemClock.uptimeMillis()

                    // TEMPORARY DIAGNOSTIC
                    val isHome = isDjiFlyHomeScreen()



                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    // Detect swipe on finger release
                    true
                }

                MotionEvent.ACTION_UP -> {

                    val distanceX = startX - event.rawX
                    val distanceY = abs(event.rawY - startY)
                    val elapsed =
                        SystemClock.uptimeMillis() - startTime

                    Log.d(
                        "RC2Gesture",
                        "Swipe X=$distanceX Y=$distanceY Time=$elapsed"
                    )

                    if (
                        isDjiFlyHomeScreen() &&
                        distanceX >= dpToPx(swipeDistanceDp) &&
                        distanceY <= dpToPx(verticalToleranceDp) &&
                        elapsed <= maxSwipeTimeMs
                    ) {

                        // Keep existing working gesture for this test
                        val homeIntent = Intent(
                            this@RC2GestureService,
                            MainActivity::class.java
                        )

                        homeIntent.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                        )

                        startActivity(homeIntent)
                    }

                    true
                }

                MotionEvent.ACTION_CANCEL -> true

                else -> true
            }
        }

        try {
            windowManager?.addView(view, params)
            gestureView = view

        } catch (e: Exception) {
            Log.e(
                "RC2Gesture",
                "Failed to create gesture overlay",
                e
            )
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not required for this diagnostic test
    }

    override fun onInterrupt() {
        // Nothing to interrupt
    }

    override fun onDestroy() {

        gestureView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (_: Exception) {
            }
        }

        gestureView = null
        windowManager = null

        super.onDestroy()
    }
}
