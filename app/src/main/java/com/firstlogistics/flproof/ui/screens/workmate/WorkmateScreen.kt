package com.firstlogistics.flproof.ui.screens.workmate

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.firstlogistics.flproof.FLProofApplication
import com.firstlogistics.flproof.ui.components.FLProofTopBar
import com.firstlogistics.flproof.ui.theme.BrandBlue
import com.firstlogistics.flproof.ui.theme.NavyBackground

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WorkmateScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as FLProofApplication
    var webViewInstance: WebView? by remember { mutableStateOf(null) }

    // Request CAMERA and RECORD_AUDIO for in-app barcode scanning & speech recognition
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        val hasCam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val hasAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (!hasCam || !hasAudio) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        }
    }

    // Hardware back press handler for WebView
    BackHandler {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            FLProofTopBar(
                title = "Volume & Berat",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                showSettingsButton = false,
                actions = {
                    IconButton(
                        onClick = {
                            val portalBase = app.settingsRepository.vercelPortalUrl.trim().ifBlank {
                                app.settingsRepository.customerWebPortalUrl.trim()
                            }
                            val url = if (portalBase.isNotBlank()) {
                                val cleanBase = portalBase.substringBefore('?').trimEnd('/')
                                "$cleanBase/workmate"
                            } else {
                                "https://timestime.vercel.app/workmate"
                            }
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Buka di Web Browser",
                            tint = BrandBlue
                        )
                    }
                }
            )
        },
        containerColor = NavyBackground
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            allowFileAccess = true
                            allowContentAccess = true
                            mediaPlaybackRequiresUserGesture = false
                            cacheMode = WebSettings.LOAD_DEFAULT
                            useWideViewPort = true
                            loadWithOverviewMode = true
                        }

                        webViewClient = WebViewClient()

                        webChromeClient = object : WebChromeClient() {
                            override fun onPermissionRequest(request: PermissionRequest) {
                                request.grant(request.resources)
                            }
                        }

                        loadUrl("file:///android_asset/workmate.html")
                        webViewInstance = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

