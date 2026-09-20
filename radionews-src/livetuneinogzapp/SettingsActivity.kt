package com.globalradio.livetuneinogzapp

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.util.UnstableApi
import com.bumptech.glide.Glide
import com.globalradio.livetuneinogzapp.databinding.ActivitySettingsBinding
import com.globalradio.livetuneinogzapp.service.RadioPlayerService
import com.globalradio.livetuneinogzapp.utils.AppSettings
import com.globalradio.livetuneinogzapp.utils.FavoritesManager
import com.globalradio.livetuneinogzapp.utils.ListenHistoryManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

private data class SettingsBackup(
    val favorites: List<String> = emptyList(),
    val history: Map<String, Int> = emptyMap()
)

@UnstableApi
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var settings: AppSettings
    private val gson = Gson()

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri ?: return@registerForActivityResult
        runCatching {
            val backup = SettingsBackup(
                favorites = FavoritesManager(this).getFavoriteIds().toList(),
                history = ListenHistoryManager(this).getAllCounts()
            )
            runCatching {
                contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            contentResolver.openOutputStream(uri)?.bufferedWriter().use {
                it?.write(gson.toJson(backup))
            }
            Toast.makeText(this, R.string.settings_exported, Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, R.string.settings_import_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@registerForActivityResult
        runCatching {
            runCatching {
                contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val json = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("empty")
            val type = object : TypeToken<SettingsBackup>() {}.type
            val backup = gson.fromJson<SettingsBackup>(json, type)
            FavoritesManager(this).addAll(backup.favorites)
            ListenHistoryManager(this).importCounts(backup.history)
            Toast.makeText(this, R.string.settings_imported, Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, R.string.settings_import_failed, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = AppSettings(this)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarSettings)
        supportActionBar?.apply {
            title = getString(R.string.settings)
            setDisplayHomeAsUpEnabled(true)
        }

        bindValues()
        setupListeners()
    }

    private fun bindValues() {
        try {
            binding.tvVersion.text = "v${packageManager.getPackageInfo(packageName, 0).versionName}"
        } catch (_: Exception) {
            binding.tvVersion.text = "v1.0"
        }
        binding.switchDarkTheme.isChecked = settings.darkTheme
        binding.switchResumeLast.isChecked = settings.resumeLastStation
        binding.switchLockControls.isChecked = settings.lockScreenControls
        binding.switchDefaultVolume.isChecked = settings.applyDefaultVolume
        binding.sliderDefaultVolume.value = settings.defaultVolumePercent.toFloat()
        binding.tvVolumeValue.text = getString(R.string.settings_volume_value, settings.defaultVolumePercent)
        binding.sliderDefaultVolume.isEnabled = settings.applyDefaultVolume
        binding.switchDataSaver.isChecked = settings.dataSaver
    }

    private fun setupListeners() {
        binding.switchDarkTheme.setOnCheckedChangeListener { _, checked ->
            if (settings.darkTheme == checked) return@setOnCheckedChangeListener
            settings.darkTheme = checked
            settings.applyNightMode()
            recreate()
        }
        binding.switchResumeLast.setOnCheckedChangeListener { _, checked ->
            settings.resumeLastStation = checked
        }
        binding.switchLockControls.setOnCheckedChangeListener { _, checked ->
            settings.lockScreenControls = checked
            runCatching {
                startService(
                    Intent(this, RadioPlayerService::class.java)
                        .setAction(RadioPlayerService.ACTION_REFRESH_NOTIF)
                )
            }
        }
        binding.switchDefaultVolume.setOnCheckedChangeListener { _, checked ->
            settings.applyDefaultVolume = checked
            binding.sliderDefaultVolume.isEnabled = checked
        }
        binding.sliderDefaultVolume.addOnChangeListener { _, value, fromUser ->
            if (!fromUser) return@addOnChangeListener
            val pct = value.toInt()
            settings.defaultVolumePercent = pct
            binding.tvVolumeValue.text = getString(R.string.settings_volume_value, pct)
        }
        binding.switchDataSaver.setOnCheckedChangeListener { _, checked ->
            settings.dataSaver = checked
        }

        binding.btnClearHistory.setOnClickListener {
            confirm(R.string.settings_confirm_history) {
                ListenHistoryManager(this).clearAll()
                Toast.makeText(this, R.string.settings_history_cleared, Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnClearCache.setOnClickListener {
            confirm(R.string.settings_confirm_cache) {
                Glide.get(this).clearMemory()
                Thread {
                    Glide.get(this).clearDiskCache()
                    runOnUiThread {
                        Toast.makeText(this, R.string.settings_cache_cleared, Toast.LENGTH_SHORT).show()
                    }
                }.start()
            }
        }
        binding.btnExportBackup.setOnClickListener {
            exportLauncher.launch("RadioSphere-backup.json")
        }
        binding.btnImportBackup.setOnClickListener {
            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
        }
        binding.btnOpenRecordings.setOnClickListener { openRecordings() }
        binding.btnClearRecordings.setOnClickListener {
            confirm(R.string.settings_confirm_recordings) { clearRecordings() }
        }
        binding.btnContactEmail.setOnClickListener {
            try {
                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:radiosphere@support.com")
                    putExtra(Intent.EXTRA_SUBJECT, getString(R.string.settings_contact_subject))
                }
                startActivity(Intent.createChooser(emailIntent, getString(R.string.settings_contact_email)))
            } catch (_: Exception) {
                Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnRateApp.setOnClickListener {
            val appId = packageName
            val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appId"))
            try {
                startActivity(market)
            } catch (_: Exception) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appId")))
            }
        }
        binding.btnPrivacyPolicy.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.settings_privacy_policy)
                .setMessage(R.string.settings_privacy_body)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
        binding.btnLicenses.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.settings_licenses)
                .setMessage(R.string.settings_licenses_body)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
        binding.btnOtherApps.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pub:RadioSphere&hl=tr")))
            } catch (_: Exception) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=RadioSphere")))
            }
        }
    }

    private fun confirm(message: Int, onYes: () -> Unit) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings_confirm_title)
            .setMessage(message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ -> onYes() }
            .show()
    }

    private fun openRecordings() {
        Toast.makeText(this, R.string.settings_recordings_path, Toast.LENGTH_LONG).show()
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "vnd.android.cursor.dir/audio")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { startActivity(intent) }
    }

    private fun clearRecordings() {
        var deleted = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            deleted = contentResolver.delete(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?",
                arrayOf("%RadioSphere%")
            )
        } else {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "RadioSphere")
            deleted = dir.listFiles()?.count { it.delete() } ?: 0
        }
        Toast.makeText(this, getString(R.string.settings_recordings_cleared) + " ($deleted)", Toast.LENGTH_SHORT).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish(); return true
        }
        return super.onOptionsItemSelected(item)
    }
}
