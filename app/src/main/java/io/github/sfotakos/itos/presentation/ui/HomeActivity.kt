package io.github.sfotakos.itos.presentation.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.snackbar.Snackbar
import io.github.sfotakos.itos.data.entities.APOD
import io.github.sfotakos.itos.data.repositories.db.ApodDb
import io.github.sfotakos.itos.network.ConnectionLiveData
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.content_home.*
import kotlin.math.roundToInt

class HomeActivity : AppCompatActivity(), ApodAdapter.ApodAdapterListener {

    private val adapter = ApodAdapter(this) { viewModel.retry() }

    private lateinit var connectionLiveData: ConnectionLiveData
    private lateinit var viewModel: ApodViewModel

    private var currentAnimator: Animator? = null
    private var shortAnimationDuration: Int = 500

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(io.github.sfotakos.itos.R.layout.activity_home)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        connectionLiveData = ConnectionLiveData(this)
        viewModel = ViewModelProviders
            .of(this, ApodViewModelFactory(ApodDb.create(this)))
            .get(ApodViewModel::class.java)

        initializeList()
        initializeNetworkObserver()
    }

    private fun initializeNetworkObserver() {
        connectionLiveData.observe(this, Observer { isConnected ->
            isConnected?.let {
                viewModel.retry()
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(io.github.sfotakos.itos.R.menu.menu_home, menu)
        return true
    }


    //TODO info about the app
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            io.github.sfotakos.itos.R.id.action_home_menu_info -> {
                Snackbar.make(this.toolbar, "Placeholder Info", Snackbar.LENGTH_LONG).show()
                //TODO add attributions here
                //https://www.flaticon.com/free-icon/asteroid_2229768
                //https://www.fontspace.com/c%C3%A9-al/space-galaxy
                //https://www.fontspace.com/heaven-castro/izayoi-monospaced
                //https://www.flaticon.com/free-icon/galaxy_124567
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initializeList() {
        apod_recyclerView.layoutManager = LinearLayoutManager(this)
        apod_recyclerView.adapter = adapter
        apod_recyclerView.addItemDecoration(
            ApodItemDecoration(
                ContextCompat.getDrawable(
                    this,
                    io.github.sfotakos.itos.R.drawable.divider_apod
                )!!
            )
        )

        viewModel.apods.observe(this, Observer<PagedList<APOD>> {
            adapter.submitList(it)
        })

        viewModel.networkState.observe(this, Observer {
            adapter.setNetworkState(it)
        })
    }

    class ApodViewModelFactory(private val db: ApodDb) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ApodViewModel(db) as T
        }
    }

    override fun zoomImageFromThumb(thumbView: View, imageUrl: String) {
        // If there's an animation in progress, cancel it
        // immediately and proceed with this one.
        currentAnimator?.cancel()

        // Load the high-resolution "zoomed-in" image.
        Glide.with(this)
            .load(imageUrl)
            .transform(
                RoundedCorners(
                    ScalingUtil.dpToPixel(this, 8f).roundToInt()
                )
            )
            .into(apod_expanded_ImageView)

        // Calculate the starting and ending bounds for the zoomed-in image.
        // This step involves lots of math. Yay, math.
        val startBoundsInt = Rect()
        val finalBoundsInt = Rect()
        val globalOffset = Point()

        // The start bounds are the global visible rectangle of the thumbnail,
        // and the final bounds are the global visible rectangle of the container
        // view. Also set the container view's offset as the origin for the
        // bounds, since that's the origin for the positioning animation
        // properties (X, Y).
        thumbView.getGlobalVisibleRect(startBoundsInt)
        container.getGlobalVisibleRect(finalBoundsInt, globalOffset)
        startBoundsInt.offset(-globalOffset.x, -globalOffset.y)
        finalBoundsInt.offset(-globalOffset.x, -globalOffset.y)

        val startBounds = RectF(startBoundsInt)
        val finalBounds = RectF(finalBoundsInt)

        // Adjust the start bounds to be the same aspect ratio as the final
        // bounds using the "center crop" technique. This prevents undesirable
        // stretching during the animation. Also calculate the start scaling
        // factor (the end scaling factor is always 1.0).
        val startScale: Float
        if ((finalBounds.width() / finalBounds.height() > startBounds.width() / startBounds.height())) {
            // Extend start bounds horizontally
            startScale = startBounds.height() / finalBounds.height()
            val startWidth: Float = startScale * finalBounds.width()
            val deltaWidth: Float = (startWidth - startBounds.width()) / 2
            startBounds.left -= deltaWidth.toInt()
            startBounds.right += deltaWidth.toInt()
        } else {
            // Extend start bounds vertically
            startScale = startBounds.width() / finalBounds.width()
            val startHeight: Float = startScale * finalBounds.height()
            val deltaHeight: Float = (startHeight - startBounds.height()) / 2f
            startBounds.top -= deltaHeight.toInt()
            startBounds.bottom += deltaHeight.toInt()
        }

        // Hide the thumbnail and show the zoomed-in view. When the animation
        // begins, it will position the zoomed-in view in the place of the
        // thumbnail.
        thumbView.alpha = 0f
        apod_expanded_ImageView.visibility = View.VISIBLE

        // Set the pivot point for SCALE_X and SCALE_Y transformations
        // to the top-left corner of the zoomed-in view (the default
        // is the center of the view).
        apod_expanded_ImageView.pivotX = 0f
        apod_expanded_ImageView.pivotY = 0f

        // Construct and run the parallel animation of the four translation and
        // scale properties (X, Y, SCALE_X, and SCALE_Y).
        currentAnimator = AnimatorSet().apply {
            play(
                ObjectAnimator.ofFloat(
                    apod_expanded_ImageView,
                    View.X,
                    startBounds.left,
                    finalBounds.left
                )
            ).apply {
                with(
                    ObjectAnimator.ofFloat(
                        apod_expanded_ImageView,
                        View.Y,
                        startBounds.top,
                        finalBounds.top
                    )
                )
                with(ObjectAnimator.ofFloat(apod_expanded_ImageView, View.SCALE_X, startScale, 1f))
                with(ObjectAnimator.ofFloat(apod_expanded_ImageView, View.SCALE_Y, startScale, 1f))
            }
            duration = shortAnimationDuration.toLong()
            interpolator = DecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {

                override fun onAnimationEnd(animation: Animator) {
                    currentAnimator = null
                }

                override fun onAnimationCancel(animation: Animator) {
                    currentAnimator = null
                }
            })
            start()
        }

        // Upon clicking the zoomed-in image, it should zoom back down
        // to the original bounds and show the thumbnail instead of
        // the expanded image.
        apod_expanded_ImageView.setOnClickListener {
            currentAnimator?.cancel()

            // Animate the four positioning/sizing properties in parallel,
            // back to their original values.
            currentAnimator = AnimatorSet().apply {
                play(
                    ObjectAnimator.ofFloat(
                        apod_expanded_ImageView,
                        View.X,
                        startBounds.left
                    )
                ).apply {
                    with(ObjectAnimator.ofFloat(apod_expanded_ImageView, View.Y, startBounds.top))
                    with(ObjectAnimator.ofFloat(apod_expanded_ImageView, View.SCALE_X, startScale))
                    with(ObjectAnimator.ofFloat(apod_expanded_ImageView, View.SCALE_Y, startScale))
                }
                duration = shortAnimationDuration.toLong()
                interpolator = DecelerateInterpolator()
                addListener(object : AnimatorListenerAdapter() {

                    override fun onAnimationEnd(animation: Animator) {
                        thumbView.alpha = 1f
                        apod_expanded_ImageView.visibility = View.GONE
                        currentAnimator = null
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        thumbView.alpha = 1f
                        apod_expanded_ImageView.visibility = View.GONE
                        currentAnimator = null
                    }
                })
                start()
            }
        }
    }

}
