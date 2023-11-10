package top.rayc.camera.practice.camera.common

import android.content.Context
import android.util.AttributeSet
import android.view.TextureView

class AutoFitTextureView @JvmOverloads constructor(
    context: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : TextureView (context, attr, defStyle) {

    private var radioWith = 0
    private var radioHeight = 0

    fun setAspectRatio(width: Int, height: Int) {
        if (width < 0 || height < 0) {
            throw IllegalArgumentException("Size cannot be negative")
        }
        radioWith = width
        radioHeight = height
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getMode(widthMeasureSpec)
        val height = MeasureSpec.getMode(heightMeasureSpec)

        if (radioWith == 0 || radioHeight == 0) {
            setMeasuredDimension(width, height);
        } else {
            setMeasuredDimension(width, (width * radioHeight) / radioWith);
        }
    }

    companion object {
        private val TAG = AutoFitTextureView::class.java.simpleName
    }

}