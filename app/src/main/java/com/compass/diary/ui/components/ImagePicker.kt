package com.compass.diary.ui.components

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.compass.diary.ui.theme.CompassColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePickerSheet(
    onInsert: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { selectedUri = it } }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Insert Image", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp))

            if (selectedUri == null) {
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PhotoLibrary, "Gallery", modifier = Modifier.size(28.dp))
                        Spacer(Modifier.height(4.dp))
                        Text("Choose from Gallery")
                    }
                }
            } else {
                AsyncImage(
                    model = selectedUri,
                    contentDescription = "Selected image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(220.dp)
                )
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { selectedUri = null }, modifier = Modifier.weight(1f)) {
                        Text("Choose again")
                    }
                    Button(
                        onClick = { selectedUri?.let { onInsert(it) } },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = CompassColors.Blue600)
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("Insert")
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
