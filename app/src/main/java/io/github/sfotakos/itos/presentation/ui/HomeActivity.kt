package io.github.sfotakos.itos.presentation.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
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
import io.github.sfotakos.itos.presentation.ui.ExpandedImageActivity.Companion.APOD_URL_ARG
import io.github.sfotakos.itos.presentation.ui.ExpandedImageActivity.Companion.TRANSITION_NAME_ARG
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.content_home.*
import kotlin.math.roundToInt

class HomeActivity : AppCompatActivity(), ApodAdapter.ApodAdapterListener {

    private val adapter = ApodAdapter(this) { viewModel.retry() }

    private lateinit var connectionLiveData: ConnectionLiveData
    private lateinit var viewModel: ApodViewModel

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

    //TODO add attributions to onOptionsItemSelected
    //https://www.flaticon.com/free-icon/asteroid_2229768
    //https://www.fontspace.com/c%C3%A9-al/space-galaxy
    //https://www.fontspace.com/heaven-castro/izayoi-monospaced
    //https://www.flaticon.com/free-icon/galaxy_124567

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
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
            this,
            thumbView,
            ViewCompat.getTransitionName(thumbView)!!
        )
        val expandedImageIntent = Intent(this, ExpandedImageActivity::class.java)
        expandedImageIntent.putExtra(TRANSITION_NAME_ARG, ViewCompat.getTransitionName(thumbView)!!)
        expandedImageIntent.putExtra(APOD_URL_ARG, imageUrl)
        startActivity(expandedImageIntent, options.toBundle())
    }
}
