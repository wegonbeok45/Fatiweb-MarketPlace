package isim.ia2y.myapplication

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import java.util.Locale

class ProfileTabFragment : Fragment(R.layout.fragment_profile_tab) {
    private var pendingLocationListener: LocationListener? = null
    private val avatarPrefsName = "profile_prefs"
    private val avatarUriKey = "avatar_uri"

    private val pickAvatarLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        saveAvatarUri(uri)
        view?.findViewById<ImageView>(R.id.ivAvatar)?.setImageURI(uri)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View?>(R.id.layoutBottomNav)?.isGone = true
        view.findViewById<View?>(R.id.viewBottomDivider)?.isGone = true
        setupProfileActions(view)
        view.findViewById<TextView>(R.id.tvUserName)?.text = getString(R.string.user_guest_name)
        restoreAvatar()
        refreshProfileLocation()
        (activity as? AppCompatActivity)?.revealViewsInOrder(
            R.id.layoutTopBar,
            R.id.layoutHeader,
            R.id.cardOrders,
            R.id.cardAddresses,
            R.id.cardSettings,
            R.id.cardHelp,
            R.id.cardLogout
        )
    }

    override fun onResume() {
        super.onResume()
        (activity as? MainActivity)?.updateHostCartBadge()
        refreshProfileLocation()
    }

    override fun onPause() {
        pendingLocationListener?.let { listener ->
            val manager = requireContext().getSystemService(LocationManager::class.java)
            manager?.removeUpdates(listener)
        }
        pendingLocationListener = null
        super.onPause()
    }

    private fun setupProfileActions(root: View) {
        root.findViewById<View>(R.id.ivBack)?.setOnClickListener {
            (activity as? MainActivity)?.selectTab(MainActivity.Tab.HOME, animate = false)
        }
        (activity as? AppCompatActivity)?.bindNotificationEntry(R.id.ivNotifications)
        root.findViewById<TextView>(R.id.tvRole)?.text = getString(R.string.profile_signup_chip)
        root.findViewById<View>(R.id.cardAvatar)?.setOnClickListener { openAvatarPicker() }
        root.findViewById<View>(R.id.cardEdit)?.setOnClickListener { openAvatarPicker() }
        root.findViewById<View>(R.id.cardRole)?.setOnClickListener {
            (activity as? AppCompatActivity)?.showAuthChoiceDialog(
                onCreateAccount = { (activity as? AppCompatActivity)?.navigateNoShift(register::class.java) },
                onExistingClient = { (activity as? AppCompatActivity)?.navigateNoShift(login::class.java) }
            )
        }
        root.findViewById<View>(R.id.tvRole)?.setOnClickListener {
            root.findViewById<View>(R.id.cardRole)?.performClick()
        }
        root.findViewById<View>(R.id.cardSettings)?.setOnClickListener {
            (activity as? AppCompatActivity)?.navigateNoShift(SettingsActivity::class.java)
        }
        (activity as? AppCompatActivity)?.bindComingSoon(
            R.id.cardOrders,
            R.id.cardAddresses,
            R.id.cardHelp
        )
        root.findViewById<View>(R.id.cardLogout)?.setOnClickListener {
            (activity as? AppCompatActivity)?.navigateNoShift(login::class.java)
        }
        (activity as? AppCompatActivity)?.applyPressFeedback(
            R.id.ivBack,
            R.id.ivNotifications,
            R.id.cardRole,
            R.id.cardAvatar,
            R.id.cardEdit,
            R.id.cardOrders,
            R.id.cardAddresses,
            R.id.cardSettings,
            R.id.cardHelp,
            R.id.cardLogout
        )
    }

    private fun openAvatarPicker() {
        pickAvatarLauncher.launch("image/*")
    }

    private fun saveAvatarUri(uri: Uri) {
        requireContext().getSharedPreferences(avatarPrefsName, Context.MODE_PRIVATE)
            .edit()
            .putString(avatarUriKey, uri.toString())
            .apply()
    }

    private fun restoreAvatar() {
        val saved = requireContext().getSharedPreferences(avatarPrefsName, Context.MODE_PRIVATE)
            .getString(avatarUriKey, null)
            ?: return
        runCatching {
            view?.findViewById<ImageView>(R.id.ivAvatar)?.setImageURI(Uri.parse(saved))
        }
    }

    private fun refreshProfileLocation() {
        val locationText = view?.findViewById<TextView>(R.id.tvLocation) ?: return
        if (!hasLocationPermission()) return

        val locationManager = requireContext().getSystemService(LocationManager::class.java) ?: return
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
        runCatching { locationManager.requestLocationUpdates(provider, 0L, 0f, listener) }
    }

    private fun hasLocationPermission(): Boolean {
        val coarse = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val fine = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
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
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        if (!Geocoder.isPresent()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                activity?.runOnUiThread {
                    val resolved = formatAddress(addresses.firstOrNull())
                    if (!resolved.isNullOrBlank()) target.text = resolved
                }
            }
            return
        }

        val addresses = runCatching {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(location.latitude, location.longitude, 1)
        }.getOrNull()

        val resolved = formatAddress(addresses?.firstOrNull())
        if (!resolved.isNullOrBlank()) target.text = resolved
    }

    private fun formatAddress(address: Address?): String? {
        address ?: return null
        val city = address.locality ?: address.subAdminArea ?: address.adminArea
        val country = address.countryName
        return when {
            !city.isNullOrBlank() && !country.isNullOrBlank() -> "$city, $country"
            !country.isNullOrBlank() -> country
            else -> null
        }
    }
}
