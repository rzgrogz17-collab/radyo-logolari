package com.globalradio.livetuneinogzapp

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.globalradio.livetuneinogzapp.databinding.DialogSleepTimerBinding
import com.globalradio.livetuneinogzapp.utils.AppSettings
import com.globalradio.livetuneinogzapp.utils.SleepTimerManager

class SleepTimerDialog : BottomSheetDialogFragment() {

    private var _binding: DialogSleepTimerBinding? = null
    private val binding get() = _binding!!
    private lateinit var settings: AppSettings
    private var customMinutes = 15
    private val presets = listOf(5, 10, 15, 30, 45, 60, 90, 120)

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
        _binding = DialogSleepTimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settings = AppSettings(requireContext())
        customMinutes = settings.lastTimerMinutes
        setupChips()
        setupControls()
        observeTimer()
        updateUI()
    }

    private fun setupChips() {
        binding.chipGroupDurations.removeAllViews()
        presets.forEach { min ->
            val chip = Chip(
                android.view.ContextThemeWrapper(
                    requireContext(),
                    R.style.ThemeOverlay_RadyoApp_TimerChips
                )
            ).apply {
                text = when (min) {
                    5 -> getString(R.string.timer_5min)
                    10 -> getString(R.string.timer_10min)
                    15 -> getString(R.string.timer_15min)
                    30 -> getString(R.string.timer_30min)
                    45 -> getString(R.string.timer_45min)
                    60 -> getString(R.string.timer_60min)
                    90 -> getString(R.string.timer_90min)
                    120 -> getString(R.string.timer_120min)
                    else -> getString(R.string.timer_n_min, min)
                }
                isCheckable = true
                isChecked = min == customMinutes
                setEnsureMinTouchTargetSize(false)
                chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    Color.parseColor("#2C2C34")
                )
                setTextColor(Color.WHITE)
                chipStrokeWidth = 0f
                setOnClickListener {
                    customMinutes = min
                    refreshCustomLabel()
                    highlightChips()
                    startTimer(min * 60 * 1000L)
                }
            }
            binding.chipGroupDurations.addView(chip)
        }
        highlightChips()
        refreshCustomLabel()
    }

    private fun highlightChips() {
        for (i in 0 until binding.chipGroupDurations.childCount) {
            val chip = binding.chipGroupDurations.getChildAt(i) as? Chip ?: continue
            val min = presets.getOrNull(i) ?: continue
            val on = min == customMinutes
            chip.isChecked = on
            chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                if (on) Color.parseColor("#E91E63") else Color.parseColor("#2C2C34")
            )
            chip.setTextColor(Color.WHITE)
            chip.chipStrokeWidth = 0f
        }
    }

    private fun setupControls() {
        binding.switchFade.isChecked = settings.timerFadeOut
        binding.switchMute.isChecked = settings.timerMuteInsteadOfStop
        binding.switchFade.setOnCheckedChangeListener { _, checked ->
            settings.timerFadeOut = checked
        }
        binding.switchMute.setOnCheckedChangeListener { _, checked ->
            settings.timerMuteInsteadOfStop = checked
        }
        binding.btnCustomMinus.setOnClickListener {
            customMinutes = (customMinutes - 1).coerceAtLeast(1)
            refreshCustomLabel()
            highlightChips()
        }
        binding.btnCustomPlus.setOnClickListener {
            customMinutes = (customMinutes + 1).coerceAtMost(180)
            refreshCustomLabel()
            highlightChips()
        }
        binding.btnStartCustom.setOnClickListener {
            startTimer(customMinutes * 60 * 1000L)
        }
        binding.btnPlus5.setOnClickListener { SleepTimerManager.extend(5 * 60 * 1000L) }
        binding.btnPlus15.setOnClickListener { SleepTimerManager.extend(15 * 60 * 1000L) }
        binding.btnMinus5.setOnClickListener { SleepTimerManager.shorten(5 * 60 * 1000L) }
        binding.btnCancelTimer.setOnClickListener {
            SleepTimerManager.cancel()
            Toast.makeText(requireContext(), R.string.timer_cancelled, Toast.LENGTH_SHORT).show()
            updateUI()
        }
    }

    private fun startTimer(durationMs: Long) {
        SleepTimerManager.start(requireContext(), durationMs)
        Toast.makeText(
            requireContext(),
            getString(R.string.timer_set_msg, SleepTimerManager.formatRemaining(durationMs)),
            Toast.LENGTH_SHORT
        ).show()
        updateUI()
    }

    private fun observeTimer() {
        SleepTimerManager.remainingMs.observe(viewLifecycleOwner) { updateUI() }
        SleepTimerManager.isActive.observe(viewLifecycleOwner) { updateUI() }
    }

    private fun updateUI() {
        if (_binding == null) return
        val ms = SleepTimerManager.remainingMs.value ?: 0L
        val active = SleepTimerManager.isActive.value == true
        binding.btnCancelTimer.visibility = if (active) View.VISIBLE else View.GONE
        binding.extendRow.visibility = if (active) View.VISIBLE else View.GONE
        binding.timerRing.progress = if (active) SleepTimerManager.progress() else 0f
        binding.timerRing.caption = ""
        if (active && ms > 0) {
            binding.timerRing.label = SleepTimerManager.formatRemaining(ms)
            binding.tvTimerBanner.text = getString(R.string.timer_will_stop)
        } else {
            binding.timerRing.label = "%d:00".format(customMinutes)
            binding.tvTimerBanner.text = getString(R.string.timer_lock_hint)
        }
        refreshCustomLabel()
    }

    private fun refreshCustomLabel() {
        binding.tvCustomMinutes.text = getString(R.string.timer_n_min, customMinutes)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = SleepTimerDialog()
    }
}
