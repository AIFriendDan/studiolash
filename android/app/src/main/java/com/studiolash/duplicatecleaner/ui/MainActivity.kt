package com.studiolash.duplicatecleaner.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.studiolash.duplicatecleaner.databinding.ActivityMainBinding
import com.studiolash.duplicatecleaner.worker.ScheduleManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.all { it }
        if (granted) startScan()
        else Toast.makeText(this, "Storage permission required to scan photos", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnScanNow.setOnClickListener { checkPermissionsAndScan() }
        binding.btnSchedule.setOnClickListener {
            startActivity(Intent(this, ScheduleActivity::class.java))
        }
        binding.btnPitCreek.setOnClickListener {
            startActivity(Intent(this, PitCreekActivity::class.java))
        }
    }

    private fun checkPermissionsAndScan() {
        val permissions = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_IMAGES)
                add(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }.toTypedArray()

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            startScan()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun startScan() {
        ScheduleManager.runNow(this)
        startActivity(Intent(this, DuplicatesActivity::class.java).apply {
            putExtra(DuplicatesActivity.EXTRA_LIVE_SCAN, true)
        })
    }
}
