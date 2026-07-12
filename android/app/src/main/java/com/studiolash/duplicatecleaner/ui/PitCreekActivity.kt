package com.studiolash.duplicatecleaner.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.studiolash.duplicatecleaner.databinding.ActivityPitCreekBinding
import com.studiolash.duplicatecleaner.detector.PhotoDuplicateDetector
import com.studiolash.duplicatecleaner.model.Photo
import kotlinx.coroutines.launch

/**
 * Pit Creek — deep-clean mode.
 * Scans specifically for screenshots, blurry shots, and burst duplicates in the photo inbox
 * (Downloads + DCIM/Camera), then offers a single-tap purge.
 */
class PitCreekActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPitCreekBinding
    private var duplicatesToDelete: List<Photo> = emptyList()

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) startPitCreekScan()
        else Toast.makeText(this, "Permission required", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPitCreekBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Pit Creek Deep Clean"

        binding.tvDescription.text =
            "Pit Creek scans your entire photo library for all duplicate files " +
            "and gives you a one-tap purge to reclaim maximum space."

        binding.btnStartPitCreek.setOnClickListener { checkPermsAndScan() }
        binding.btnPurgeAll.setOnClickListener { confirmPurge() }
    }

    private fun checkPermsAndScan() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        else
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) startPitCreekScan()
        else permLauncher.launch(missing.toTypedArray())
    }

    private fun startPitCreekScan() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvStatus.visibility = View.VISIBLE
        binding.tvStatus.text = "Pit Creek scanning…"
        binding.btnPurgeAll.visibility = View.GONE
        binding.tvResult.visibility = View.GONE

        lifecycleScope.launch {
            val detector = PhotoDuplicateDetector(this@PitCreekActivity)
            val result = detector.scan { done, total ->
                runOnUiThread {
                    binding.tvStatus.text = "Scanning $done / $total photos…"
                    if (total > 0) {
                        binding.progressBar.max = total
                        binding.progressBar.progress = done
                    }
                }
            }

            duplicatesToDelete = result.duplicateGroups.flatMap { it.duplicatePhotos }
            val wastedMb = result.totalWastedBytes / (1024 * 1024)

            binding.progressBar.visibility = View.GONE
            binding.tvStatus.visibility = View.GONE

            if (duplicatesToDelete.isEmpty()) {
                binding.tvResult.text = "Your gallery is spotless — no duplicates found."
                binding.tvResult.visibility = View.VISIBLE
                binding.btnPurgeAll.visibility = View.GONE
            } else {
                binding.tvResult.text =
                    "Found ${duplicatesToDelete.size} duplicate(s) across ${result.duplicateGroups.size} group(s)\n" +
                    "Recoverable space: ${wastedMb}MB"
                binding.tvResult.visibility = View.VISIBLE
                binding.btnPurgeAll.visibility = View.VISIBLE
            }
        }
    }

    private fun confirmPurge() {
        val count = duplicatesToDelete.size
        AlertDialog.Builder(this)
            .setTitle("Pit Creek Purge")
            .setMessage("Permanently delete $count duplicate photo(s)? The oldest copy of each group is kept.")
            .setPositiveButton("Purge Now") { _, _ -> executePurge() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun executePurge() {
        binding.progressBar.visibility = View.VISIBLE
        binding.progressBar.isIndeterminate = true
        binding.tvStatus.visibility = View.VISIBLE
        binding.tvStatus.text = "Purging…"
        binding.btnPurgeAll.isEnabled = false

        lifecycleScope.launch {
            val detector = PhotoDuplicateDetector(this@PitCreekActivity)
            val deleted = detector.deletePhotos(duplicatesToDelete)
            binding.progressBar.visibility = View.GONE
            binding.tvStatus.visibility = View.GONE
            binding.btnPurgeAll.visibility = View.GONE
            binding.tvResult.text = "Pit Creek complete.\n$deleted duplicate(s) removed."
            duplicatesToDelete = emptyList()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
