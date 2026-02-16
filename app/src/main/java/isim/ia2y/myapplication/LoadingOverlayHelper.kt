package isim.ia2y.myapplication

import android.animation.ObjectAnimator
import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator

/**
 * Helper to manage a global loading overlay on any activity.
 */
class LoadingOverlayHelper(private val activity: Activity) {

    private val handler = Handler(Looper.getMainLooper())
    private var overlay: View? = activity.findViewById(R.id.layoutGlobalLoading)
    private var isVisible = false
    private var startTime = 0L

    private val showRunnable = Runnable {
        if (!isVisible) {
            val v = overlay ?: return@Runnable
            isVisible = true
            v.visibility = View.VISIBLE
            v.alpha = 0f
            v.animate()
                .alpha(1f)
                .setDuration(150)
                .setInterpolator(DecelerateInterpolator())
                .start()
            startTime = System.currentTimeMillis()
        }
    }

    fun show() {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed(showRunnable, 250) // Anti-flicker delay
    }

    fun hide() {
        handler.removeCallbacks(showRunnable)
        if (!isVisible) return

        val diff = System.currentTimeMillis() - startTime
        val remaining = 500 - diff // Minimum show time

        if (remaining > 0) {
            handler.postDelayed({ hideImmediate() }, remaining)
        } else {
            hideImmediate()
        }
    }

    private fun hideImmediate() {
        if (!isVisible) return
        isVisible = false
        val animator = overlay?.animate() ?: return
        animator.alpha(0f)
            .setDuration(150)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction { overlay?.visibility = View.GONE }
            .start()
    }
}
