package isim.ia2y.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Locale

class user : AppCompatActivity() {
    private var pendingLocationListener: LocationListener? = null
    private val avatarPrefsName = "profile_prefs"
    private val avatarUriKey = "avatar_uri"
    private val logTag = "UserActivity"

    private val pickAvatarLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        saveAvatarUri(uri)
        findViewById<ImageView>(R.id.ivAvatar)?.setImageURI(uri)
    }

    override fun onResume() {
        super.onResume()
        runCatching {
            updateBottomCartBadge()
        }.onFailure { error ->
            Log.w(logTag, "onResume update failed", error)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching {
            enableEdgeToEdge()
            setContentView(R.layout.activity_user)
            findViewById<View?>(R.id.main)?.let { root ->
                ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
                    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                    insets
                }
            }
            bindBackToMainForBottomNavScreens()
            setupBottomNavigation()
            updateBottomCartBadge()
            setupProfileActions()
            findViewById<TextView>(R.id.tvUserName)?.text = getString(R.string.user_guest_name)
            restoreAvatar()
            refreshProfileLocation()
            revealViewsInOrder(
                R.id.layoutTopBar,
                R.id.layoutHeader,
                R.id.cardOrders,
                R.id.cardAddresses,
                R.id.cardSettings,
                R.id.cardHelp,
                R.id.cardLogout,
                R.id.layoutBottomNav
            )
        }.onFailure { error ->
            Log.e(logTag, "Failed to initialize profile screen", error)
            showToast(getString(R.string.coming_soon))
            navigateBackToMain()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (handleNotificationPermissionResult(requestCode, grantResults)) return
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onPause() {
        runCatching {
            pendingLocationListener?.let { listener ->
                val manager = getSystemService(LocationManager::class.java)
                manager?.removeUpdates(listener)
            }
        }.onFailure { error ->
            Log.w(logTag, "onPause cleanup failed", error)
        }
        pendingLocationListener = null
        super.onPause()
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

    private fun setupProfileActions() {
        findViewById<View>(R.id.ivBack)?.setOnClickListener { navigateBackToMain() }
        bindNotificationEntry(R.id.ivNotifications)
        findViewById<TextView>(R.id.tvRole)?.text = getString(R.string.profile_signup_chip)
        findViewById<View>(R.id.cardAvatar)?.setOnClickListener { openAvatarPicker() }
        findViewById<View>(R.id.cardEdit)?.setOnClickListener { openAvatarPicker() }
        findViewById<View>(R.id.cardRole)?.setOnClickListener {
            showAuthChoiceDialog(
                onCreateAccount = { navigateNoShift(register::class.java) },
                onExistingClient = { navigateNoShift(login::class.java) }
            )
        }
        findViewById<View>(R.id.tvRole)?.setOnClickListener {
            findViewById<View>(R.id.cardRole)?.performClick()
        }
        findViewById<View>(R.id.cardSettings)?.setOnClickListener {
            navigateNoShift(SettingsActivity::class.java)
        }
        findViewById<View>(R.id.cardOrders)?.setOnClickListener {
            navigateNoShift(OrdersHistoryActivity::class.java)
        }
        findViewById<View>(R.id.cardAddresses)?.setOnClickListener {
            navigateNoShift(AddressesActivity::class.java)
        }
        bindComingSoon(R.id.cardHelp)
        findViewById<View>(R.id.cardLogout)?.setOnClickListener {
            navigateNoShift(login::class.java)
        }
        applyPressFeedback(
            R.id.ivBack,
            R.id.ivNotifications,
            R.id.cardRole,
            R.id.cardAvatar,
            R.id.cardEdit,
            R.id.cardOrders,
            R.id.cardAddresses,
            R.id.cardSettings,
            R.id.cardHelp,
            R.id.cardLogout,
            R.id.navHome,
            R.id.navExplore,
            R.id.navCart,
            R.id.navProfile
        )
        emphasizeCta(R.id.cardLogout, delayMs = 420L)
    }

    private fun openAvatarPicker() {
        pickAvatarLauncher.launch("image/*")
    }

    private fun saveAvatarUri(uri: Uri) {
        getSharedPreferences(avatarPrefsName, MODE_PRIVATE)
            .edit()
            .putString(avatarUriKey, uri.toString())
            .apply()
    }

    private fun restoreAvatar() {
        val saved = getSharedPreferences(avatarPrefsName, MODE_PRIVATE)
            .getString(avatarUriKey, null)
            ?: return
        runCatching {
            findViewById<ImageView>(R.id.ivAvatar)?.setImageURI(Uri.parse(saved))
        }
    }

    private fun refreshProfileLocation() {
        runCatching {
            val locationText = findViewById<TextView>(R.id.tvLocation) ?: return
            if (!hasLocationPermission()) {
                return
            }

            val locationManager = getSystemService(LocationManager::class.java) ?: return
            val location = getBestLastKnownLocation(locationManager)
            if (location != null) {
                reverseGeocodeAndSet(location, locationText)
                return
            }

            val providers = listOf(
                LocationManager.NETWORK_PROVIDER,
                LocationManager.GPS_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )

            val provider = providers.firstOrNull {
                runCatching { locationManager.isProviderEnabled(it) }.getOrDefault(false)
            } ?: return

            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    reverseGeocodeAndSet(location, locationText)
                    locationManager.removeUpdates(this)
                    if (pendingLocationListener == this) pendingLocationListener = null
                }
            }
            pendingLocationListener = listener
            runCatching {
                locationManager.requestLocationUpdates(provider, 0L, 0f, listener)
            }
        }.onFailure { error ->
            Log.w(logTag, "Failed to refresh profile location", error)
        }
    }

    private fun hasLocationPermission(): Boolean {
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return coarse || fine
    }

    private fun getBestLastKnownLocation(locationManager: LocationManager): Location? {
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )

        return providers
            .mapNotNull { provider ->
                runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
            }
            .maxByOrNull { it.time }
    }

    private fun reverseGeocodeAndSet(location: Location, target: TextView) {
        runCatching {
            val geocoder = Geocoder(this, Locale.getDefault())
            if (!Geocoder.isPresent()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                    runOnUiThread {
                        val resolved = formatAddress(addresses.firstOrNull())
                        if (!resolved.isNullOrBlank()) {
                            target.text = resolved
                        }
                    }
                }
                return
            }

            val addresses = runCatching {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(location.latitude, location.longitude, 1)
            }.getOrNull()

            val resolved = formatAddress(addresses?.firstOrNull())
            if (!resolved.isNullOrBlank()) {
                target.text = resolved
            }
        }.onFailure { error ->
            Log.w(logTag, "Reverse geocoding failed", error)
        }
    }

    private fun formatAddress(address: Address?): String? {
        address ?: return null
        val city = address.locality
            ?: address.subAdminArea
            ?: address.adminArea
        val country = address.countryName
        return when {
            !city.isNullOrBlank() && !country.isNullOrBlank() -> "$city, $country"
            !country.isNullOrBlank() -> country
            else -> null
        }
    }
}


