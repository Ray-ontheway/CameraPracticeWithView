package top.rayc.camera.practice.common.helper

import android.view.LayoutInflater
import androidx.activity.ComponentActivity
import androidx.viewbinding.ViewBinding

/**
 * TODO 有一个问题, 在一个Android Studio 创建的的一个默认的页面中, 有一个 {@link }
 *
 */
inline fun <reified VB: ViewBinding> ComponentActivity.binding() = lazy {
    inflateBinding<VB>(layoutInflater).also { binding ->
        setContentView(binding.root)
    }
}

inline fun <reified VB: ViewBinding> inflateBinding(layoutInflater: LayoutInflater) =
    VB::class.java.getMethod("inflate", LayoutInflater::class.java).invoke(null, layoutInflater) as VB

