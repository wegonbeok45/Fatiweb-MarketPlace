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
import com.google.android.material.button.MaterialButton

class panier : AppCompatActivity() {
    private lateinit var emptyText: TextView
    private lateinit var itemsContainer: LinearLayout
    private lateinit var summaryGap: View
    private lateinit var summaryCard: View
    private lateinit var subtotalValue: TextView
    private lateinit var livraisonValue: TextView
    private lateinit var totalValue: TextView
    private var shouldAnimateListOnNextRender = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_panier)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        bindBackToMainForBottomNavScreens()
        setupBottomNavigation()
        setupPanierActions()
        bindViews()
        renderCart()
        applyPressFeedback(
            R.id.ivBack,
            R.id.flNotifications,
            R.id.btnCheckout,
            R.id.navHome,
            R.id.navExplore,
            R.id.navCart,
            R.id.navProfile
        )
        revealViewsInOrder(
            R.id.layoutTopBar,
            R.id.scrollPanierContent,
            R.id.layoutBottomNav
        )
    }

    override fun onResume() {
        super.onResume()
        renderCart()
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

    private fun setupPanierActions() {
        findViewById<View>(R.id.ivBack)?.setOnClickListener { navigateBackToMain() }
        bindNotificationEntry(R.id.flNotifications)

        findViewById<View>(R.id.btnCheckout)?.setOnClickListener {
            if (CartStore.itemCount(this) > 0) {
                showMotionSnackbar(getString(R.string.checkout_placeholder), R.id.layoutBottomNav)
            }
        }
    }

    private fun bindViews() {
        emptyText = findViewById(R.id.tvEmptyCart)
        itemsContainer = findViewById(R.id.layoutCartItemsContainer)
        summaryGap = findViewById(R.id.spaceBeforeSummary)
        summaryCard = findViewById(R.id.cardSummary)
        subtotalValue = findViewById(R.id.tvSubtotalValue)
        livraisonValue = findViewById(R.id.tvLivraisonValue)
        totalValue = findViewById(R.id.tvTotalValue)
    }

    private fun renderCart() {
        updateBottomCartBadge()
        itemsContainer.removeAllViews()

        val cart = CartStore.getCart(this)
        val lines = ProductCatalog.orderedFavorites(cart.keys).mapNotNull { product ->
            val qty = cart[product.id] ?: 0
            if (qty <= 0) null else product to qty
        }

        val hasItems = lines.isNotEmpty()
        emptyText.visibility = if (hasItems) View.GONE else View.VISIBLE
        itemsContainer.visibility = if (hasItems) View.VISIBLE else View.GONE
        summaryGap.visibility = if (hasItems) View.VISIBLE else View.GONE
        summaryCard.visibility = if (hasItems) View.VISIBLE else View.GONE
        findViewById<MaterialButton?>(R.id.btnCheckout)?.isEnabled = hasItems

        if (!hasItems) return

        val inflater = LayoutInflater.from(this)
        lines.forEachIndexed { index, (product, qty) ->
            val row = inflater.inflate(R.layout.item_panier_product_dynamic, itemsContainer, false)
            val params = (row.layoutParams as? LinearLayout.LayoutParams)
                ?: LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            params.topMargin = if (index == 0) 0 else resources.getDimensionPixelSize(R.dimen.panier_product_card_spacing)
            row.layoutParams = params

            row.findViewById<ImageView>(R.id.ivCartItemImage)?.setImageResource(product.imageRes)
            row.findViewById<TextView>(R.id.tvCartItemTitle)?.text = product.title
            row.findViewById<TextView>(R.id.tvCartItemSubtitle)?.text = product.subtitle
            row.findViewById<TextView>(R.id.tvCartItemPrice)?.text = formatDt(product.unitPrice * qty)
            row.findViewById<TextView>(R.id.tvQtyValue)?.text = qty.toString()

            row.findViewById<View>(R.id.btnRemoveItem)?.setOnClickListener {
                CartStore.remove(this, product.id)
                shouldAnimateListOnNextRender = false
                renderCart()
            }
            row.findViewById<View>(R.id.btnQtyMinus)?.setOnClickListener {
                CartStore.decrement(this, product.id)
                shouldAnimateListOnNextRender = false
                renderCart()
            }
            row.findViewById<View>(R.id.btnQtyPlus)?.setOnClickListener {
                CartStore.increment(this, product.id)
                shouldAnimateListOnNextRender = false
                renderCart()
            }
            row.setOnClickListener {
                navigateToProductDetails(product.id)
            }

            itemsContainer.addView(row)
            if (shouldAnimateListOnNextRender) {
                animateListItemEntry(row, index, startDelayMs = 45L)
            }
        }

        val subtotal = lines.sumOf { (product, qty) -> product.unitPrice * qty }
        val livraison = CartStore.LIVRAISON_FEE
        subtotalValue.text = formatDt(subtotal)
        livraisonValue.text = formatDt(livraison)
        totalValue.text = formatDt(subtotal + livraison)
        if (shouldAnimateListOnNextRender) {
            revealSingleView(R.id.cardSummary)
        }
        shouldAnimateListOnNextRender = false
    }
}
