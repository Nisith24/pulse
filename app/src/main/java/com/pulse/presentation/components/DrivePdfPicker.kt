package com.pulse.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulse.data.services.btr.BtrFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrivePdfPicker(
    pdfs: List<BtrFile>,
    onPdfSelected: (BtrFile) -> Unit,
    onDismissRequest: () -> Unit,
    isLoading: Boolean = false
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .padding(bottom = 16.dp)
        ) {
            Text(
                "Select PDF from Drive",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (pdfs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No PDFs found in this folder.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(pdfs) { pdf ->
                        ListItem(
                            headlineContent = { Text(pdf.name) },
                            leadingContent = { 
                                Icon(
                                    Icons.Default.PictureAsPdf, 
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                ) 
                            },
                            modifier = Modifier.clickable { onPdfSelected(pdf) }
                        )
                    }
                }
            }
        }
    }
}
