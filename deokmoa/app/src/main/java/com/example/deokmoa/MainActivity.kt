package com.example.deokmoa

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.Fragment
import com.example.deokmoa.databinding.ActivityMainBinding
import com.example.deokmoa.ui.FilterFragment
import com.example.deokmoa.ui.HomeFragment
import com.example.deokmoa.ui.RankingFragment
import com.example.deokmoa.ui.SettingFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        //Splash screen 추가
        installSplashScreen()

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        setBottomNavigationView()

        // 앱이 처음 실행될 때 홈 프래그먼트를 표시
        if (savedInstanceState == null) {
            binding.bottomNavigationView.selectedItemId = R.id.fragment_home
        }
    }
    private fun setBottomNavigationView() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.fragment_home -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.fragment_ranking -> {
                    replaceFragment(RankingFragment())
                    true
                }
                R.id.fragment_flitter -> {
                    replaceFragment(FilterFragment())
                    true
                }
                R.id.fragment_setting -> {
                    replaceFragment(SettingFragment())
                    true
                }
                else -> false
            }
        }
    }
    // 프래그먼트를 교체하는 공통 함수
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_container, fragment)
            .commit()
    }
}
