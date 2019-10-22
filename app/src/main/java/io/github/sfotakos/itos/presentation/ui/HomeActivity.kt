package io.github.sfotakos.itos.presentation.ui

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.sfotakos.itos.presentation.viewmodel.APODViewModel
import io.github.sfotakos.itos.R
import io.github.sfotakos.itos.data.entities.APOD
import io.github.sfotakos.itos.network.ConnectionLiveData
import io.github.sfotakos.itos.network.ResponseWrapper

import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.content_home.*

class HomeActivity : AppCompatActivity() {

    private lateinit var viewModel: APODViewModel
    private lateinit var connectionLiveData : ConnectionLiveData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        setSupportActionBar(toolbar)

        viewModel = ViewModelProviders.of(this).get(APODViewModel::class.java)
        connectionLiveData = ConnectionLiveData(this)

        connectionLiveData.observe(this, Observer { isConnected ->
            isConnected?.let {
                if (isConnected) {
                    val apodLiveData : LiveData<ResponseWrapper<APOD>> = viewModel.getApodLiveData()
                    viewModel.getApod()
                    apodLiveData.removeObservers(this)
                    apodLiveData.observe(this, Observer { apod ->
                        loading_progressBar.visibility = View.GONE
                        if (apod.data != null) (apod_recyclerView.adapter as ApodAdapter).addApod(apod.data)
                        else if (apod.apiException != null) showErrorMessage(apod.apiException.getErrorMessage())
                    })
                }
            }
        })

        apod_recyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        apod_recyclerView.adapter = ApodAdapter()
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

    private fun showErrorMessage(errorMessage: String) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
    }
}
