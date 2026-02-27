package isim.ia2y.myapplication

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.Collections
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TabDataPrefetcher(context: Context) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val ioExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val warmedTabs = Collections.synchronizedSet(mutableSetOf<MainActivity.Tab>())

    fun preload(tab: MainActivity.Tab, force: Boolean = false, callback: (Result<Unit>) -> Unit) {
        if (!force && warmedTabs.contains(tab)) {
            mainHandler.post { callback(Result.success(Unit)) }
            return
        }

        ioExecutor.execute {
            val result = runCatching {
                when (tab) {
                    MainActivity.Tab.HOME -> {
                        ProductCatalog.all().size
                        FavoritesStore.getFavorites(appContext).size
                    }

                    MainActivity.Tab.EXPLORE -> {
                        ProductCatalog.all().size
                    }

                    MainActivity.Tab.CART -> {
                        val cart = CartStore.getCart(appContext)
                        ProductCatalog.orderedFavorites(cart.keys)
                    }

                    MainActivity.Tab.PROFILE -> {
                        appContext.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)
                            .getString("avatar_uri", null)
                    }
                }
                warmedTabs.add(tab)
                Unit
            }
            mainHandler.post { callback(result) }
        }
    }

    fun shutdown() {
        ioExecutor.shutdownNow()
    }
}
