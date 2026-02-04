package com.NovaStudios.custMagisk

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.NovaStudios.custMagisk.core.RootProbe

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("app_state", MODE_PRIVATE)
        if (!prefs.getBoolean("first_run_done", false)) {
            val st = RootProbe.probe(this)

            if (st.isMagiskSu && st.rootGranted) {
            } else if (st.isMagiskSu && !st.rootGranted) {
                android.app.AlertDialog.Builder(this)
                    .setTitle("Root не выдан")
                    .setMessage("Найден MagiskSU, но root-доступ не был выдан.\n\nОткройте Magisk и выдайте разрешение этому приложению.")
                    .setPositiveButton("Ок", null)
                    .show()
            } else if (st.suPath != null && !st.isMagiskSu) {
                android.app.AlertDialog.Builder(this)
                    .setTitle("Найден другой su")
                    .setMessage("Найден su, но он не похож на MagiskSU.\n\nМенеджер рассчитан на Magisk. Некоторые функции могут не работать.")
                    .setPositiveButton("Ок", null)
                    .show()
            } else {
                android.app.AlertDialog.Builder(this)
                    .setTitle("Root не найден")
                    .setMessage("su не найден. Magisk/Root отсутствуют или не установлены.")
                    .setPositiveButton("Ок", null)
                    .show()
            }

            prefs.edit().putBoolean("first_run_done", true).apply()
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, DashboardFragment())
                .commit()
        }

        findViewById<BottomNavigationView>(R.id.bottomNav).setOnItemSelectedListener { item ->
            val f = when (item.itemId) {
                R.id.tabDashboard -> DashboardFragment()
                R.id.tabMagisk -> MagiskFragment()
                R.id.tabModules -> ModulesFragment()
                R.id.tabPlugins -> PluginsFragment()
                R.id.tabSecurity -> SecurityFragment()
                else -> DashboardFragment()
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, f)
                .commit()
            true
        }
    }
}