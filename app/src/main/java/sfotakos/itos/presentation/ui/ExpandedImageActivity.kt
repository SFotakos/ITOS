package sfotakos.itos.presentation.ui

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.animation.Animator
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import kotlinx.android.synthetic.main.activity_expanded_image.*
import sfotakos.itos.BuildConfig
import sfotakos.itos.R
import sfotakos.itos.data.FileUtils.addImageToGallery
import sfotakos.itos.data.FileUtils.checkIfFileExists
import sfotakos.itos.data.FileUtils.compressAndSaveImage
import sfotakos.itos.data.FileUtils.generateImagePath
import sfotakos.itos.data.entities.APOD
import java.io.File
import java.util.*
import kotlin.concurrent.schedule

class ExpandedImageActivity : AppCompatActivity() {

    companion object {
        const val APOD_IMAGE_TRANSITION_NAME = "ApodImageTransition"
        const val APOD_ARG = "ApodArgument"
        const val REQUEST_WRITE_STORAGE_REQUEST_CODE_SAVE = 7891
        const val REQUEST_WRITE_STORAGE_REQUEST_CODE_SHARE = 7892
        const val DOUBLE_CLICK_DELAY: Long = 300
    }

    lateinit var requestPermissionCallback: () -> Unit
    lateinit var shareImageCallback: () -> Unit
    lateinit var apod: APOD

    private val requestListener = object : RequestListener<Drawable> {
        override fun onLoadFailed(
            e: GlideException?, model: Any?, target: Target<Drawable>?,
            isFirstResource: Boolean
        ): Boolean {
            //TODO log error maybe retry
            supportStartPostponedEnterTransition()
            return false
        }

        override fun onResourceReady(
            resource: Drawable?, model: Any?, target: Target<Drawable>?,
            dataSource: DataSource?, isFirstResource: Boolean
        ): Boolean {

            supportStartPostponedEnterTransition()
            resource?.let { drawable ->
                requestPermissionCallback = {
                    var imagePath: Uri? = null
                    saveImage.addAnimatorListener(object : Animator.AnimatorListener {
                        override fun onAnimationRepeat(animation: Animator?) {
                            saveImage.cancelAnimation()
                            if (imagePath == null) {
                                Toast.makeText(
                                    this@ExpandedImageActivity,
                                    "Error saving image, please try again",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            saveImage.isClickable = true
                        }

                        override fun onAnimationStart(animation: Animator?) {
                            saveImage.isClickable = false
                        }

                        override fun onAnimationEnd(animation: Animator?) {/*unused*/
                        }

                        override fun onAnimationCancel(animation: Animator?) {/*unused*/
                        }
                    })
                    saveImage.playAnimation()
                    imagePath = saveImageToGallery(
                        drawableToBitmap(drawable),
                        apod.title,
                        apod.explanation,
                        apod.date
                    )
                }
                shareImageCallback = {
                    val imagePath = saveImageToGallery(
                        drawableToBitmap(drawable),
                        apod.title,
                        apod.explanation,
                        apod.date
                    )
                    if (imagePath != null) {
                        val shareIntent = Intent(Intent.ACTION_SEND)
                        shareIntent.type = "image/*"
                        shareIntent.putExtra(Intent.EXTRA_STREAM, imagePath)
                        val copyrightText =
                            if (apod.copyright != null) " captured by " + apod.copyright
                            else " as seen"
                        shareIntent.putExtra(
                            Intent.EXTRA_TEXT,
                            apod.title + copyrightText +
                                    " on " + apod.date +
                                    "\nYou can see more by downloading our app " +
                                    "https://play.google.com/store/apps/details?id=" +
                                    BuildConfig.APPLICATION_ID
                        )
                        startActivity(Intent.createChooser(shareIntent, "Share with..."))
                        Timer("AvoidDoubleClick", false).schedule(DOUBLE_CLICK_DELAY) {
                            shareApod.isClickable = true
                        }
                    } else {
                        Toast.makeText(
                            this@ExpandedImageActivity,
                            "Error sharing image, please try again",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            saveImage.setOnClickListener {
                if (hasWritePermissions()) {
                    requestPermissionCallback.invoke()
                } else {
                    requestWritePermission(REQUEST_WRITE_STORAGE_REQUEST_CODE_SAVE)
                }
            }
            shareApod.setOnClickListener {
                shareApod.isClickable = false
                if (hasWritePermissions()) {
                    shareImageCallback.invoke()
                } else {
                    requestWritePermission(REQUEST_WRITE_STORAGE_REQUEST_CODE_SHARE)
                }
            }
            return false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_expanded_image)
        supportPostponeEnterTransition()
        supportActionBar?.hide()

        val extras = intent.extras
        check(extras != null)

        val pictureTransitionName = extras.getString(APOD_IMAGE_TRANSITION_NAME)
        apod = extras.getSerializable(APOD_ARG) as APOD
        check(pictureTransitionName != null)

        expanded_ImageView.transitionName = pictureTransitionName

        Glide.with(this)
            .load(apod.url)
            .listener(requestListener)
            .error(ContextCompat.getDrawable(this, R.drawable.ic_destroyed_planet))
            .into(expanded_ImageView as ImageView)

        closeDialog.setOnClickListener {
            super.onBackPressed()
        }
    }

    fun saveImageToGallery(bitmap: Bitmap, title: String, description: String, date: String): Uri? {
        val storedImagePath: File = generateImagePath(title, date)
        if (checkIfFileExists(storedImagePath)) {
            return Uri.parse(storedImagePath.path)
        }
        if (!compressAndSaveImage(storedImagePath, bitmap)) {
            return null
        }
        return addImageToGallery(contentResolver, title, description, date, storedImagePath)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_WRITE_STORAGE_REQUEST_CODE_SAVE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestPermissionCallback.invoke()
            }
        } else if (requestCode == REQUEST_WRITE_STORAGE_REQUEST_CODE_SHARE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                shareImageCallback.invoke()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun requestWritePermission(requestCode: Int) {
        if (hasWritePermissions()) {
            return
        }

        ActivityCompat.requestPermissions(
            this, arrayOf(WRITE_EXTERNAL_STORAGE), requestCode
        )
    }

    private fun hasWritePermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            baseContext, WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun drawableToBitmap(drawable: Drawable): Bitmap {
        check(!(drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0))

        val bitmap: Bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )

        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }

        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}