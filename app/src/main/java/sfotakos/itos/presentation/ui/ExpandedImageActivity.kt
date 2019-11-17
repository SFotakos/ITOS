package sfotakos.itos.presentation.ui

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import kotlinx.android.synthetic.main.activity_expanded_image.*
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import com.google.android.material.snackbar.Snackbar
import sfotakos.itos.R

class ExpandedImageActivity : AppCompatActivity() {

    companion object {
        const val TRANSITION_NAME_ARG = "TransitionNameArgument"
        const val APOD_URL_ARG = "ApodUrl"
        const val REQUEST_WRITE_STORAGE_REQUEST_CODE = 1234
    }

    lateinit var requestPermissionCallback : () -> Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_expanded_image)
        supportPostponeEnterTransition()
        supportActionBar?.hide()

        intent.extras?.let {
            expanded_ImageView.transitionName = it.getString(TRANSITION_NAME_ARG)

            Glide.with(this)
                .load(it.getString(APOD_URL_ARG))
                .listener( object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        supportStartPostponedEnterTransition()
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        supportStartPostponedEnterTransition()
                        resource?.let { drawable ->
                            requestPermissionCallback = {
                                saveImageToGallery(
                                    drawableToBitmap(drawable),
                                    "tempTitle",
                                    "tempDescr"
                                )
                            }
                        }
                        saveImage.setOnClickListener {
                            if (hasWritePermissions()) {
                                requestPermissionCallback.invoke()
                            } else {
                                requestWritePermission()
                            }
                        }
                        return false
                    }
                })
                .error(ContextCompat.getDrawable(this, R.drawable.ic_asteroid))
                .into(expanded_ImageView as ImageView)
        }
        closeDialog.setOnClickListener {
            super.onBackPressed()
        }
    }

    fun saveImageToGallery(bitmap: Bitmap, title: String, description: String) {
        MediaStore.Images.Media.insertImage(contentResolver, bitmap, title, description)
        Snackbar.make(saveImage, "Image Saved Successfully", Snackbar.LENGTH_LONG).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_WRITE_STORAGE_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestPermissionCallback.invoke()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun requestWritePermission() {
        if (hasWritePermissions()) {
            return
        }

        ActivityCompat.requestPermissions(
            this, arrayOf(WRITE_EXTERNAL_STORAGE), REQUEST_WRITE_STORAGE_REQUEST_CODE
        )
    }

    private fun hasWritePermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            baseContext, WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    fun drawableToBitmap(drawable: Drawable): Bitmap {
        val bitmap: Bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            Bitmap.createBitmap(
                1,
                1,
                Bitmap.Config.ARGB_8888
            ) // Single color bitmap will be created of 1x1 pixel
        } else {
            Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
        }

        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }

        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }


}
