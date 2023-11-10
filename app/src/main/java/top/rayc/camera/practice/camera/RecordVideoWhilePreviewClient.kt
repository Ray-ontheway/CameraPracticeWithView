package top.rayc.camera.practice.camera

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.media.MediaCodec
import android.media.MediaRecorder
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import com.eacenic.sqa.common.camera.CompareSizeByArea
import top.rayc.camera.practice.camera.common.AutoFitTextureView
import top.rayc.camera.practice.common.helper.showToast
import top.rayc.camera.practice.ui.activity.RecordVideoWhilePreview
import java.util.Collections
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

class RecordVideoWhilePreviewClient(
    val context: Context,
    val mTextureView: AutoFitTextureView,
    val filePath: String,
    val stateListener: CameraStateListener
) {

    companion object {
        const val TAG = "RecordVideoWhilePreviewClient"
    }

    private var state: State = State.NEW

    val SENSOR_ORIENTATION_DEFAULT_DEGREES = 90
    private val SENSOR_ORIENTATION_INVERSE_DEGREES = 270
    private val DEAULT_ORIENTATIONS = SparseIntArray().apply {
        append(Surface.ROTATION_0, 90)
        append(Surface.ROTATION_90, 0)
        append(Surface.ROTATION_180, 270)
        append(Surface.ROTATION_270, 180)
    }
    private val INVERSE_ORIENTATIONS = SparseIntArray().apply {
        append(Surface.ROTATION_0, 270)
        append(Surface.ROTATION_90, 180)
        append(Surface.ROTATION_180, 90)
        append(Surface.ROTATION_270, 0)
    }

    private var mBackgroundThread: HandlerThread = HandlerThread(TAG)
    private lateinit var mBackHandler: Handler

    @Volatile
    private var mCameraDevice: CameraDevice? = null
    @Volatile
    private var mCameraCaptureSession: CameraCaptureSession? = null
    @Volatile
    private var mCaptureRequest: CaptureRequest.Builder? = null
    @Volatile
    private var mPreviewSurface: Surface? = null
    @Volatile
    private var mRecordSurface: Surface? = null
    @Volatile
    private var mMediaRecorder: MediaRecorder? = null

    @Volatile
    private var mSensorOrientation: Int = 0
    @Volatile
    private var mCameraOpenCloseLock = Semaphore(1)
    private lateinit var mPreviewSize: Size
    private lateinit var mVideoSize: Size

    private val mSurfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            openCamera(width, height)
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
            configureTransform(width, height)
        }

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture) = true

        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {
        }
    }

    private val mCameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(device: CameraDevice) {
            mCameraOpenCloseLock.release()
            mCameraDevice = device

            if (!mTextureView.isAvailable) return

            try {
                val texture = mTextureView.surfaceTexture!!
//                texture.setDefaultBufferSize(, 720)
                val previewSurface = Surface(texture)
                mPreviewSurface = previewSurface

                setupMediaRecorder(1280, 720)
                val recordSurface = mRecordSurface!!

                mCaptureRequest = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                    addTarget(previewSurface)
                    addTarget(recordSurface)
                }

                mCameraDevice?.createCaptureSession(
                    listOf(previewSurface, recordSurface),
                    mCaptureSessionStateCallback,
                    mBackHandler
                )
            } catch (e: CameraAccessException) {
                Log.e(TAG, "onOpened: ")
            }

        }

        override fun onDisconnected(device: CameraDevice) {
            mCameraOpenCloseLock.release()
            device.close()
            mCameraDevice = null
        }

        override fun onError(device: CameraDevice, error: Int) {
            mCameraOpenCloseLock.release()
            device.close()
            mCameraDevice = null
        }
    }

    private val mCaptureSessionStateCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            mCameraCaptureSession = session

            val camera = mCameraDevice ?: return
            val previewSurface = mPreviewSurface ?: return
            val recordSurface = mRecordSurface ?: return

            val captureRequest = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(previewSurface)
                addTarget(recordSurface)

