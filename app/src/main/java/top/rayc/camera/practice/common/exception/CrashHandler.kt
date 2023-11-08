package top.rayc.camera.practice.common.exception

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.Looper
import android.os.Process
import android.util.Log
import android.widget.Toast
import java.io.FileOutputStream
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.io.Writer
import java.nio.charset.StandardCharsets
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.HashMap

class CrashHandler private constructor(): Thread.UncaughtExceptionHandler{

    private var mDefaultHandler: Thread.UncaughtExceptionHandler? = null
    private var mContext: Context? = null
    private val infos: MutableMap<String, String> = HashMap()

    @SuppressLint("SimpleDateFormat")
    private val formatter: DateFormat = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")
    fun init(context: Context?) {
        mContext = context
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        if (!handleException(e) && mDefaultHandler != null) {
            mDefaultHandler!!.uncaughtException(t, e)
        } else {
            try {
                Thread.sleep(3000)
            } catch (interruptedException: InterruptedException) {
//                interruptedException.printStackTrace();
                Log.e(TAG, "uncaughtException: ", e)
            }
            Process.killProcess(Process.myPid())
            System.exit(1)
        }
    }

    private fun handleException(ex: Throwable?): Boolean {
        if (ex == null) {
            return false
        }
        object : Thread() {
            override fun run() {
                Looper.prepare()
                Toast.makeText(mContext, "程序出现Bug, 即将退出", Toast.LENGTH_SHORT).show()
                Looper.loop()
            }
        }.start()
        collectionDeviceInfo(mContext)
        saveCrashInfo2File(ex)
        return true
    }

    fun collectionDeviceInfo(ctx: Context?) {
        try {
            val pm = ctx!!.packageManager
            val pi = pm.getPackageInfo(ctx.packageName, PackageManager.GET_ACTIVITIES)
            pi.apply {
                infos["versionName"] = versionName
                infos["versionCode"] = "$versionCode"
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "collectionDeviceInfo: ", e)
        }
        val fields = Build::class.java.declaredFields
        for (field in fields) {
            try {
                field.isAccessible = true
                infos[field.name] = field[null].toString()
                Log.e(TAG, field.name + " : " + field[null])
            } catch (e: IllegalAccessException) {
                Log.e(TAG, "an error occured when collect crash info : ", e)
            }
        }
    }

    private fun saveCrashInfo2File(ex: Throwable): String? {
        val sb = StringBuffer()
        for ((key, value) in infos) {
            sb.append("$key = $value\n")
        }
        val writer: Writer = StringWriter()
        val printWriter = PrintWriter(writer)
        ex.printStackTrace(printWriter)
        var cause = ex.cause
        while (cause != null) {
            cause.printStackTrace(printWriter)
            cause = cause.cause
        }
        printWriter.close()
        val result = writer.toString()
        sb.append(result)
        try {
            val timestamp = System.currentTimeMillis()
            val time = formatter.format(Date())
            val fileName = "crash-$time-$timestamp.log"
            if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                val dir = mContext?.getExternalFilesDir("error")
                val fos = FileOutputStream("${dir?.absoluteFile}/${fileName}")
                fos.write(sb.toString().toByteArray(StandardCharsets.UTF_8))
                fos.close()
            }
        } catch (e: IOException) {
            Log.e(TAG, "saveCrashInfo2File:", e)
        }
        return null
    }

    companion object {
        private const val TAG = "CrashedHandler"
        val instance = CrashHandler()
    }

}