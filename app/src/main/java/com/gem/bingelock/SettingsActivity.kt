package com.gem.bingelock

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefsHelper: PrefsHelper

    private lateinit var batteryFloorLabel: TextView
    private lateinit var batterySeek: SeekBar

    private lateinit var faceDownSwitch: SwitchCompat
    private lateinit var faceDownDurationContainer: LinearLayout
    private lateinit var faceDownDurationLabel: TextView
    private lateinit var faceDownDurationSeek: SeekBar

    private lateinit var ytCheck: CheckBox
    private lateinit var ytMorpheCheck: CheckBox

    private lateinit var promptPhrasesEdit: EditText
    private lateinit var actionButtonsEdit: EditText

    private lateinit var btnDone: Button
    private lateinit var btnReset: Button

    private val saveHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefsHelper = PrefsHelper(this)

        batteryFloorLabel = findViewById(R.id.batteryFloorLabel)
        batterySeek = findViewById(R.id.batterySeek)

        faceDownSwitch = findViewById(R.id.faceDownSwitch)
        faceDownDurationContainer = findViewById(R.id.faceDownDurationContainer)
        faceDownDurationLabel = findViewById(R.id.faceDownDurationLabel)
        faceDownDurationSeek = findViewById(R.id.faceDownDurationSeek)

        ytCheck = findViewById(R.id.ytCheck)
        ytMorpheCheck = findViewById(R.id.ytMorpheCheck)

        promptPhrasesEdit = findViewById(R.id.promptPhrasesEdit)
        actionButtonsEdit = findViewById(R.id.actionButtonsEdit)

        btnDone = findViewById(R.id.btnDone)
        btnReset = findViewById(R.id.btnReset)

        setupListeners()
        loadPreferencesIntoUi()
    }

    private fun setupListeners() {
        // Battery floor SeekBar: 5% to 50% (min 5, progress 0..45)
        bindSeekBar(
            seekBar = batterySeek,
            minValue = 5,
            formatText = { value -> "Disable BingeLock below: $value%" },
            labelView = batteryFloorLabel,
            onValueChanged = { value -> prefsHelper.batteryFloor = value }
        )

        // Face-down toggle
        faceDownSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefsHelper.faceDownEnabled = isChecked
            updateFaceDownDurationState(isChecked)
        }

        // Face-down duration SeekBar: 5 to 60 seconds (min 5, progress 0..55)
        bindSeekBar(
            seekBar = faceDownDurationSeek,
            minValue = 5,
            formatText = { value -> "Pause after: $value seconds" },
            labelView = faceDownDurationLabel,
            onValueChanged = { value -> prefsHelper.faceDownDurationMs = value * 1000L }
        )

        // Target apps checkboxes
        ytCheck.setOnCheckedChangeListener { _, isChecked ->
            handleTargetAppChanged("com.google.android.youtube", isChecked, ytCheck)
        }

        ytMorpheCheck.setOnCheckedChangeListener { _, isChecked ->
            handleTargetAppChanged("app.morphe.android.youtube", isChecked, ytMorpheCheck)
        }

        // Debounced text saving
        attachDebouncedSave(promptPhrasesEdit) { prefsHelper.promptPhrases = it }
        attachDebouncedSave(actionButtonsEdit) { prefsHelper.actionButtons = it }

        btnDone.setOnClickListener {
            finish()
        }

        btnReset.setOnClickListener {
            prefsHelper.resetToDefaults()
            loadPreferencesIntoUi()
            Toast.makeText(this, "Settings reset to defaults", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadPreferencesIntoUi() {
        // Battery floor (range 5..50)
        val currentBattery = prefsHelper.batteryFloor.coerceIn(5, 50)
        batterySeek.progress = currentBattery - 5
        batteryFloorLabel.text = "Disable BingeLock below: $currentBattery%"

        // Face-down enabled & duration
        val isFaceDownOn = prefsHelper.faceDownEnabled
        faceDownSwitch.isChecked = isFaceDownOn
        updateFaceDownDurationState(isFaceDownOn)

        val currentDurationSec = (prefsHelper.faceDownDurationMs / 1000L).toInt().coerceIn(5, 60)
        faceDownDurationSeek.progress = currentDurationSec - 5
        faceDownDurationLabel.text = "Pause after: $currentDurationSec seconds"

        // Target apps
        val targets = prefsHelper.targetPackages
        ytCheck.isChecked = targets.contains("com.google.android.youtube")
        ytMorpheCheck.isChecked = targets.contains("app.morphe.android.youtube")

        // Advanced
        promptPhrasesEdit.setText(prefsHelper.promptPhrases)
        actionButtonsEdit.setText(prefsHelper.actionButtons)
    }

    private fun updateFaceDownDurationState(enabled: Boolean) {
        faceDownDurationContainer.isEnabled = enabled
        faceDownDurationLabel.isEnabled = enabled
        faceDownDurationSeek.isEnabled = enabled
        faceDownDurationContainer.alpha = if (enabled) 1.0f else 0.4f
    }

    private fun handleTargetAppChanged(pkg: String, isChecked: Boolean, checkBox: CheckBox) {
        val currentTargets = prefsHelper.targetPackages.toMutableSet()
        if (isChecked) {
            currentTargets.add(pkg)
            prefsHelper.targetPackages = currentTargets
        } else {
            if (currentTargets.size <= 1 && currentTargets.contains(pkg)) {
                // Prevent unchecking the last item
                checkBox.isChecked = true
                Toast.makeText(this, "At least one target app is required", Toast.LENGTH_SHORT).show()
            } else {
                currentTargets.remove(pkg)
                prefsHelper.targetPackages = currentTargets
            }
        }
    }

    private fun attachDebouncedSave(edit: EditText, saveAction: (String) -> Unit) {
        var localPending: Runnable? = null
        edit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                localPending?.let { saveHandler.removeCallbacks(it) }
                val runnable = Runnable {
                    saveAction(s?.toString()?.trim() ?: "")
                }
                localPending = runnable
                saveHandler.postDelayed(runnable, 500L)
                // Stash it on the view's tag so onDestroy can still find it
                edit.setTag(runnable)
            }
        })
    }

    /**
     * Helper to wire SeekBar progress changes to a label and update callback.
     */
    private fun bindSeekBar(
        seekBar: SeekBar,
        minValue: Int,
        formatText: (Int) -> String,
        labelView: TextView,
        onValueChanged: (Int) -> Unit
    ) {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress + minValue
                labelView.text = formatText(value)
                if (fromUser) {
                    onValueChanged(value)
                }
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    override fun onDestroy() {
        // Remove any callbacks still pending on either EditText
        (promptPhrasesEdit.tag as? Runnable)?.let { saveHandler.removeCallbacks(it) }
        (actionButtonsEdit.tag as? Runnable)?.let { saveHandler.removeCallbacks(it) }
        super.onDestroy()
    }
}
