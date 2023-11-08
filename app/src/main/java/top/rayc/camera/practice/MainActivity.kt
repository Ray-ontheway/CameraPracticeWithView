package top.rayc.camera.practice

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import top.rayc.camera.practice.common.helper.binding
import top.rayc.camera.practice.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by binding()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.hello.text = "Hello ViewBinding"
    }

}