//                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
//                set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON)
//                set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(30, 30))
            }

            mCaptureRequest = captureRequest

            session.setRepeatingRequest(captureRequest.build(), mCaptureCallback, mBackHandler)
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            Log.e(TAG, "CameraCaptureSession.StateCallback -- onConfigureFailed: ")
        }
    }

    private val mCaptureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            super.onCaptureCompleted(session, request, result)
            if (state == State.INITIALIZING) {
                onStateChange(State.READY)
            }
        }
    }

    init {
        mBackgroundThread.start()
        mBackHandler = Handler(mBackgroundThread.looper)
    }

    @SuppressLint("MissingPermission")
    fun openCamera(width: Int, height: Int) {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening")
            }
            val cameraId = manager.cameraIdList[0]
            val characteristics = manager.getCameraCharacteristics(cameraId)

            val range21 = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);

            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?: throw  RuntimeException("Cannot get available preview/video sizes")

            mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!


            mVideoSize = Size(1280, 720)
            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture::class.java), width, height, Size(2400, 1080));

            mTextureView.setAspectRatio(mPreviewSize.width, mPreviewSize.height)
            configureTransform(width, height)
            mMediaRecorder = MediaRecorder()
            manager.openCamera(cameraId, mCameraStateCallback, null)
        } catch (e: CameraAccessException) {
            Log.e(RecordVideoWhilePreview.TAG, "openCamera: Cannot access the camera. \n$e")
        } catch (e: NullPointerException) {
            Log.e(RecordVideoWhilePreview.TAG, "onRequestPermissionsResult: 没得相机硬件")
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.")
        }
    }

    private fun setupMediaRecorder(width: Int, height: Int) {
        var mediaRecorder = mMediaRecorder
        if (mMediaRecorder == null) {
            mediaRecorder = MediaRecorder()
        } else {
            mediaRecorder?.reset()
        }

        val rotation = (context as Activity).windowManager.defaultDisplay.rotation
        when (mSensorOrientation) {
            SENSOR_ORIENTATION_DEFAULT_DEGREES -> {
                mediaRecorder?.setOrientationHint(DEAULT_ORIENTATIONS.get(rotation))
            }
            SENSOR_ORIENTATION_INVERSE_DEGREES -> {
                mediaRecorder?.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation))
            }
        }

        var recordSurface = mRecordSurface
        if (recordSurface == null) {
            recordSurface = MediaCodec.createPersistentInputSurface()
            mRecordSurface = recordSurface
        }

        mediaRecorder?.apply {
            setInputSurface(recordSurface!!)

            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

            setOutputFile(filePath)

            setVideoEncodingBitRate(1280*720)
            setVideoSize(width, height)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)

            setCaptureRate(30.0)
            setVideoFrameRate(30)

            prepare()
        }

        mMediaRecorder = mediaRecorder
    }

    private fun chooseOptimalSize(
        choices: Array<Size>,
        width: Int,
        height: Int,
        aspectRatio: Size
    ): Size {
        val w = aspectRatio.width
        val h = aspectRatio.height
        val bigEnough = choices.filter {
            it.height == it.width * h / w && it.width >= width && it.height >= height
        }
        return if (bigEnough.isNotEmpty()) {
            Collections.min(bigEnough, CompareSizeByArea())
        } else {
            choices[0]
        }
    }

    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        val rotation = (context as Activity).windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f, mPreviewSize.height.toFloat(), mPreviewSize.width.toFloat())
        val centerY = viewRect.centerY()
        val centerX = viewRect.centerX()

        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
            val scale = Math.max(
                viewHeight.toFloat() / mPreviewSize.height,
                viewWidth.toFloat() / mPreviewSize.width
            )
            with(matrix) {
                postScale(scale, scale, centerX, centerY)
                postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
            }
        }
        mTextureView.setTransform(matrix)
    }

    private fun onStateChange(newState: State) {
        state = newState
        stateListener.onStateChange(state)
        val state = when (state) {
            State.NEW -> "STATE.NEW"
            State.INITIALIZING -> "STATE.INITIALIZING"
            State.READY -> "STATE.READY"
            State.RECORDING -> "STATE.RECORDING"
            State.COMPETE -> "STATE.COMPLETE"
        }
        context.showToast(state, Toast.LENGTH_SHORT)
    }

    fun initialize() {
        if (mTextureView.isAvailable) {
            openCamera(mTextureView.width, mTextureView.height)
        } else {
            mTextureView.surfaceTextureListener = mSurfaceTextureListener
        }
        onStateChange(State.INITIALIZING)
    }

    fun close() {
        mBackgroundThread?.quitSafely()
        try {
            mBackgroundThread?.join()
        } catch (e: InterruptedException) {
            Log.e(TAG, "close: ")
        }
        mPreviewSurface?.release()
        mRecordSurface?.release()
        mMediaRecorder?.release()
        mMediaRecorder = null
    }

    fun startRecord() {
        if (state != State.READY && state != State.COMPETE) return

        context.showToast("开始录像", Toast.LENGTH_SHORT)
        setupMediaRecorder(1280, 720)
        mMediaRecorder?.let {
            it.start()
        }
        onStateChange(State.RECORDING)
    }

    fun stopRecord() {
        if (state != State.RECORDING) return

        context.showToast("结束录像", Toast.LENGTH_SHORT)
        mMediaRecorder?.let {
            it.stop()
            it.reset()
        }
        onStateChange(State.COMPETE)
    }

    interface CameraStateListener {
        fun onStateChange(newState: State)
    }

    enum class State {
        NEW,
        INITIALIZING,
        READY,
        RECORDING,
        COMPETE,
    }


}