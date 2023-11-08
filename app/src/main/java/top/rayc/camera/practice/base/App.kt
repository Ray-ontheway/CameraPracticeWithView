package top.rayc.camera.practice.base

import android.app.Application
import top.rayc.camera.practice.common.exception.CrashHandler

class App: Application() {

    override fun onCreate() {
        super.onCreate()

        // initialize CrashHandler
        CrashHandler.instance.init(this)
    }

}