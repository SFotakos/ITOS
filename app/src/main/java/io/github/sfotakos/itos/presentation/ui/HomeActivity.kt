package io.github.sfotakos.itos.presentation.ui

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.Observer
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.sfotakos.itos.R
import io.github.sfotakos.itos.data.entities.APOD
import io.github.sfotakos.itos.data.repositories.ApodDataSource
import io.github.sfotakos.itos.network.ConnectionLiveData
import io.github.sfotakos.itos.network.ResponseWrapper

import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.content_home.*

class HomeActivity : AppCompatActivity() {

    private lateinit var connectionLiveData : ConnectionLiveData

    private val adapter = ApodAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        setSupportActionBar(toolbar)

        connectionLiveData = ConnectionLiveData(this)

        initializeList()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_home_menu_info -> {
                Snackbar.make(this.toolbar, "Placeholder Info", Snackbar.LENGTH_LONG).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initializeList() {
        apod_recyclerView.layoutManager = LinearLayoutManager(this)
        apod_recyclerView.adapter = adapter

        val config = PagedList.Config.Builder()
            .setInitialLoadSizeHint(5)
            .setPageSize(3)
            .setEnablePlaceholders(false)
            .build()

        val liveData = initializedPagedListBuilder(config).build()

        liveData.observe(this, Observer<PagedList<APOD>> { pagedList ->
            adapter.submitList(pagedList)
        })
    }

    private fun initializedPagedListBuilder(config: PagedList.Config):
            LivePagedListBuilder<String, APOD> {

        val dataSourceFactory = object : DataSource.Factory<String, APOD>() {
            override fun create(): DataSource<String, APOD> {
                return ApodDataSource()
            }
        }
        return LivePagedListBuilder<String, APOD>(dataSourceFactory, config)
    }
}
