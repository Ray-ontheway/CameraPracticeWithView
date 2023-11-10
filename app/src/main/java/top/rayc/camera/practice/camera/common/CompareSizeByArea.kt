package com.eacenic.sqa.common.camera

import android.util.Size
import java.lang.Long.signum


class CompareSizeByArea() : Comparator<Size> {

    override fun compare(lhs: Size, rhs: Size): Int =
        signum((lhs.width * lhs.height).toLong() - (rhs.width * rhs.height).toLong())

}