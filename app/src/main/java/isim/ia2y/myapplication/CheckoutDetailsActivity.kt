package isim.ia2y.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.card.MaterialCardView

class CheckoutDetailsActivity : AppCompatActivity() {

    private val EXPRESS_FEE = 12.500

    /** true = Standard selected, false = Express selected */
    private var isStandardSelected = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_checkout_details)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layoutCheckoutRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupActions()
        bindDynamicData()
        applyDeliverySelection()
        
        revealViewsInOrder(
            R.id.layoutCheckoutTopBar,
            R.id.scrollCheckoutContent,
            R.id.layoutCheckoutBottomBar
        )
        applyPressFeedback(
            R.id.tvCheckoutBack,
            R.id.tvCheckoutModifyAddress,
            R.id.cardCheckoutAddress,
            R.id.cardDeliveryStandard,
            R.id.cardDeliveryExpress,
            R.id.btnCheckoutContinue
        )
    }

    private fun setupActions() {
        // Back navigation
        findViewById<View>(R.id.tvCheckoutBack)?.setOnClickListener {
            finishWithMotion()
        }

        // Modify address — placeholder
        findViewById<View>(R.id.tvCheckoutModifyAddress)?.setOnClickListener {
            showMotionSnackbar(getString(R.string.coming_soon))
        }

        // Delivery method toggle
        findViewById<View>(R.id.cardDeliveryStandard)?.setOnClickListener {
            if (!isStandardSelected) {
                isStandardSelected = true
                applyDeliverySelection()
            }
        }
        findViewById<View>(R.id.cardDeliveryExpress)?.setOnClickListener {
            if (isStandardSelected) {
                isStandardSelected = false
                applyDeliverySelection()
            }
        }

        // CTA
        findViewById<View>(R.id.btnCheckoutContinue)?.setOnClickListener {
            showMotionSnackbar(getString(R.string.checkout_placeholder))
        }
    }

    private fun bindDynamicData() {
        // 1. Address
        val address = AddressBookStore.getAddresses(this).firstOrNull() ?: "Tunis, Tunisie"
        findViewById<TextView>(R.id.tvCheckoutAddressName)?.text = getString(R.string.user_guest_name)
        findViewById<TextView>(R.id.tvCheckoutAddressLine1)?.text = address
        findViewById<TextView>(R.id.tvCheckoutAddressLine2)?.visibility = View.GONE
        findViewById<TextView>(R.id.tvCheckoutAddressPhone)?.visibility = View.GONE

        // 2. Articles Thumbnails
        val tray = findViewById<LinearLayout>(R.id.layoutCheckoutArticles)
        tray?.removeAllViews()
        val cart = CartStore.getCart(this)
        val items = ProductCatalog.orderedFavorites(cart.keys)
        
        val inflater = LayoutInflater.from(this)
        items.forEach { product ->
            val card = inflater.inflate(R.layout.item_checkout_thumbnail, tray, false) as MaterialCardView
            card.findViewById<ImageView>(R.id.ivThumbnail)?.setImageResource(product.imageRes)
            tray?.addView(card)
        }

        // 3. Summary
        updateSummary()
    }

    private fun updateSummary() {
        val subtotal = CartStore.subtotal(this)
        val shipping = if (isStandardSelected) 0.0 else EXPRESS_FEE
        val total = subtotal + shipping

        findViewById<TextView>(R.id.tvCheckoutSubtotal)?.text = formatDt(subtotal)
        
        val tvShippingLabel = findViewById<TextView>(R.id.tvCheckoutShippingLabel)
        val tvShippingValue = findViewById<TextView>(R.id.tvCheckoutShippingValue)
        
        if (isStandardSelected) {
            tvShippingLabel?.text = "Frais de port (Standard)"
            tvShippingValue?.text = "GRATUIT"
        } else {
            tvShippingLabel?.text = "Frais de port (Express)"
            tvShippingValue?.text = formatDt(EXPRESS_FEE)
        }

        findViewById<TextView>(R.id.tvCheckoutTotal)?.text = formatDt(total)
    }

    private fun applyDeliverySelection() {
        val cardStandard = findViewById<MaterialCardView>(R.id.cardDeliveryStandard)
        val cardExpress  = findViewById<MaterialCardView>(R.id.cardDeliveryExpress)
        val checkStandard = findViewById<View>(R.id.ivStandardCheck)
        val radioExpress  = findViewById<View>(R.id.ivExpressRadio)

        val colorSelected = android.graphics.Color.parseColor("#CDAA7D")
        val colorUnselected = android.graphics.Color.parseColor("#EFEBE4")
        val strokeSelected = resources.getDimensionPixelSize(R.dimen.checkout_selected_stroke)
        val strokeUnselected = resources.getDimensionPixelSize(R.dimen.checkout_unselected_stroke)

        if (isStandardSelected) {
            cardStandard?.strokeColor = colorSelected
            cardStandard?.strokeWidth = strokeSelected
            checkStandard?.visibility = View.VISIBLE

            cardExpress?.strokeColor = colorUnselected
            cardExpress?.strokeWidth = strokeUnselected
            radioExpress?.visibility = View.VISIBLE
        } else {
            cardStandard?.strokeColor = colorUnselected
            cardStandard?.strokeWidth = strokeUnselected
            checkStandard?.visibility = View.GONE

            cardExpress?.strokeColor = colorSelected
            cardExpress?.strokeWidth = strokeSelected
            radioExpress?.visibility = View.GONE
        }
        
        updateSummary()
    }
}
