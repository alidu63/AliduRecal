package com.alidurecal

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var currentFile: File? = null
    private var recordingSeconds = 0
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var btnRecord: MaterialButton
    private lateinit var tvTimer: TextView
    private lateinit var tvStatus: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView

    private val recordings = mutableListOf<File>()
    private lateinit var adapter: RecordingsAdapter

    private val PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnRecord = findViewById(R.id.btnRecord)
        tvTimer = findViewById(R.id.tvTimer)
        tvStatus = findViewById(R.id.tvStatus)
        recyclerView = findViewById(R.id.recyclerView)
        tvEmpty = findViewById(R.id.tvEmpty)

        adapter = RecordingsAdapter(recordings) { file, action ->
            when (action) {
                "share" -> shareFile(file)
                "delete" -> deleteFile(file)
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnRecord.setOnClickListener {
            if (isRecording) stopRecording() else checkPermissionsAndRecord()
        }

        loadRecordings()
    }

    private fun checkPermissionsAndRecord() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isEmpty()) startRecording()
        else ActivityCompat.requestPermissions(this, notGranted.toTypedArray(), PERMISSION_CODE)
    }

    private fun startRecording() {
        val dir = getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: filesDir
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        currentFile = File(dir, "VoiceLog_$timestamp.m4a")

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        mediaRecorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(128000)
            setOutputFile(currentFile!!.absolutePath)
            prepare()
            start()
        }

        isRecording = true
        recordingSeconds = 0
        btnRecord.text = "⏹ STOP"
        btnRecord.setBackgroundColor(getColor(R.color.red))
        tvStatus.text = "● Registrazione in corso..."
        startTimer()
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        isRecording = false
        stopTimer()

        btnRecord.text = "🎙 REGISTRA"
        btnRecord.setBackgroundColor(getColor(R.color.purple_500))
        tvStatus.text = "Pronto"
        tvTimer.text = "00:00"

        currentFile?.let {
            if (it.exists() && it.length() > 0) {
                Toast.makeText(this, "Salvato: ${it.name}", Toast.LENGTH_SHORT).show()
                loadRecordings()
            }
        }
    }

    private val timerRunnable = object : Runnable {
        override fun run() {
            recordingSeconds++
            val min = recordingSeconds / 60
            val sec = recordingSeconds % 60
            tvTimer.text = String.format("%02d:%02d", min, sec)
            handler.postDelayed(this, 1000)
        }
    }

    private fun startTimer() = handler.postDelayed(timerRunnable, 1000)
    private fun stopTimer() = handler.removeCallbacks(timerRunnable)

    private fun loadRecordings() {
        val dir = getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: filesDir
        recordings.clear()
        dir.listFiles { f -> f.extension == "m4a" }
            ?.sortedByDescending { it.lastModified() }
            ?.let { recordings.addAll(it) }

        adapter.notifyDataSetChanged()
        tvEmpty.visibility = if (recordings.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (recordings.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun shareFile(file: File) {
        val uri: Uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Condividi registrazione"))
    }

    private fun deleteFile(file: File) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Elimina registrazione")
            .setMessage("Eliminare ${file.name}?")
            .setPositiveButton("Elimina") { _, _ ->
                file.delete()
                loadRecordings()
                Toast.makeText(this, "Eliminato", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<String>, results: IntArray) {
        super.onRequestPermissionsResult(code, perms, results)
        if (code == PERMISSION_CODE && results.all { it == PackageManager.PERMISSION_GRANTED }) {
            startRecording()
        } else {
            Toast.makeText(this, "Permesso microfono necessario!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRecording) stopRecording()
    }
}

// ---- Adapter RecyclerView ----
class RecordingsAdapter(
    private val items: List<File>,
    private val onAction: (File, String) -> Unit
) : RecyclerView.Adapter<RecordingsAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvSize: TextView = view.findViewById(R.id.tvSize)
        val btnShare: ImageButton = view.findViewById(R.id.btnShare)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_recording, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val file = items[pos]
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val sizeKB = file.length() / 1024

        h.tvName.text = file.nameWithoutExtension
        h.tvDate.text = sdf.format(Date(file.lastModified()))
        h.tvSize.text = if (sizeKB > 1024) "${sizeKB / 1024} MB" else "$sizeKB KB"
        h.btnShare.setOnClickListener { onAction(file, "share") }
        h.btnDelete.setOnClickListener { onAction(file, "delete") }
    }

    override fun getItemCount() = items.size
}
