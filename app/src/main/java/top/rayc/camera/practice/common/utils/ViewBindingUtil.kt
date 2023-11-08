package top.rayc.camera.practice.common.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.viewbinding.ViewBinding
import java.lang.IllegalArgumentException
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.ParameterizedType

object ViewBindingUtil {

    private fun <VB: ViewBinding> withGenericBindingClass(genericOwner: Any, block: (Class<VB>) -> VB): VB {
        var genericSuperclass = genericOwner.javaClass.genericSuperclass
        var superclass = genericOwner.javaClass.superclass
        while (superclass != null) {
            if (genericSuperclass is ParameterizedType) {
                genericSuperclass.actualTypeArguments.forEach {
                    try {
                        return block.invoke(it as Class<VB>)
                    } catch (e: NoSuchMethodException) {
                    } catch (e: ClassCastException) {
                    } catch (e: InvocationTargetException) {
                        throw e.targetException
                    }
                }
            }
            genericSuperclass = superclass.genericSuperclass
            superclass = superclass.superclass
        }
        throw IllegalArgumentException("There is no generic of ViewBinding")
    }

    @JvmStatic
    fun <VB: ViewBinding> inflateWithGeneric(genericOwner: Any, layoutInflater: LayoutInflater): VB =
        withGenericBindingClass(genericOwner) { clazz ->
            // 反射
            clazz.getMethod("inflate", LayoutInflater::class.java).invoke(null, layoutInflater) as VB
        }.also { binding ->
            // 如果是 ViewDataBinding, 需要绑定lifecycle
        }

    @JvmStatic
    fun <VB : ViewBinding> inflateWithGeneric(genericOwner: Any, parent: ViewGroup): VB =
        inflateWithGeneric(genericOwner, LayoutInflater.from(parent.context), parent, false)

    @JvmStatic
    fun <VB : ViewBinding> inflateWithGeneric(genericOwner: Any, layoutInflater: LayoutInflater, parent: ViewGroup?, attachToParent: Boolean): VB =
        withGenericBindingClass<VB>(genericOwner) { clazz ->
            clazz.getMethod("inflate", LayoutInflater::class.java, ViewGroup::class.java, Boolean::class.java)
                .invoke(null, layoutInflater, parent, attachToParent) as VB
        }

    @JvmStatic
    fun <VB : ViewBinding> bindWithGeneric(genericOwner: Any, view: View): VB =
        withGenericBindingClass<VB>(genericOwner) { clazz ->
            clazz.getMethod("bind", View::class.java).invoke(null, view) as VB
        }

}