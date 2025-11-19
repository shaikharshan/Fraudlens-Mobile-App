package com.example.fraudlens.ui.screens

import android.Manifest
import android.content.Context
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@Composable
fun QRCheckerScreen(){



}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QRScanner(
    context: Context
){
    var barcode = rememberSaveable {
        mutableStateOf<String>("No QR scanned")
    }
    // State to manage the camera permission
    val permissionState = rememberPermissionState(
        Manifest.permission.CAMERA // Permission being requested
    )

    // State to track whether to show the rationale dialog for the permission
    var oncancel by remember(permissionState.status.shouldShowRationale) {
        mutableStateOf(permissionState.status.shouldShowRationale)
    }

    val cameraController = remember {
        LifecycleCameraController(context)
    }
    val lifcycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = Modifier.fillMaxSize(), // Make the view take up the entire screen
        factory = { ctx ->
            PreviewView(ctx).apply {
                // Bind the camera controller to the lifecycle owner
                cameraController.bindToLifecycle(lifcycleOwner)

                // Set the camera controller for the PreviewView
                this.controller = cameraController
            }
        }
    )


}