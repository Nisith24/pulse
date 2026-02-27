package com.pulse.presentation.lecture

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.util.FitPolicy
import java.io.File

@Composable
fun PdfViewer(
    pdfPath: String,
    initialPage: Int,
    isPdfDownloaded: Boolean,
    onPageChanged: (Int) -> Unit,
    onAddLocalPdf: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isContentUri = pdfPath.startsWith("content://")
    val fileExists = if (isContentUri) true else File(pdfPath).exists()
    
    // Using key(pdfPath) forces Compose to completely demount and recreate the internal 
    // AndroidView whenever the path changes, ensuring no stale data from previous videos remains.
    key(pdfPath) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .clipToBounds()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (pdfPath.isEmpty() || (!isContentUri && !fileExists)) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "PDF",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (isPdfDownloaded) "PDF file not found" else "No PDF attached",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onAddLocalPdf,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add Local PDF")
                    }
                }
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val frameLayout = FrameLayout(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                        
                        val pdfView = PDFView(ctx, null).apply {
                            id = android.view.View.generateViewId()
                            layoutParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                            )
                        }
                        
                        frameLayout.addView(pdfView)
                        loadPdfIntoView(pdfView, pdfPath, initialPage, isContentUri, onPageChanged)
                        frameLayout
                    },
                    update = { _ -> 
                        // Handled by key() recreate
                    },
                    onRelease = { frameLayout ->
                        // Explicitly clear resources when the Composable is removed
                        val pdfView = frameLayout.getChildAt(0) as? PDFView
                        pdfView?.recycle()
                    }
                )
            }
        }
    }
}

private fun loadPdfIntoView(
    pdfView: PDFView,
    path: String,
    page: Int,
    isContentUri: Boolean,
    onPageChanged: (Int) -> Unit
) {
    val config = if (isContentUri) {
        pdfView.fromUri(Uri.parse(path))
    } else {
        pdfView.fromFile(File(path))
    }
    
    config
        .defaultPage(page)
        .enableSwipe(true)
        .swipeHorizontal(false)
        .enableDoubletap(true)
        .enableAntialiasing(true)
        .nightMode(true)
        .spacing(4)
        .onPageChange { p, _ -> onPageChanged(p) }
        .onError { t -> t.printStackTrace() }
        .pageFitPolicy(FitPolicy.WIDTH)
        .fitEachPage(true)
        .load()
}
