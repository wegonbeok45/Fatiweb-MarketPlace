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
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ProfileTabFragment : Fragment(R.layout.fragment_profile_tab) {
    private var pendingLocationListener: LocationListener? = null
    private val avatarPrefsName = "profile_prefs"
    private val avatarUriKey = "avatar_uri"
    private val logTag = "ProfileTabFragment"
    private val mainHandler = Handler(Looper.getMainLooper())

    private val pickAvatarLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        val internalUri = copyUriToInternalStorage(uri)
        if (internalUri != null) {
            saveAvatarUri(internalUri)
            view?.findViewById<ImageView>(R.id.ivAvatar)?.setImageURI(internalUri)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        runCatching {
            view.findViewById<View?>(R.id.layoutBottomNav)?.isGone = true
            view.findViewById<View?>(R.id.viewBottomDivider)?.isGone = true
            view.findViewById<View?>(R.id.layoutTopBar)?.isGone = true
            view.findViewById<View?>(R.id.viewTopDivider)?.isGone = true
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
        }.onFailure { error ->
            Log.e(logTag, "Failed to initialize profile tab", error)
            (activity as? AppCompatActivity)?.showToast(getString(R.string.coming_soon))
            (activity as? MainActivity)?.selectTab(MainActivity.Tab.HOME, animate = false)
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? MainActivity)?.updateHostCartBadge()
        refreshProfileLocation()
    }

    override fun onPause() {
        val context = context
        pendingLocationListener?.let { listener ->
            val manager = context?.getSystemService(LocationManager::class.java)
            runCatching { manager?.removeUpdates(listener) }
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
        root.findViewById<View>(R.id.cardOrders)?.setOnClickListener {
            (activity as? AppCompatActivity)?.navigateNoShift(OrdersHistoryActivity::class.java)
        }
        root.findViewById<View>(R.id.cardAddresses)?.setOnClickListener {
            (activity as? AppCompatActivity)?.navigateNoShift(AddressesActivity::class.java)
        }
        (activity as? AppCompatActivity)?.bindComingSoon(R.id.cardHelp)
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

    private fun copyUriToInternalStorage(uri: Uri): Uri? {
        return runCatching {
            val context = requireContext()
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val file = java.io.File(context.filesDir, "user_avatar.jpg")
            val outputStream = java.io.FileOutputStream(file)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(file)
        }.getOrNull()
    }

    private fun saveAvatarUri(uri: Uri) {
        requireContext().getSharedPreferences(avatarPrefsName, Context.MODE_PRIVATE)
            .edit()
            .putString(avatarUriKey, uri.toString())
            .apply()
    }

    private fun restoreAvatar() {
        val context = context ?: return
        val saved = context.getSharedPreferences(avatarPrefsName, Context.MODE_PRIVATE)
            .getString(avatarUriKey, null)
            ?: return
        
        val uri = Uri.parse(saved)
        val imageView = view?.findViewById<ImageView>(R.id.ivAvatar) ?: return

        // If it's a file URI, check if file exists
        if (uri.scheme == "file") {
            val file = java.io.File(uri.path ?: "")
            if (!file.exists()) return
        }

        runCatching {
            imageView.setImageURI(uri)
        }.onFailure { e ->
            Log.e(logTag, "Failed to restore avatar", e)
        }
    }

    private val requestLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            refreshProfileLocation()
        } else {
            Log.w(logTag, "Location permissions denied")
        }
    }

    private fun refreshProfileLocation() {
        runCatching {
            val context = context ?: return
            val view = view ?: return
            val locationText = view.findViewById<TextView>(R.id.tvLocation) ?: return
            
            if (!LocationHelper.hasPermission(context)) {
                requestLocationLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
                return
            }

            LocationHelper.resolveCurrentLocation(context) { resolved ->
                activity?.runOnUiThread {
                    locationText.text = resolved
                }
            }
        }.onFailure { error ->
            Log.w(logTag, "Failed to refresh profile location", error)
        }
    }
}
