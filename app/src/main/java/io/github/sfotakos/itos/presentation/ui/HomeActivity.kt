package io.github.sfotakos.itos.presentation.ui

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.sfotakos.itos.data.entities.APOD
import io.github.sfotakos.itos.data.repositories.db.ApodDb
import io.github.sfotakos.itos.network.ConnectionLiveData

import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.content_home.*
import androidx.core.content.ContextCompat

class HomeActivity : AppCompatActivity() {

    private val adapter = ApodAdapter { viewModel.retry() }

    private lateinit var connectionLiveData : ConnectionLiveData
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
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initializeList() {
        apod_recyclerView.layoutManager = LinearLayoutManager(this)
        apod_recyclerView.adapter = adapter
        apod_recyclerView.addItemDecoration(
            ApodItemDecoration(ContextCompat.getDrawable(this, io.github.sfotakos.itos.R.drawable.divider_apod)!!))

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
}
