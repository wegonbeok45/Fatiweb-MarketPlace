package isim.ia2y.myapplication

import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.progressindicator.LinearProgressIndicator

class LoadingScreen : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var isReady = false

    private data class Milestone(val label: Int, val progress: Int)

    private val milestones = listOf(
        Milestone(R.string.loading_step_preferences, 15),
        Milestone(R.string.loading_step_language, 30),
        Milestone(R.string.loading_step_catalog, 50),
        Milestone(R.string.loading_step_cart, 65),
        Milestone(R.string.loading_step_onboarding, 80),
        Milestone(R.string.loading_step_ready, 100)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Keep the splash visible until our content layout is ready
        splashScreen.setKeepOnScreenCondition { !isReady }

        enableEdgeToEdge()
        setContentView(R.layout.activity_loading_screen)

        val rootView = findViewById<View>(R.id.layoutLoadingRoot)
        val initialPaddingLeft = rootView.paddingLeft
        val initialPaddingTop = rootView.paddingTop
        val initialPaddingRight = rootView.paddingRight
        val initialPaddingBottom = rootView.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                initialPaddingLeft + systemBars.left,
                initialPaddingTop + systemBars.top,
                initialPaddingRight + systemBars.right,
                initialPaddingBottom + systemBars.bottom
            )
            insets
        }

        // Mark content ready so splash dismisses
        rootView.post { isReady = true }

        // Reveal views with stagger
        revealViewsInOrder(
            R.id.ivAppLogo,
            R.id.tvAppName,
            R.id.tvTagline,
            R.id.layoutProgressContainer,
            R.id.layoutSecurityBadge
        )

        // Run milestone-based loading
        runMilestoneLoading()
    }

    private fun runMilestoneLoading() {
        val progressBar = findViewById<LinearProgressIndicator>(R.id.progressBar) ?: return
        val progressText = findViewById<TextView>(R.id.tvProgressPercent) ?: return
        val stepLabel = findViewById<TextView>(R.id.tvProgressLabel) ?: return

        if (isReducedMotionEnabled()) {
            progressBar.setProgressCompat(100, false)
            progressText.text = getString(R.string.loading_progress_percent, 100)
            handler.postDelayed({ navigateAway() }, 400)
            return
        }

        var currentIndex = 0
        fun runNext() {
            if (currentIndex >= milestones.size) {
                // All milestones complete — navigate after brief pause
                handler.postDelayed({ navigateAway() }, 300)
                return
            }
            val milestone = milestones[currentIndex]
            stepLabel.text = getString(milestone.label)

            val animator = ObjectAnimator.ofInt(
                progressBar, "progress",
                progressBar.progress, milestone.progress
            ).apply {
                duration = 120L
                interpolator = AccelerateDecelerateInterpolator()
            }
            animator.addUpdateListener {
                val value = it.animatedValue as Int
                progressText.text = getString(R.string.loading_progress_percent, value)
            }
            animator.doOnEnd {
                currentIndex++
                val delay = when (currentIndex) {
                    1 -> 80L
                    2 -> 100L
                    3 -> 120L
                    4 -> 80L
                    5 -> 100L
                    else -> 60L
                }
                handler.postDelayed({ runNext() }, delay)
            }
            animator.start()
        }
        runNext()
    }

    private fun navigateAway() {
        // Fade out then navigate
        val root = findViewById<View>(R.id.layoutLoadingRoot)
        if (!isReducedMotionEnabled()) {
            root.animate()
                .alpha(0f)
                .setDuration(250L)
                .withEndAction { performNavigation() }
                .start()
        } else {
            performNavigation()
        }
    }

    private fun performNavigation() {
        if (isOnboardingCompleted()) {
            navigateToMainTab(MainActivity.Tab.HOME)
        } else {
            navigateNoShift(Onboard1::class.java)
        }
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
