package com.studiolash.duplicatecleaner.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.studiolash.duplicatecleaner.databinding.ActivityDuplicatesBinding
import com.studiolash.duplicatecleaner.detector.PhotoDuplicateDetector
import com.studiolash.duplicatecleaner.model.ScanResult
import kotlinx.coroutines.launch

class DuplicatesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDuplicatesBinding
    private lateinit var detector: PhotoDuplicateDetector
    private var scanResult: ScanResult? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDuplicatesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Duplicate Photos"

        detector = PhotoDuplicateDetector(this)

        val autoDelete = intent.getBooleanExtra(EXTRA_AUTO_DELETE, false)
        val liveScan = intent.getBooleanExtra(EXTRA_LIVE_SCAN, false)

        when {
            autoDelete -> runScanThenDeleteAll()
            liveScan -> runLiveScan()
            pendingResult != null -> displayResult(pendingResult!!)
            else -> runLiveScan()
        }

        binding.btnDeleteAll.setOnClickListener { confirmDeleteAll(advanced = false) }
        binding.btnAdvancedDelete.setOnClickListener { confirmDeleteAll(advanced = true) }
        binding.btnEditDuplicates.setOnClickListener { openEditor() }
    }

    private fun runLiveScan() {
        showScanning()
        lifecycleScope.launch {
            val result = detector.scan { done, total ->
                runOnUiThread {
                    binding.tvProgress.text = "Scanning: $done / $total"
                    if (total > 0) {
                        binding.progressBar.max = total
                        binding.progressBar.progress = done
                    }
                }
            }
            pendingResult = result
            displayResult(result)
        }
    }

    private fun runScanThenDeleteAll() {
        showScanning()
        lifecycleScope.launch {
            val result = detector.scan()
            val duplicates = result.duplicateGroups.flatMap { it.duplicatePhotos }
            val deleted = detector.deletePhotos(duplicates)
            showDeleteResult(deleted)
        }
    }

    private fun displayResult(result: ScanResult) {
        scanResult = result
        binding.progressBar.visibility = View.GONE
        binding.tvProgress.visibility = View.GONE

        if (result.duplicateGroups.isEmpty()) {
            binding.tvSummary.text = "No duplicates found in ${result.totalScanned} photos. Your gallery is clean!"
            binding.btnDeleteAll.visibility = View.GONE
            binding.btnAdvancedDelete.visibility = View.GONE
            binding.btnEditDuplicates.visibility = View.GONE
            binding.recyclerDuplicates.visibility = View.GONE
        } else {
            val wastedMb = result.totalWastedBytes / (1024 * 1024)
            binding.tvSummary.text =
                "Found ${result.totalDuplicates} duplicate(s) in ${result.duplicateGroups.size} group(s)\n" +
                "Wasted space: ${wastedMb}MB"
            binding.btnDeleteAll.visibility = View.VISIBLE
            binding.btnAdvancedDelete.visibility = View.VISIBLE
            binding.btnEditDuplicates.visibility = View.VISIBLE

            val adapter = DuplicateGroupAdapter(result.duplicateGroups)
            binding.recyclerDuplicates.adapter = adapter
            binding.recyclerDuplicates.visibility = View.VISIBLE
        }
    }

    private fun confirmDeleteAll(advanced: Boolean) {
        val result = scanResult ?: return
        val count = result.totalDuplicates
        val wastedMb = result.totalWastedBytes / (1024 * 1024)

        val message = if (advanced) {
            "Delete all $count duplicate(s) and free ${wastedMb}MB?\n\nThe oldest copy of each group will be kept."
        } else {
            "Delete $count duplicate photo(s)?"
        }

        AlertDialog.Builder(this)
            .setTitle("Delete Duplicates")
            .setMessage(message)
            .setPositiveButton("Delete All") { _, _ ->
                deleteAll(result)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAll(result: ScanResult) {
        showScanning()
        binding.tvProgress.text = "Deleting duplicates…"
        lifecycleScope.launch {
            val duplicates = result.duplicateGroups.flatMap { it.duplicatePhotos }
            val deleted = detector.deletePhotos(duplicates)
            showDeleteResult(deleted)
        }
    }

    private fun showDeleteResult(deleted: Int) {
        binding.progressBar.visibility = View.GONE
        binding.tvProgress.visibility = View.GONE
        binding.tvSummary.text = "Done! Removed $deleted duplicate photo(s)."
        binding.btnDeleteAll.visibility = View.GONE
        binding.btnAdvancedDelete.visibility = View.GONE
        binding.btnEditDuplicates.visibility = View.GONE
        binding.recyclerDuplicates.visibility = View.GONE
        pendingResult = null
    }

    private fun openEditor() {
        Toast.makeText(this, "Tap individual photos in each group to select which to keep", Toast.LENGTH_LONG).show()
        val result = scanResult ?: return
        val adapter = binding.recyclerDuplicates.adapter as? DuplicateGroupAdapter ?: return
        adapter.setEditMode(true)
    }

    private fun showScanning() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvProgress.visibility = View.VISIBLE
        binding.tvProgress.text = "Preparing scan…"
        binding.tvSummary.text = ""
        binding.btnDeleteAll.visibility = View.GONE
        binding.btnAdvancedDelete.visibility = View.GONE
        binding.btnEditDuplicates.visibility = View.GONE
        binding.recyclerDuplicates.visibility = View.GONE
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    companion object {
        const val EXTRA_AUTO_DELETE = "extra_auto_delete"
        const val EXTRA_LIVE_SCAN = "extra_live_scan"

        // Simple in-process cache to pass scan result from background worker
        @Volatile
        var pendingResult: ScanResult? = null
    }
}
