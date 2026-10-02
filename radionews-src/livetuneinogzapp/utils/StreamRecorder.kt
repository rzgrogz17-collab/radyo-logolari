package com.globalradio.livetuneinogzapp.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import com.globalradio.livetuneinogzapp.model.RadioStation
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Çalan radyo akışını Müzik/RadioSphere klasörüne kaydeder.
 * Progressive (mp3/aac) doğrudan yazılır; HLS (.m3u8) segment segment birleştirilir.
 */
class StreamRecorder(private val appContext: Context) {

    interface Listener {
        fun onStarted()
        fun onSaved(displayName: String)
        fun onError(message: String)
    }

    private val running = AtomicBoolean(false)
    @Volatile private var activeCall: okhttp3.Call? = null
    @Volatile var startedAtElapsed: Long = 0L
        private set
    @Volatile private var bytesWritten: Long = 0L

    private val mainHandler = Handler(Looper.getMainLooper())

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .cache(null)
        .build()

    private val segmentClient = client.newBuilder()
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    fun isRunning(): Boolean = running.get()

    fun start(station: RadioStation, listener: Listener) {
        if (!running.compareAndSet(false, true)) return
        startedAtElapsed = SystemClock.elapsedRealtime()
        bytesWritten = 0L
        val url = station.getStreamUrl()
        if (url.isBlank()) {
            running.set(false)
            listener.onError("no_url")
            return
        }
        Thread({
            var out: OutputStream? = null
            var pendingUri: Uri? = null
            var fallbackFile: File? = null
            val displayName = buildFileName(station)
            try {
                val sink = openSink(displayName)
                out = sink.stream
                pendingUri = sink.uri
                fallbackFile = sink.file
                mainHandler.post { listener.onStarted() }
                if (url.contains(".m3u8", ignoreCase = true) || url.contains("m3u8?")) {
                    recordHls(url, out)
                } else {
                    recordProgressive(url, out)
                }
                out.flush()
                val saved = bytesWritten > 1024
                if (saved) finalizeSink(pendingUri, fallbackFile) else abortSink(pendingUri, fallbackFile)
                mainHandler.post {
                    if (saved) listener.onSaved(displayName)
                    else listener.onError("short")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Record failed: ${e.message}")
                val saved = bytesWritten > 1024
                try { out?.flush() } catch (_: Exception) {}
                if (saved) finalizeSink(pendingUri, fallbackFile) else abortSink(pendingUri, fallbackFile)
                mainHandler.post {
                    if (saved) listener.onSaved(displayName)
                    else listener.onError(e.message ?: "error")
                }
            } finally {
                try { out?.close() } catch (_: Exception) {}
                activeCall = null
                running.set(false)
            }
        }, "radio-recorder").start()
    }

    fun stop() {
        running.set(false)
        try { activeCall?.cancel() } catch (_: Exception) {}
    }

    private fun recordProgressive(url: String, out: OutputStream) {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Icy-MetaData", "0")
            .build()
        val call = client.newCall(req)
        activeCall = call
        call.execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
            val body = resp.body ?: throw IllegalStateException("empty")
            val source = body.byteStream()
            val buf = ByteArray(16 * 1024)
            while (running.get()) {
                val n = source.read(buf)
                if (n < 0) break
                if (n > 0) {
                    out.write(buf, 0, n)
                    bytesWritten += n
                }
            }
        }
    }

    private fun recordHls(masterUrl: String, out: OutputStream) {
        var playlistUrl = masterUrl
        val seen = LinkedHashSet<String>()
        var mediaUrl = resolveMediaPlaylist(playlistUrl)
        playlistUrl = mediaUrl
        while (running.get()) {
            val text = fetchText(playlistUrl) ?: break
            if (text.contains("#EXT-X-STREAM-INF")) {
                mediaUrl = pickFirstVariant(playlistUrl, text) ?: break
                playlistUrl = mediaUrl
                continue
            }
            val base = playlistUrl
            val lines = text.lines()
            var i = 0
            while (i < lines.size && running.get()) {
                val line = lines[i].trim()
                if (line.startsWith("#") || line.isEmpty()) {
                    i++
                    continue
                }
                val seg = absolutize(base, line)
                if (seen.add(seg)) {
                    appendSegment(seg, out)
                }
                i++
            }
            if (text.contains("#EXT-X-ENDLIST")) break
            try { Thread.sleep(2000) } catch (_: InterruptedException) { break }
        }
    }

    private fun resolveMediaPlaylist(url: String): String = url

    private fun pickFirstVariant(playlistUrl: String, text: String): String? {
        val lines = text.lines()
        for (i in lines.indices) {
            if (lines[i].contains("#EXT-X-STREAM-INF") && i + 1 < lines.size) {
                val next = lines[i + 1].trim()
                if (next.isNotEmpty() && !next.startsWith("#")) {
                    return absolutize(playlistUrl, next)
                }
            }
        }
        return null
    }

    private fun appendSegment(url: String, out: OutputStream) {
        val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        val call = segmentClient.newCall(req)
        activeCall = call
        call.execute().use { resp ->
            if (!resp.isSuccessful) return
            val body = resp.body ?: return
            val src = body.byteStream()
            val buf = ByteArray(16 * 1024)
            while (running.get()) {
                val n = src.read(buf)
                if (n < 0) break
                if (n > 0) {
                    out.write(buf, 0, n)
                    bytesWritten += n
                }
            }
        }
    }

    private fun fetchText(url: String): String? {
        val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        val call = segmentClient.newCall(req)
        activeCall = call
        return call.execute().use { resp ->
            if (!resp.isSuccessful) null else resp.body?.string()
        }
    }

    private fun absolutize(baseUrl: String, ref: String): String {
        if (ref.startsWith("http://") || ref.startsWith("https://")) return ref
        return try {
            URI(baseUrl).resolve(ref).toString()
        } catch (_: Exception) {
            val cut = baseUrl.substringBeforeLast('/') + "/" + ref.trimStart('/')
            cut
        }
    }

    private data class Sink(val stream: OutputStream, val uri: Uri?, val file: File?)

    private fun openSink(displayName: String): Sink {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Audio.Media.MIME_TYPE, mimeFor(displayName))
                put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/RadioSphere")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val uri = appContext.contentResolver.insert(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values
            ) ?: throw IllegalStateException("mediastore")
            val stream = appContext.contentResolver.openOutputStream(uri)
                ?: throw IllegalStateException("stream")
            return Sink(stream, uri, null)
        }
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            "RadioSphere"
        )
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, displayName)
        return Sink(FileOutputStream(file), null, file)
    }

    private fun finalizeSink(uri: Uri?, file: File?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uri != null) {
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.IS_PENDING, 0)
            }
            appContext.contentResolver.update(uri, values, null, null)
        }
        file // saved on disk already
    }

    private fun abortSink(uri: Uri?, file: File?) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uri != null) {
                appContext.contentResolver.delete(uri, null, null)
            }
        } catch (_: Exception) {}
        try { file?.delete() } catch (_: Exception) {}
    }

    private fun buildFileName(station: RadioStation): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val raw = station.name.ifBlank { "radio" }
        val safe = raw.replace(Regex("[^A-Za-z0-9._-]+"), "_").trim('_').take(40)
        val ext = if (station.isHlsStream()) "aac" else "mp3"
        return "${safe}_$stamp.$ext"
    }

    private fun mimeFor(name: String): String =
        if (name.endsWith(".aac", true)) "audio/aac" else "audio/mpeg"

    companion object {
        private const val TAG = "StreamRecorder"
        private const val USER_AGENT = "RadioSphere/1.0 (Android)"
    }
}
