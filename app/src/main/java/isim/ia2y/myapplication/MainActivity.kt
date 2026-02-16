package isim.ia2y.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager

class MainActivity : AppCompatActivity() {

    enum class Tab {
        HOME, EXPLORE, CART, PROFILE
    }

    private val locationPermissionRequestCode = 903
    private var currentTab: Tab = Tab.HOME

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupBottomNav()
        requestLocationPermissionIfNeeded()

        currentTab = savedInstanceState?.getString(KEY_SELECTED_TAB)
            ?.let { runCatching { Tab.valueOf(it) }.getOrNull() }
            ?: intent.getStringExtra(EXTRA_OPEN_TAB)?.let { runCatching { Tab.valueOf(it) }.getOrNull() }
            ?: Tab.HOME
        selectTab(currentTab, animate = false)

        onBackPressedDispatcher.addCallback(this) {
            if (currentTab != Tab.HOME) {
                selectTab(Tab.HOME, animate = false)
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateBottomNavSelection(currentTab)
        updateTabIndicator(currentTab, animate = false)
        updateHostCartBadge()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_SELECTED_TAB, currentTab.name)
        super.onSaveInstanceState(outState)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val requested = intent.getStringExtra(EXTRA_OPEN_TAB)
            ?.let { runCatching { Tab.valueOf(it) }.getOrNull() }
            ?: return
        selectTab(requested, animate = false)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (handleNotificationPermissionResult(requestCode, grantResults)) return
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    fun selectTab(tab: Tab, animate: Boolean = true) {
        runCatching {
            if (tab == currentTab && supportFragmentManager.findFragmentByTag(tab.name) != null) {
                updateBottomNavSelection(tab)
                updateTabIndicator(tab, animate = animate)
                updateHostCartBadge()
                return
            }
            val transaction = supportFragmentManager.beginTransaction().setReorderingAllowed(true)

            val currentFragment = supportFragmentManager.findFragmentByTag(currentTab.name)
            if (currentFragment != null) {
                transaction.hide(currentFragment)
            }

            val target = supportFragmentManager.findFragmentByTag(tab.name) ?: createTabFragment(tab).also {
                transaction.add(R.id.hostFragmentContainer, it, tab.name)
            }
            transaction.show(target)
            transaction.runOnCommit {
                playTabEnterAnimation(enabled = animate)
            }
            transaction.commit()

            currentTab = tab
            updateBottomNavSelection(tab)
            updateTabIndicator(tab, animate = animate)
            updateHostCartBadge()
        }.onFailure { error ->
            Log.e(TAG, "Failed to open tab: $tab", error)
            showToast(getString(R.string.coming_soon))
            if (tab != Tab.HOME) {
                runCatching { selectTab(Tab.HOME, animate = false) }
            }
        }
    }

    private fun createTabFragment(tab: Tab): Fragment = when (tab) {
        Tab.HOME -> HomeTabFragment()
        Tab.EXPLORE -> ExploreTabFragment()
        Tab.CART -> CartTabFragment()
        Tab.PROFILE -> ProfileTabFragment()
    }

    fun updateHostCartBadge() {
        val badgeContainer = findViewById<View>(R.id.hostCardBottomCartBadge)
        val badgeText = findViewById<TextView>(R.id.hostTvBottomCartBadge)
        val count = CartStore.itemCount(this)
        if (count <= 0) {
            badgeContainer.visibility = View.GONE
            return
        }
        badgeContainer.visibility = View.VISIBLE
        badgeText.text = count.toString()
    }

    private fun setupBottomNav() {
        findViewById<View>(R.id.hostNavHome).setOnClickListener { selectTab(Tab.HOME) }
        findViewById<View>(R.id.hostNavExplore).setOnClickListener { selectTab(Tab.EXPLORE) }
        findViewById<View>(R.id.hostNavCart).setOnClickListener { selectTab(Tab.CART) }
        findViewById<View>(R.id.hostNavProfile).setOnClickListener { selectTab(Tab.PROFILE) }
    }

    private fun updateBottomNavSelection(selected: Tab) {
        setNavItemState(
            containerId = R.id.hostNavHome,
            iconId = R.id.hostNavHomeIcon,
            labelId = R.id.hostNavHomeLabel,
            active = selected == Tab.HOME
        )
        setNavItemState(
            containerId = R.id.hostNavExplore,
            iconId = R.id.hostNavExploreIcon,
            labelId = R.id.hostNavExploreLabel,
            active = selected == Tab.EXPLORE
        )
        setNavItemState(
            containerId = R.id.hostNavCart,
            iconId = R.id.hostIvBottomCartIcon,
            labelId = R.id.hostNavCartLabel,
            active = selected == Tab.CART
        )
        setNavItemState(
            containerId = R.id.hostNavProfile,
            iconId = R.id.hostNavProfileIcon,
            labelId = R.id.hostNavProfileLabel,
            active = selected == Tab.PROFILE
        )
    }

    private fun setNavItemState(
        containerId: Int,
        iconId: Int,
        labelId: Int,
        active: Boolean
    ) {
        val color = ContextCompat.getColor(
            this,
            if (active) R.color.profile_nav_active else R.color.profile_nav_inactive
        )
        (findViewById<View>(containerId) as? LinearLayout)?.alpha = if (active) 1f else 0.95f
        findViewById<ImageView>(iconId).setColorFilter(color)
        findViewById<TextView>(labelId).setTextColor(color)
    }

    private fun requestLocationPermissionIfNeeded() {
        val coarse = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val fine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (coarse || fine) return

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            locationPermissionRequestCode
        )
    }

    private fun playTabEnterAnimation(enabled: Boolean) {
        if (!enabled || isReducedMotionEnabled()) return
        val content = findViewById<View>(R.id.hostFragmentContainer)
        val distance = 14f * resources.displayMetrics.density
        content.animate().cancel()
        content.alpha = 0f
        content.scaleX = 0.98f
        content.scaleY = 0.98f
        content.translationY = distance
        content.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationY(0f)
            .setDuration(300L)
            .setInterpolator(FastOutSlowInInterpolator())
            .start()
    }

    private fun updateTabIndicator(tab: Tab, animate: Boolean) {
        val navContainer = findViewById<ConstraintLayout>(R.id.hostLayoutBottomNav) ?: return
        val indicator = findViewById<View>(R.id.nav_indicator) ?: return
        val targetViewId = getTabContainerId(tab)

        // Reset any residual translationX from old approach
        indicator.translationX = 0f

        val constraintSet = ConstraintSet()
        constraintSet.clone(navContainer)
        constraintSet.connect(R.id.nav_indicator, ConstraintSet.START, targetViewId, ConstraintSet.START)
        constraintSet.connect(R.id.nav_indicator, ConstraintSet.END, targetViewId, ConstraintSet.END)

        if (animate && !isReducedMotionEnabled()) {
            val transition = AutoTransition()
            transition.duration = 300L
            transition.interpolator = FastOutSlowInInterpolator()
            TransitionManager.beginDelayedTransition(navContainer, transition)
        }

        constraintSet.applyTo(navContainer)
    }

    private fun getTabContainerId(tab: Tab): Int = when (tab) {
        Tab.HOME -> R.id.hostNavHome
        Tab.EXPLORE -> R.id.hostNavExplore
        Tab.CART -> R.id.hostNavCart
        Tab.PROFILE -> R.id.hostNavProfile
    }

    companion object {
        const val EXTRA_OPEN_TAB = "open_main_tab"
        private const val KEY_SELECTED_TAB = "selected_tab"
        private const val TAG = "MainActivity"
    }
}
