package io.github.sfotakos.itos.presentation.ui

import android.graphics.drawable.Drawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import io.github.sfotakos.itos.R
import kotlinx.android.synthetic.main.activity_expanded_image.closeDialog
import kotlinx.android.synthetic.main.activity_expanded_image.expanded_ImageView

class ExpandedImageActivity : AppCompatActivity() {

    companion object {
        const val TRANSITION_NAME_ARG = "TransitionNameArgument"
        const val APOD_URL_ARG = "ApodUrl"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_expanded_image)
        supportPostponeEnterTransition()
        supportActionBar?.hide()

        intent.extras?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                expanded_ImageView.transitionName = it.getString(TRANSITION_NAME_ARG)
            }
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
                        return false
                    }
                })
                .error(ContextCompat.getDrawable(this, R.drawable.ic_asteroid))
                .into(expanded_ImageView)
        }
        closeDialog.setOnClickListener {
            super.onBackPressed()
        }
    }

}
