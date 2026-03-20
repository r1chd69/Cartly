package com.rtech.cartly

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val allDeals = mutableListOf<Map<String, String>>()
    private val favouriteNames = mutableSetOf<String>()
    private lateinit var dealsContainer: LinearLayout
    private lateinit var locationLabel: TextView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private var selectedStore = "All"
    private var searchQuery = ""
    private val filterButtons = mutableListOf<TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dealsContainer = findViewById(R.id.dealsContainer)
        locationLabel = findViewById(R.id.locationLabel)
        swipeRefresh = findViewById(R.id.swipeRefresh)

        swipeRefresh.setColorSchemeColors(
            ContextCompat.getColor(this, R.color.cartlyGreen)
        )

        swipeRefresh.setOnRefreshListener {
            loadFavourites()
        }

        val prefs = getSharedPreferences("CartlyPrefs", Context.MODE_PRIVATE)
        locationLabel.text = prefs.getString("location", "Sandton, Johannesburg")

        val searchBar = findViewById<EditText>(R.id.searchBar)
        searchBar.addTextChangedListener { text ->
            searchQuery = text.toString()
            filterDeals()
        }

        val btnSettings = findViewById<TextView>(R.id.btnSettings)
        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        val filterAll = findViewById<TextView>(R.id.filterAll)
        val filterCheckers = findViewById<TextView>(R.id.filterCheckers)
        val filterPnP = findViewById<TextView>(R.id.filterPnP)
        val filterShoprite = findViewById<TextView>(R.id.filterShoprite)
        val filterSpar = findViewById<TextView>(R.id.filterSpar)

        filterButtons.addAll(listOf(filterAll, filterCheckers, filterPnP, filterShoprite, filterSpar))
        val filterNames = listOf("All", "Checkers", "Pick n Pay", "Shoprite", "Spar")

        filterButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                selectedStore = filterNames[index]
                updateFilterButtons(index)
                filterDeals()
            }
        }

        loadFavourites()
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("CartlyPrefs", Context.MODE_PRIVATE)
        locationLabel.text = prefs.getString("location", "Sandton, Johannesburg")
    }

    private fun isDarkMode(): Boolean {
        return resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    private fun updateFilterButtons(activeIndex: Int) {
        filterButtons.forEachIndexed { index, button ->
            if (index == activeIndex) {
                button.setBackgroundResource(R.drawable.filter_active)
                button.setTextColor(Color.WHITE)
            } else {
                button.setBackgroundResource(R.drawable.filter_inactive)
                button.setTextColor(ContextCompat.getColor(this, R.color.cartlyGreen))
            }
        }
    }

    private fun loadFavourites() {
        db.collection("favourites")
            .get()
            .addOnSuccessListener { result ->
                favouriteNames.clear()
                for (document in result) {
                    val name = document.getString("name") ?: ""
                    if (name != "placeholder") {
                        favouriteNames.add(name)
                    }
                }
                loadDealsFromFirebase()
            }
    }

    private fun loadDealsFromFirebase() {
        db.collection("deals")
            .get()
            .addOnSuccessListener { result ->
                allDeals.clear()
                for (document in result) {
                    allDeals.add(mapOf(
                        "name" to (document.getString("name") ?: ""),
                        "store" to (document.getString("store") ?: ""),
                        "distance" to (document.getString("distance") ?: ""),
                        "price_now" to (document.getString("price_now") ?: ""),
                        "price_was" to (document.getString("price_was") ?: ""),
                        "discount" to (document.getString("discount") ?: ""),
                        "emoji" to (document.getString("emoji") ?: "🛒")
                    ))
                }
                filterDeals()
                swipeRefresh.isRefreshing = false
            }
            .addOnFailureListener { e ->
                android.util.Log.e("CARTLY", "Error: $e")
                swipeRefresh.isRefreshing = false
            }
    }

    private fun filterDeals() {
        dealsContainer.removeAllViews()

        val filtered = allDeals.filter { deal ->
            val matchesStore = selectedStore == "All" || deal["store"] == selectedStore
            val matchesSearch = searchQuery.isEmpty() ||
                    deal["name"]!!.contains(searchQuery, ignoreCase = true) ||
                    deal["store"]!!.contains(searchQuery, ignoreCase = true)
            matchesStore && matchesSearch
        }

        if (filtered.isEmpty()) {
            val noResults = TextView(this).apply {
                text = "No deals found"
                textSize = 14f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.textSecondary))
                gravity = Gravity.CENTER
                setPadding(0, 60, 0, 0)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            dealsContainer.addView(noResults)
        } else {
            for (deal in filtered) {
                val card = createDealCard(
                    deal["name"]!!, deal["store"]!!, deal["distance"]!!,
                    deal["price_now"]!!, deal["price_was"]!!, deal["discount"]!!, deal["emoji"]!!
                )
                dealsContainer.addView(card)
            }
        }
    }

    private fun toggleFavourite(name: String, heartBtn: TextView) {
        if (favouriteNames.contains(name)) {
            favouriteNames.remove(name)
            heartBtn.text = "♡"
            heartBtn.setTextColor(ContextCompat.getColor(this, R.color.textSecondary))
            db.collection("favourites")
                .whereEqualTo("name", name)
                .get()
                .addOnSuccessListener { result ->
                    for (document in result) {
                        document.reference.delete()
                    }
                }
        } else {
            favouriteNames.add(name)
            heartBtn.text = "♥"
            heartBtn.setTextColor(Color.parseColor("#E24B4A"))
            db.collection("favourites").add(mapOf("name" to name))
        }
    }

    private fun createDealCard(
        name: String, store: String, distance: String,
        priceNow: String, priceWas: String, discount: String, emoji: String
    ): LinearLayout {

        val cardBg = if (isDarkMode()) R.drawable.card_background_dark else R.drawable.card_background

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(cardBg)
            setPadding(24, 24, 24, 16)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(28, 0, 28, 20)
            layoutParams = params
            elevation = 4f
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            isClickable = true
            isFocusable = true
            setOnClickListener {
                val intent = Intent(this@MainActivity, DealDetailActivity::class.java)
                intent.putExtra("name", name)
                intent.putExtra("store", store)
                intent.putExtra("distance", distance)
                intent.putExtra("price_now", priceNow)
                intent.putExtra("price_was", priceWas)
                intent.putExtra("discount", discount)
                intent.putExtra("emoji", emoji)
                startActivity(intent)
            }
        }

        val emojiView = TextView(this).apply {
            text = emoji
            textSize = 32f
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 24, 0)
            layoutParams = params
        }

        val infoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val nameView = TextView(this).apply {
            text = name
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.textPrimary))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val storeView = TextView(this).apply {
            text = "$store • $distance away"
            textSize = 12f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.textSecondary))
        }

        val discountView = TextView(this).apply {
            text = discount
            textSize = 11f
            setTextColor(Color.parseColor("#A32D2D"))
            setBackgroundColor(Color.parseColor("#FCEBEB"))
            setPadding(6, 4, 6, 4)
        }

        infoLayout.addView(nameView)
        infoLayout.addView(storeView)
        infoLayout.addView(discountView)

        val priceLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
        }

        val priceNowView = TextView(this).apply {
            text = priceNow
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.cartlyGreen))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val priceWasView = TextView(this).apply {
            text = priceWas
            textSize = 12f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.textSecondary))
        }

        priceLayout.addView(priceNowView)
        priceLayout.addView(priceWasView)

        topRow.addView(emojiView)
        topRow.addView(infoLayout)
        topRow.addView(priceLayout)

        val bottomRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val isFav = favouriteNames.contains(name)
        val heartBtn = TextView(this).apply {
            text = if (isFav) "♥" else "♡"
            textSize = 20f
            setTextColor(if (isFav) Color.parseColor("#E24B4A") else
                ContextCompat.getColor(this@MainActivity, R.color.textSecondary))
            setPadding(0, 8, 0, 0)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                toggleFavourite(name, this)
            }
        }

        bottomRow.addView(heartBtn)
        card.addView(topRow)
        card.addView(bottomRow)

        return card
    }
}