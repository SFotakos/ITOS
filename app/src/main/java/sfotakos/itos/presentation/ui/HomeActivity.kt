package sfotakos.itos.presentation.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
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
import sfotakos.itos.data.entities.APOD
import sfotakos.itos.data.repositories.db.ApodDb
import sfotakos.itos.network.ConnectionLiveData
import sfotakos.itos.presentation.ui.ExpandedImageActivity.Companion.APOD_IMAGE_TRANSITION_NAME
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.content_home.*
import sfotakos.itos.presentation.ui.ExpandedImageActivity.Companion.APOD_ARG

class HomeActivity : AppCompatActivity(), ApodAdapter.ApodAdapterListener {

    private val adapter = ApodAdapter(this) { viewModel.retry() }

    private lateinit var connectionLiveData: ConnectionLiveData
    private lateinit var viewModel: ApodViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(sfotakos.itos.R.layout.activity_home)
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
                    sfotakos.itos.R.drawable.divider_apod
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

    override fun expandImage(apodPicture: View, apod: APOD) {
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
            this,
            apodPicture,
            ViewCompat.getTransitionName(apodPicture)!!
        )
        val expandedImageIntent = Intent(this, ExpandedImageActivity::class.java)
        expandedImageIntent.putExtra(APOD_IMAGE_TRANSITION_NAME, ViewCompat.getTransitionName(apodPicture)!!)
        expandedImageIntent.putExtra(APOD_ARG, apod)
        startActivity(expandedImageIntent, options.toBundle())
    }
}
