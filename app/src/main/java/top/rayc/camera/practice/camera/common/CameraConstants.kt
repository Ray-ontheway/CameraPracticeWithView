package top.rayc.camera.practice.camera.common

import android.Manifest
import android.os.Build


object CameraConstants {

    const val REQUEST_CODE_VIDEO_PERMISSIONS: Int = 1

    val REQUIRED_VIDEO_PERMISSIONS =
        mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()

}