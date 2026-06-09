package com.compass.diary.ui.components

import android.Manifest
import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.compass.diary.ui.theme.CompassColors
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VoiceRecorder(
    dateKey: String,
    onSaved: (filePath: String, durationMs: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isRecording  by remember { mutableStateOf(false) }
    var isPlaying    by remember { mutableStateOf(false) }
    var recordedPath by remember { mutableStateOf<String?>(null) }
    var elapsedMs    by remember { mutableLongStateOf(0L) }
    var durationMs   by remember { mutableLongStateOf(0L) }
    val recorderState = remember { RecorderState(context) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* permission result handled silently */ }

    LaunchedEffect(Unit) { permLauncher.launch(Manifest.permission.RECORD_AUDIO) }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            elapsedMs = 0L
            while (isRecording) { delay(100); elapsedMs += 100 }
        }
    }

    DisposableEffect(Unit) { onDispose { recorderState.release() } }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "pulseScale"
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Voice Note", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close") }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                formatTime(elapsedMs),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Light,
                color = if (isRecording) CompassColors.Error else MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(24.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(88.dp).scale(if (isRecording) pulse else 1f)
            ) {
                if (isRecording) {
                    Box(Modifier.size(88.dp).background(CompassColors.Error.copy(0.15f), CircleShape))
                }
                FilledIconButton(
                    onClick = {
                        if (!isRecording) {
                            val p = getOutputPath(context, dateKey)
                            recorderState.start(p)
                            recordedPath = p
                            isRecording = true
                        } else {
                            durationMs = elapsedMs
                            recorderState.stop()
                            isRecording = false
                        }
                    },
                    modifier = Modifier.size(72.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isRecording) CompassColors.Error else CompassColors.Blue600
                    )
                ) {
                    Icon(
                        if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        null, tint = Color.White, modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                if (isRecording) "Recording… tap to stop"
                else if (recordedPath != null) "Tap play to preview" else "Tap to record",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AnimatedVisibility(visible = recordedPath != null && !isRecording) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedIconButton(onClick = {
                            if (!isPlaying) {
                                recorderState.play(recordedPath!!) { isPlaying = false }
                                isPlaying = true
                            } else {
                                recorderState.stopPlayback()
                                isPlaying = false
                            }
                        }) {
                            Icon(if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, null)
                        }
                        Text(formatTime(durationMs), style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = {
                            recordedPath?.let { File(it).delete() }
                            recordedPath = null; elapsedMs = 0L
                        }) { Text("Re-record") }
                        Button(
                            onClick = { recordedPath?.let { onSaved(it, durationMs) } },
                            colors = ButtonDefaults.buttonColors(containerColor = CompassColors.Blue600)
                        ) { Text("Insert") }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun formatTime(ms: Long): String { val s = ms / 1000; return "%d:%02d".format(s / 60, s % 60) }
private fun getOutputPath(context: Context, dateKey: String): String {
    val dir = context.getExternalFilesDir("voice_notes") ?: context.filesDir
    return "${dir.absolutePath}/${dateKey}_${SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date())}.m4a"
}

class RecorderState(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer?     = null
    fun start(outputPath: String) {
        recorder?.release()
        recorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context)
        else @Suppress("DEPRECATION") MediaRecorder()).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputPath)
            prepare(); start()
        }
    }
    fun stop()  { try { recorder?.stop() } catch (_: Exception) {}; recorder?.release(); recorder = null }
    fun play(path: String, onComplete: () -> Unit) {
        player?.release()
        player = MediaPlayer().apply { setDataSource(path); prepare(); start(); setOnCompletionListener { onComplete() } }
    }
    fun stopPlayback() { player?.stop(); player?.release(); player = null }
    fun release()      { stop(); stopPlayback() }
}
