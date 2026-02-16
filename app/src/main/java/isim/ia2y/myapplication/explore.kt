package isim.ia2y.myapplication

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class explore : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_explore)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        bindBackToMainForBottomNavScreens()
        setupBottomNavigation()
        setupHeaderAndExploreActions()
        polishExploreUi()
    }

    override fun onResume() {
        super.onResume()
        applyBottomNavSelection(
            selectedId = R.id.navExplore,
            homeId = R.id.navHome,
            exploreId = R.id.navExplore,
            favoritesId = null,
            cartId = R.id.navCart,
            profileId = R.id.navProfile
        )
        updateBottomCartBadge()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (handleNotificationPermissionResult(requestCode, grantResults)) return
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun setupBottomNavigation() {
        bindBottomNav(
            homeId = R.id.navHome,
            exploreId = R.id.navExplore,
            favoritesId = null,
            cartId = R.id.navCart,
            profileId = R.id.navProfile
        )
    }

    private fun setupHeaderAndExploreActions() {
        findViewById<View>(R.id.ivHomeLogo)?.setOnClickListener {
            navigateNoShift(MainActivity::class.java)
        }
        findViewById<View>(R.id.tvBrand)?.setOnClickListener {
            navigateNoShift(MainActivity::class.java)
        }
        findViewById<View>(R.id.ivTopCart)?.setOnClickListener {
            navigateNoShift(favoris::class.java)
        }

        bindNotificationEntry(R.id.ivTopNotifications)
        bindComingSoon(
            R.id.cardCategoryArtisanat,
            R.id.cardCategoryCosmetiques,
            R.id.cardCategoryEpices,
            R.id.cardCategoryVetements,
            R.id.cardCategoryDecoration,
            R.id.cardCategoryHuiles
        )
        bindSearchComingSoon(
            R.id.layoutSearchBar,
            R.id.ivSearch,
            R.id.ivFilter
        )
        startTypingHintAnimation(
            hintViewId = R.id.tvSearchHint,
            fullText = getString(R.string.search_hint_products),
            stepDelayMs = 115L,
            R.id.layoutSearchBar,
            R.id.ivSearch,
            R.id.tvSearchHint,
            R.id.ivFilter
        )
    }

    private fun polishExploreUi() {
        applyPressFeedback(
            R.id.ivHomeLogo,
            R.id.tvBrand,
            R.id.ivTopCart,
            R.id.ivTopNotifications,
            R.id.navHome,
            R.id.navExplore,
            R.id.navCart,
            R.id.navProfile,
            R.id.cardCategoryArtisanat,
            R.id.cardCategoryCosmetiques,
            R.id.cardCategoryEpices,
            R.id.cardCategoryVetements,
            R.id.cardCategoryDecoration,
            R.id.cardCategoryHuiles
        )

        window.decorView.post {
            animateExploreEntrance(
                topSectionId = R.id.layoutTopSection,
                scrollId = R.id.scrollExploreContent,
                bottomNavId = R.id.layoutBottomNav,
                cardIds = intArrayOf(
                    R.id.cardCategoryArtisanat,
                    R.id.cardCategoryCosmetiques,
                    R.id.cardCategoryEpices,
                    R.id.cardCategoryVetements,
                    R.id.cardCategoryDecoration,
                    R.id.cardCategoryHuiles
                )
            )
        }
    }
}


