package io.github.sfotakos.itos.presentation.ui

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.github.sfotakos.itos.APODViewModel
import io.github.sfotakos.itos.R
import io.github.sfotakos.itos.data.entities.APOD

import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.content_home.*
import org.json.JSONObject

class HomeActivity : AppCompatActivity() {

    private lateinit var viewModel: APODViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        setSupportActionBar(toolbar)

        viewModel = ViewModelProviders.of(this).get(APODViewModel::class.java)
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

    override fun onResume() {
        super.onResume()
        viewModel.getAPOD().observe(this, Observer { apod -> populateViews(apod) })
    }

    fun populateViews(apod: APOD) {
        Glide.with(this).load(apod.url).fitCenter().into(apodPicture_imageView)
        apodTitle_textView.text = apod.title
        apodCopyright_textView.text = apod.copyright
        apodDate_textView.text = apod.date
    }
}
