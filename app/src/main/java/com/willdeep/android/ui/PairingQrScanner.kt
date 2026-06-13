package com.willdeep.android.ui

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.common.InputImage

@Composable
fun PairingQrScanner(
    modifier: Modifier = Modifier,
    onPayloadScanned: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
    }
    var handled by remember { mutableStateOf(false) }

    DisposableEffect(scanner) {
        onDispose { scanner.close() }
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            val controller = LifecycleCameraController(viewContext).apply {
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                setImageAnalysisAnalyzer(
                    ContextCompat.getMainExecutor(viewContext),
                    PairingQrAnalyzer(scanner) { rawValue ->
                        if (!handled) {
                            handled = true
                            onPayloadScanned(rawValue)
                        }
                    }
                )
                bindToLifecycle(lifecycleOwner)
            }
            PreviewView(viewContext).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                this.controller = controller
            }
        }
    )
}

private class PairingQrAnalyzer(
    private val scanner: BarcodeScanner,
    private val onQrCode: (String) -> Unit,
) : ImageAnalysis.Analyzer {
    override fun analyze(imageProxy: ImageProxy) {
        val image = imageProxy.image
        if (image == null) {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)
        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                val rawValue = barcodes.firstNotNullOfOrNull { barcode -> barcode.rawValue }
                if (!rawValue.isNullOrBlank()) {
                    onQrCode(rawValue)
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}
