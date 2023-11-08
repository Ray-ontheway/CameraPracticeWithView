package top.rayc.camera.practice.ui.activity

import android.hardware.camera2.CameraDevice
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import top.rayc.camera.practice.R
import top.rayc.camera.practice.common.helper.binding
import top.rayc.camera.practice.databinding.ActivityRecordVideoWhilePreviewBinding

class RecordVideoWhilePreview : AppCompatActivity() {

    private val binding: ActivityRecordVideoWhilePreviewBinding by binding()

    private var mCameraDevice: CameraDevice? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    private fun initView() {
    }

    private fun initViewModel() {
    }

}