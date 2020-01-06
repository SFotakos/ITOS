package sfotakos.itos.presentation.ui

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.content_home.*
import sfotakos.itos.data.entities.APOD
import sfotakos.itos.data.repositories.db.ApodDb
import sfotakos.itos.data.repositories.db.ContinuityDb
import sfotakos.itos.network.ConnectionLiveData
import sfotakos.itos.presentation.ui.ExpandedImageActivity.Companion.APOD_ARG
import sfotakos.itos.presentation.ui.ExpandedImageActivity.Companion.APOD_IMAGE_TRANSITION_NAME
import java.util.*
import java.util.Calendar.JUNE
import sfotakos.itos.R

class HomeActivity : AppCompatActivity(), ApodAdapter.ApodAdapterListener {

    private val adapter = ApodAdapter(this) { viewModel.retry() }

    private lateinit var connectionLiveData: ConnectionLiveData
    private lateinit var viewModel: ApodViewModel

    private val selectionCalendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        connectionLiveData = ConnectionLiveData(this)
        viewModel = ViewModelProviders
            .of(this, ApodViewModelFactory(ApodDb.create(this), ContinuityDb.create(this)))
            .get(ApodViewModel::class.java)

        initializeList()
        initializeNetworkObserver()
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
            R.id.action_home_menu_calendar -> {
                val dateSetListener: DatePickerDialog.OnDateSetListener? =
                    DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
                        selectionCalendar.set(Calendar.YEAR, year)
                        selectionCalendar.set(Calendar.MONTH, month)
                        selectionCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                        fetchApodByDate(selectionCalendar.time)
                    }
                val datePickerDialog = DatePickerDialog(
                    this, dateSetListener,
                    selectionCalendar.get(Calendar.YEAR),
                    selectionCalendar.get(Calendar.MONTH),
                    selectionCalendar.get(Calendar.DAY_OF_MONTH)
                )
                val minDateCalendar = Calendar.getInstance()
                minDateCalendar.set(Calendar.YEAR, 1995)
                minDateCalendar.set(Calendar.MONTH, JUNE)
                minDateCalendar.set(Calendar.DAY_OF_MONTH, 16)
                datePickerDialog.datePicker.maxDate = Calendar.getInstance().timeInMillis
                datePickerDialog.datePicker.minDate = minDateCalendar.timeInMillis
                datePickerDialog.show()
                true
            }
            R.id.action_home_menu_info -> {
                val dialog = Dialog(this)
                dialog.setContentView(R.layout.popup_about)
                dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                dialog.show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
                    R.drawable.divider_apod
                )!!
            )
        )
        attachObservers()
    }

    class ApodViewModelFactory(private val apodDb: ApodDb, private val continuityDb: ContinuityDb) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ApodViewModel(apodDb, continuityDb) as T
        }
    }

    override fun expandImage(apodPicture: View, apod: APOD) {
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
            this,
            apodPicture,
            ViewCompat.getTransitionName(apodPicture)!!
        )
        val expandedImageIntent = Intent(this, ExpandedImageActivity::class.java)
        expandedImageIntent.putExtra(
            APOD_IMAGE_TRANSITION_NAME,
            ViewCompat.getTransitionName(apodPicture)!!
        )
        expandedImageIntent.putExtra(APOD_ARG, apod)
        startActivity(expandedImageIntent, options.toBundle())
    }

    //TODO research if I really need to rebind the observer like this
    //TODO should attachObservers when viewModel has finished preparation, probably observe on another variable.
    private fun fetchApodByDate(date: Date) {
        detachObservers()
        viewModel.fetchApodByDate(date)
        attachObservers()
    }

    private fun detachObservers() {
        viewModel.apods.removeObservers(this)
        viewModel.networkState.removeObservers(this)
    }

    private fun attachObservers() {
        viewModel.apods.observe(this, Observer<PagedList<APOD>> {
            adapter.submitList(it)
        })

        viewModel.networkState.observe(this, Observer {
            adapter.setNetworkState(it)
        })
    }
}
