package top.rayc.camera.practice

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import top.rayc.camera.practice.common.helper.binding
import top.rayc.camera.practice.databinding.ActivityMainBinding
import top.rayc.camera.practice.ui.activity.RecordVideoWhilePreview

class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by binding()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.btnGoToRecordVideoWhilePreview.setOnClickListener { _ ->
            startActivity(Intent(this@MainActivity, RecordVideoWhilePreview::class.java))
        }

    }

}