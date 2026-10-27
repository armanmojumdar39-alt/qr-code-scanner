package com.qrpro.scanner.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.qrpro.scanner.R
import com.qrpro.scanner.data.formatName
import com.qrpro.scanner.util.Actions
import java.util.concurrent.Executors

private fun hasCameraPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

@Composable
fun ScannerScreen(
    paused: Boolean,
    bottomPadding: Dp,
    onCode: (raw: String, format: String) -> Unit,
) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasCameraPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted = it }

    // Re-check when the user comes back from system settings.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { granted = hasCameraPermission(context) }
    LaunchedEffect(Unit) {
        if (!granted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (!granted) {
        PermissionPrompt(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = bottomPadding),
            onAllow = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onSettings = { Actions.openAppSettings(context) },
        )
        return
    }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torch by remember { mutableStateOf(false) }
    val hasFlash = camera?.cameraInfo?.hasFlashUnit() == true

    LaunchedEffect(camera, torch) {
        camera?.cameraControl?.enableTorch(torch)
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scanImage(context, uri) { raw, format ->
                if (raw == null) {
                    Toast.makeText(context, R.string.msg_no_code, Toast.LENGTH_LONG).show()
                } else {
                    onCode(raw, format)
                }
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .padding(bottom = bottomPadding)
            .background(Color.Black)
    ) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        CameraBinding(
            previewView = previewView,
            paused = paused,
            onCamera = { camera = it },
            onCode = onCode,
        )

        ScanOverlay(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(camera) {
                    detectTransformGestures { _, _, zoom, _ ->
                        val cam = camera ?: return@detectTransformGestures
                        val zs = cam.cameraInfo.zoomState.value ?: return@detectTransformGestures
                        val ratio = (zs.zoomRatio * zoom).coerceIn(zs.minZoomRatio, zs.maxZoomRatio)
                        cam.cameraControl.setZoomRatio(ratio)
                    }
                }
                .pointerInput(camera) {
                    detectTapGestures { pos ->
                        val cam = camera ?: return@detectTapGestures
                        val point = previewView.meteringPointFactory.createPoint(pos.x, pos.y)
                        cam.cameraControl.startFocusAndMetering(
                            FocusMeteringAction.Builder(point).build()
                        )
                    }
                }
        )

        // Top bar: brand + actions
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandChip()
            Spacer(Modifier.weight(1f))
            RoundIconButton(
                icon = Icons.Outlined.PhotoLibrary,
                description = stringResource(R.string.cd_gallery),
                onClick = {
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            )
            if (hasFlash) {
                Spacer(Modifier.width(10.dp))
                RoundIconButton(
                    icon = if (torch) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                    description = stringResource(R.string.cd_torch),
                    active = torch,
                    onClick = { torch = !torch },
                )
            }
        }

        // Bottom hint
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Text(
                text = stringResource(R.string.scan_hint),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Binds CameraX preview + barcode analysis to the lifecycle. */
@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraBinding(
    previewView: PreviewView,
    paused: Boolean,
    onCamera: (Camera) -> Unit,
    onCode: (String, String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentPaused by rememberUpdatedState(paused)
    val currentOnCode by rememberUpdatedState(onCode)
    val currentOnCamera by rememberUpdatedState(onCamera)

    DisposableEffect(lifecycleOwner) {
        val executor = Executors.newSingleThreadExecutor()
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build()
        )
        val providerFuture = ProcessCameraProvider.getInstance(context)

        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(executor) { proxy ->
                val media = proxy.image
                if (media == null || currentPaused) {
                    proxy.close()
                    return@setAnalyzer
                }
                val image = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                scanner.process(image)
                    .addOnSuccessListener { codes ->
                        val hit = codes.firstOrNull { !it.rawValue.isNullOrEmpty() }
                        if (hit != null && !currentPaused) {
                            currentOnCode(hit.rawValue.orEmpty(), formatName(hit.format))
                        }
                    }
                    .addOnCompleteListener { proxy.close() }
            }

            try {
                provider.unbindAll()
                val camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis,
                )
                currentOnCamera(camera)
            } catch (e: Exception) {
                // Camera unavailable (in use by another app, etc.) - leave preview blank.
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            runCatching { providerFuture.get().unbindAll() }
            scanner.close()
            executor.shutdown()
        }
    }
}

/** Decodes a code from an image picked from the gallery. */
private fun scanImage(context: Context, uri: Uri, onResult: (String?, String) -> Unit) {
    val image = try {
        InputImage.fromFilePath(context, uri)
    } catch (e: Exception) {
        onResult(null, "")
        return
    }
    val client = BarcodeScanning.getClient()
    client.process(image)
        .addOnSuccessListener { codes ->
            val hit = codes.firstOrNull { !it.rawValue.isNullOrEmpty() }
            onResult(hit?.rawValue, if (hit != null) formatName(hit.format) else "")
        }
        .addOnFailureListener { onResult(null, "") }
        .addOnCompleteListener { client.close() }
}

@Composable
private fun ScanOverlay(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "scan")
    val progress by transition.animateFloat(
        initialValue = 0.04f,
        targetValue = 0.96f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "line",
    )
    val accent = MaterialTheme.colorScheme.primary

    Canvas(modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        val side = size.minDimension * 0.7f
        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f - size.height * 0.05f
        val right = left + side
        val bottom = top + side
        val radius = 28.dp.toPx()
        val len = 46.dp.toPx()

        // Dim everything, then punch a clear window for the scan area.
        drawRect(Color.Black.copy(alpha = 0.58f))
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(side, side),
            cornerRadius = CornerRadius(radius),
            blendMode = BlendMode.Clear,
        )

        // Rounded corner brackets
        val d = radius * 2f
        val brackets = Path().apply {
            // top-left
            moveTo(left, top + len)
            lineTo(left, top + radius)
            arcTo(Rect(left, top, left + d, top + d), 180f, 90f, false)
            lineTo(left + len, top)
            // top-right
            moveTo(right - len, top)
            lineTo(right - radius, top)
            arcTo(Rect(right - d, top, right, top + d), 270f, 90f, false)
            lineTo(right, top + len)
            // bottom-right
            moveTo(right, bottom - len)
            lineTo(right, bottom - radius)
            arcTo(Rect(right - d, bottom - d, right, bottom), 0f, 90f, false)
            lineTo(right - len, bottom)
            // bottom-left
            moveTo(left + len, bottom)
            lineTo(left + radius, bottom)
            arcTo(Rect(left, bottom - d, left + d, bottom), 90f, 90f, false)
            lineTo(left, bottom - len)
        }
        drawPath(
            path = brackets,
            color = accent,
            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
        )

        // Animated scan beam
        val y = top + side * progress
        val inset = 20.dp.toPx()
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(accent.copy(alpha = 0f), accent, accent.copy(alpha = 0f)),
                startX = left,
                endX = right,
            ),
            start = Offset(left + inset, y),
            end = Offset(right - inset, y),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun BrandChip() {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.QrCodeScanner,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.app_name),
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun RoundIconButton(
    icon: ImageVector,
    description: String,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (active) scheme.primary else Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (active) scheme.onPrimary else Color.White,
        )
    }
}

@Composable
private fun PermissionPrompt(
    modifier: Modifier,
    onAllow: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.CameraAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(40.dp),
            )
        }
        Spacer(Modifier.size(24.dp))
        Text(
            text = stringResource(R.string.perm_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(10.dp))
        Text(
            text = stringResource(R.string.perm_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(28.dp))
        Button(onClick = onAllow, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.perm_button))
        }
        TextButton(onClick = onSettings) {
            Text(stringResource(R.string.perm_open_settings))
        }
    }
}
