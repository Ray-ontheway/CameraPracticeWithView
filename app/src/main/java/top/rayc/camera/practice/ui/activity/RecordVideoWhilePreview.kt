package top.rayc.camera.practice.ui.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import top.rayc.camera.practice.R
import top.rayc.camera.practice.camera.RecordVideoWhilePreviewClient
import top.rayc.camera.practice.camera.common.AutoFitTextureView
import top.rayc.camera.practice.common.helper.showToast
import top.rayc.camera.practice.common.utils.FileUtils
import top.rayc.camera.practice.databinding.ActivityRecordVideoWhilePreviewBinding

class RecordVideoWhilePreview : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityRecordVideoWhilePreviewBinding

    companion object {
        const val TAG = "RecordVideoWhilePreview"

        const val REQUEST_PERMISSION_CODE = 10001
    }

    private var mRecordable = true

    private lateinit var textureView: AutoFitTextureView


    private lateinit var recordVideoWhilePreviewClient: RecordVideoWhilePreviewClient
    private var state = RecordVideoWhilePreviewClient.State.NEW

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordVideoWhilePreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRecordToggle.setOnClickListener(this)
        // 权限申请
        checkPermission()
        textureView = binding.preview!!

        recordVideoWhilePreviewClient = RecordVideoWhilePreviewClient(
            this,
            textureView,
            FileUtils.getVideoFilePath(this),
            object : RecordVideoWhilePreviewClient.CameraStateListener {
                override fun onStateChange(newState: RecordVideoWhilePreviewClient.State) {
                    state = newState
                }

            }
        )
    }

    override fun onResume() {
        super.onResume()
        recordVideoWhilePreviewClient.initialize()
    }

    override fun onStop() {
        super.onStop()
        recordVideoWhilePreviewClient.close()
    }

    override fun onClick(v: View) {
        when (v.id) {
            // 切换相机录制状态
            R.id.btn_record_toggle -> {
                Log.e(TAG, "onClick TOGGLE CLICK")
                if (mRecordable) {
                    startRecord()
                } else {
                    stopRecord()
                }
            }
        }
    }

    private fun checkPermission() {
        val cameraIsOk = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (!cameraIsOk) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun startRecord() {
        mRecordable = false
        val stateStr = when (state) {
            RecordVideoWhilePreviewClient.State.NEW -> "STATE.NEW"
            RecordVideoWhilePreviewClient.State.INITIALIZING -> "STATE.INITIALIZING"
            RecordVideoWhilePreviewClient.State.READY -> "STATE.READY"
            RecordVideoWhilePreviewClient.State.RECORDING -> "STATE.RECORDING"
            RecordVideoWhilePreviewClient.State.COMPETE -> "STATE.COMPLETE"
        }
        showToast(stateStr, Toast.LENGTH_SHORT)
        Log.e(TAG, "startRecord")
        if (state == RecordVideoWhilePreviewClient.State.READY ||
            state == RecordVideoWhilePreviewClient.State.COMPETE
            ) {
            recordVideoWhilePreviewClient.startRecord()
        }
    }

    private fun stopRecord() {
        mRecordable = true
        Log.e(TAG, "stopRecord")
        if (state == RecordVideoWhilePreviewClient.State.RECORDING) {
            recordVideoWhilePreviewClient.stopRecord()
        }
    }
}