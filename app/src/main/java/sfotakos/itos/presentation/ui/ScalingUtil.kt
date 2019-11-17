package sfotakos.itos.presentation.ui

import android.content.Context
import android.util.DisplayMetrics

class ScalingUtil {
    companion object {
        fun dpToPixel(context: Context, dp: Float): Float {
            return dp * (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
        }

        fun pixelsToDp(context: Context, pixels: Float): Float {
            return pixels / (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
        }
    }

}