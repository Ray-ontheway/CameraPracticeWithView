package top.rayc.camera.practice.common.utils

import android.content.Context

object FileUtils {

    fun getVideoFilePath(context: Context): String {
        val filename = "${System.currentTimeMillis()}.mp4"
        val dir = context.getExternalFilesDir(null)

        return if (dir == null) {
            filename
        } else {
            "${dir.absoluteFile}/$filename"
        }
    }

}