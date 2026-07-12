package com.studiolash.duplicatecleaner.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.studiolash.duplicatecleaner.databinding.ActivityScheduleBinding
import com.studiolash.duplicatecleaner.model.ScheduleSlot
import com.studiolash.duplicatecleaner.worker.ScheduleManager

class ScheduleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScheduleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Auto-Scan Schedule"

        val prefs = getSharedPreferences("schedule_prefs", MODE_PRIVATE)

        // Load saved state
        binding.switchMorning.isChecked = prefs.getBoolean("slot_MORNING", true)
        binding.switchAfternoon.isChecked = prefs.getBoolean("slot_AFTERNOON", false)
        binding.switchNight.isChecked = prefs.getBoolean("slot_NIGHT", false)

        binding.tvMorningLabel.text = "${ScheduleSlot.MORNING.label}"
        binding.tvAfternoonLabel.text = "${ScheduleSlot.AFTERNOON.label}"
        binding.tvNightLabel.text = "${ScheduleSlot.NIGHT.label}"

        binding.btnSaveSchedule.setOnClickListener {
            val enabled = buildSet {
                if (binding.switchMorning.isChecked) add(ScheduleSlot.MORNING)
                if (binding.switchAfternoon.isChecked) add(ScheduleSlot.AFTERNOON)
                if (binding.switchNight.isChecked) add(ScheduleSlot.NIGHT)
            }

            prefs.edit()
                .putBoolean("slot_MORNING", binding.switchMorning.isChecked)
                .putBoolean("slot_AFTERNOON", binding.switchAfternoon.isChecked)
                .putBoolean("slot_NIGHT", binding.switchNight.isChecked)
                .apply()

            if (enabled.isEmpty()) {
                ScheduleManager.cancelAll(this)
                Toast.makeText(this, "All scheduled scans disabled", Toast.LENGTH_SHORT).show()
            } else {
                ScheduleManager.scheduleAll(this, enabled)
                val labels = enabled.joinToString(", ") { it.label }
                Toast.makeText(this, "Schedule saved: $labels", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
