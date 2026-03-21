package com.rtech.cartly

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.view.WindowManager
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class ContainerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_container)

        window.navigationBarColor = android.graphics.Color.parseColor("#F8F8F8")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            window.decorView.systemUiVisibility =
                android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        loadFragment(DealsFragment())

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_deals -> loadFragment(DealsFragment())
                R.id.nav_search -> loadFragment(SearchFragment())
                R.id.nav_basket -> loadFragment(BasketFragment())
                R.id.nav_profile -> loadFragment(ProfileFragment())
            }
            true
        }
    }    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}