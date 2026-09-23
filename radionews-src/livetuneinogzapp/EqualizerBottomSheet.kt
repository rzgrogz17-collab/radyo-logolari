package com.globalradio.livetuneinogzapp

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.globalradio.livetuneinogzapp.databinding.FragmentEqualizerBinding
import com.globalradio.livetuneinogzapp.service.RadioPlayerService
import androidx.media3.common.util.UnstableApi
import com.globalradio.livetuneinogzapp.utils.EqBandView

@UnstableApi
class EqualizerBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentEqualizerBinding? = null
    private val binding get() = _binding!!

    private val radioService: RadioPlayerService?
        get() = (activity as? MainActivity)?.getRadioService()
            ?: (activity as? CountryStationsActivity)?.getRadioService()

    private val presetNameRes = intArrayOf(
        R.string.eq_flat, R.string.eq_rock, R.string.eq_pop, R.string.eq_hiphop,
        R.string.eq_jazz, R.string.eq_classical, R.string.eq_bass, R.string.eq_vocal
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet =
                dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                it.setBackgroundColor(Color.TRANSPARENT)
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEqualizerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionId = radioService?.getAudioSessionId() ?: 0
        if (sessionId != 0) {
            EqualizerManager.init(requireContext(), sessionId)
        }

        binding.btnCloseEqualizer.setOnClickListener { dismiss() }

        if (!EqualizerManager.isAvailable && sessionId == 0) {
            binding.tvEqUnavailable.visibility = View.VISIBLE
        }

        setupPower()
        setupPresets()
        setupBandSliders()
        setupSliders()
        binding.btnEqReset.setOnClickListener {
            if (!EqualizerManager.isEnabled) return@setOnClickListener
            EqualizerManager.resetFlat(requireContext())
            refreshBandSliders()
            refreshSliders()
            refreshPresetSelection()
        }
        binding.btnEqSave.setOnClickListener { promptSavePreset() }
        updateControlsEnabled(EqualizerManager.isEnabled)
    }

    private fun setupPower() {
        renderPower(EqualizerManager.isEnabled)
        binding.btnEqPower.setOnClickListener {
            if (!EqualizerManager.isAvailable && (radioService?.getAudioSessionId() ?: 0) == 0) {
                Toast.makeText(requireContext(), R.string.eq_unavailable, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val next = !EqualizerManager.isEnabled
            EqualizerManager.setEnabled(requireContext(), next)
            renderPower(next)
            updateControlsEnabled(next)
        }
    }

    private fun renderPower(on: Boolean) {
        binding.btnEqPower.text = getString(if (on) R.string.eq_on else R.string.eq_off)
        binding.tvEqStatus.text = getString(if (on) R.string.eq_status_on else R.string.eq_status_off)
        binding.eqSpectrum.active = on && radioService?.isPlaying() == true
    }

    private fun updateControlsEnabled(enabled: Boolean) {
        val alpha = if (enabled) 1f else 0.4f
        binding.bandSlidersContainer.alpha = alpha
        for (i in 0 until binding.bandSlidersContainer.childCount) {
            val col = binding.bandSlidersContainer.getChildAt(i) as? LinearLayout ?: continue
            (col.getChildAt(1) as? EqBandView)?.interactive = enabled
        }
        binding.sliderBassBoost.isEnabled = enabled
        binding.sliderTreble.isEnabled = enabled
        binding.sliderVirtualizer.isEnabled = enabled
        binding.sliderBassBoost.alpha = alpha
        binding.sliderTreble.alpha = alpha
        binding.sliderVirtualizer.alpha = alpha
        binding.chipGroupPresets.alpha = alpha
        for (i in 0 until binding.chipGroupPresets.childCount) {
            val chip = binding.chipGroupPresets.getChildAt(i)
            chip.isEnabled = enabled
            chip.isClickable = enabled
        }
        binding.btnEqReset.isEnabled = enabled
        binding.btnEqSave.isEnabled = enabled
        binding.eqSpectrum.active = enabled && radioService?.isPlaying() == true
    }

    private fun setupPresets() {
        binding.chipGroupPresets.removeAllViews()
        EqualizerManager.presets.forEachIndexed { i, preset ->
            val label = presetNameRes.getOrNull(i)?.let { getString(it) } ?: preset.name
            addChip(label, selected = i == EqualizerManager.currentPresetIndex) {
                EqualizerManager.applyPreset(requireContext(), i)
                refreshBandSliders()
                refreshSliders()
                refreshPresetSelection()
            }
        }
        val custom = addChip(getString(R.string.eq_custom), selected = EqualizerManager.currentPresetIndex < 0) {}
        custom.isCheckable = true
        custom.isClickable = false
        EqualizerManager.userPresets.forEachIndexed { i, preset ->
            val chip = addChip(
                preset.name,
                selected = EqualizerManager.selectedUserPresetIndex() == i
            ) {
                EqualizerManager.applyUserPreset(requireContext(), i)
                refreshBandSliders()
                refreshSliders()
                refreshPresetSelection()
            }
            chip.setOnLongClickListener {
                EqualizerManager.deleteUserPreset(requireContext(), i)
                setupPresets()
                true
            }
        }
    }

    private fun addChip(text: String, selected: Boolean, onClick: () -> Unit): Chip {
        val chip = Chip(requireContext()).apply {
            this.text = text
            isCheckable = true
            isChecked = selected
            isCheckedIconVisible = false
            setChipBackgroundColorResource(R.color.timer_chip_idle)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.on_dark_sheet))
            chipStrokeColor = android.content.res.ColorStateList.valueOf(
                if (selected) Color.parseColor("#E94560")
                else ContextCompat.getColor(requireContext(), R.color.on_dark_sheet_muted)
            )
            chipStrokeWidth = if (selected) 2.5f else 1.5f
            setOnClickListener {
                if (!EqualizerManager.isEnabled) return@setOnClickListener
                onClick()
            }
        }
        binding.chipGroupPresets.addView(chip)
        return chip
    }

    private fun refreshPresetSelection() {
        val customIndex = EqualizerManager.presets.size
        for (idx in 0 until binding.chipGroupPresets.childCount) {
            val chip = binding.chipGroupPresets.getChildAt(idx) as? Chip ?: continue
            val selected = when {
                idx < EqualizerManager.presets.size -> idx == EqualizerManager.currentPresetIndex
                idx == customIndex -> EqualizerManager.currentPresetIndex < 0
                else -> EqualizerManager.selectedUserPresetIndex() == idx - customIndex - 1
            }
            chip.isChecked = selected
            chip.chipStrokeColor = android.content.res.ColorStateList.valueOf(
                if (selected) Color.parseColor("#E94560")
                else ContextCompat.getColor(requireContext(), R.color.on_dark_sheet_muted)
            )
            chip.chipStrokeWidth = if (selected) 2.5f else 1.5f
        }
    }

    private fun setupBandSliders() {
        val container = binding.bandSlidersContainer
        container.removeAllViews()
        val bands = EqualizerManager.getNumberOfBands()
        val (minLevel, maxLevel) = EqualizerManager.getBandLevelRange()
        val freqLabels = arrayOf("60Hz", "250Hz", "1kHz", "4kHz", "14kHz")
        val density = resources.displayMetrics.density

        for (i in 0 until bands) {
            val col = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
            }
            val db = TextView(requireContext()).apply {
                tag = "db_$i"
                text = "${EqualizerManager.getBandLevel(i) / 100}dB"
                textSize = 11f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.on_dark_sheet_muted))
                gravity = Gravity.CENTER
            }
            val band = EqBandView(requireContext()).apply {
                tag = "band_$i"
                this.minLevel = minLevel
                this.maxLevel = maxLevel
                level = EqualizerManager.getBandLevel(i)
                onLevelChanged = { value, fromUser ->
                    if (fromUser && EqualizerManager.isEnabled) {
                        db.text = "${value / 100}dB"
                        EqualizerManager.setBandLevel(requireContext(), i, value)
                        refreshPresetSelection()
                        if (i == bands - 1) {
                            binding.sliderTreble.value = (value / 100).toFloat()
                                .coerceIn(binding.sliderTreble.valueFrom, binding.sliderTreble.valueTo)
                        }
                    }
                }
            }
            val freq = TextView(requireContext()).apply {
                text = if (i < freqLabels.size) freqLabels[i]
                else "${EqualizerManager.getBandFreq(i)}Hz"
                textSize = 10f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.on_dark_sheet_muted))
                gravity = Gravity.CENTER
            }
            col.addView(db, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            col.addView(band, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply {
                topMargin = (4 * density).toInt()
                bottomMargin = (4 * density).toInt()
            })
            col.addView(freq, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            container.addView(col, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
        }
    }

    private fun refreshBandSliders() {
        val container = binding.bandSlidersContainer
        val bands = EqualizerManager.getNumberOfBands()
        for (i in 0 until bands.coerceAtMost(container.childCount)) {
            val col = container.getChildAt(i) as? LinearLayout ?: continue
            val band = col.findViewWithTag<EqBandView>("band_$i") ?: continue
            val db = col.findViewWithTag<TextView>("db_$i")
            val level = EqualizerManager.getBandLevel(i)
            band.level = level
            db?.text = "${level / 100}dB"
        }
    }

    private fun setupSliders() {
        refreshSliders()
        binding.sliderBassBoost.addOnChangeListener { _, value, fromUser ->
            if (fromUser && EqualizerManager.isEnabled) {
                EqualizerManager.setBassBoostStrength(requireContext(), value.toInt())
            }
        }
        binding.sliderTreble.addOnChangeListener { _, value, fromUser ->
            if (fromUser && EqualizerManager.isEnabled) {
                EqualizerManager.setTreble(requireContext(), (value * 100).toInt())
                refreshBandSliders()
                refreshPresetSelection()
            }
        }
        binding.sliderVirtualizer.addOnChangeListener { _, value, fromUser ->
            if (fromUser && EqualizerManager.isEnabled) {
                EqualizerManager.setVirtualizerStrength(requireContext(), value.toInt())
            }
        }
    }

    private fun refreshSliders() {
        binding.sliderBassBoost.value = EqualizerManager.bassStrength.toFloat()
        val trebleDb = (EqualizerManager.trebleLevel() / 100).toFloat()
        binding.sliderTreble.value = trebleDb.coerceIn(-15f, 15f)
        binding.sliderVirtualizer.value = EqualizerManager.virtualizerStrength.toFloat()
    }

    private fun promptSavePreset() {
        if (!EqualizerManager.isEnabled) return
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.eq_preset_name_hint)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            setHintTextColor(ContextCompat.getColor(requireContext(), R.color.text_hint))
            setPadding(40, 24, 40, 24)
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.eq_save)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                EqualizerManager.saveUserPreset(requireContext(), input.text.toString())
                setupPresets()
                Toast.makeText(requireContext(), R.string.eq_preset_saved, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        binding.eqSpectrum.active = false
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = EqualizerBottomSheet()
    }
}
