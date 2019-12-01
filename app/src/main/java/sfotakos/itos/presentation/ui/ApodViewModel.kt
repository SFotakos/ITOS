package sfotakos.itos.presentation.ui

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import sfotakos.itos.data.entities.APOD
import sfotakos.itos.data.repositories.APODService
import sfotakos.itos.data.repositories.ApodBoundaryCallback
import sfotakos.itos.data.repositories.db.ApodDb
import sfotakos.itos.data.repositories.db.ContinuityDb
import sfotakos.itos.network.NetworkState
import sfotakos.itos.network.ResponseWrapper
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class ApodViewModel(private val apodDb: ApodDb, private val continuityDb: ContinuityDb) : ViewModel() {

    companion object {
        const val INITIAL_LOAD_SIZE = 8
        const val PAGE_SIZE = 4
    }

    private val config = PagedList.Config.Builder()
        .setInitialLoadSizeHint(INITIAL_LOAD_SIZE)
        .setPageSize(PAGE_SIZE)
        .setEnablePlaceholders(false)
        .build()

    private var repoResult: MutableLiveData<ResponseWrapper<APOD>> = fetchApods()

    var apods: LiveData<PagedList<APOD>> = Transformations.switchMap(repoResult) { it.pagedList }
    var networkState: LiveData<NetworkState> =
        Transformations.switchMap(repoResult) { it.networkState }

    fun retry() {
        val listing = repoResult.value
        listing?.retry?.invoke()
    }

    private fun fetchApods(initialDate: String? = null): MutableLiveData<ResponseWrapper<APOD>> {
        val livePageListBuilder = LivePagedListBuilder<Int, APOD>(
            continuityDb.apodDao().queryAllApods(),
            config
        )
        val boundaryCallback = ApodBoundaryCallback(apodDb, continuityDb)
        boundaryCallback.setInitialKey(initialDate)
        livePageListBuilder.setBoundaryCallback(boundaryCallback)

        val mutableLiveData: MutableLiveData<ResponseWrapper<APOD>> = MutableLiveData()
        mutableLiveData.value = ResponseWrapper(
            pagedList = livePageListBuilder.build(),
            networkState = boundaryCallback.networkState,
            retry = {
                boundaryCallback.helper.retryAllFailed()
            }
        )

        return mutableLiveData
    }

    @SuppressLint("SimpleDateFormat")
    fun fetchApodByDate(date: Date) {
        apods.value?.dataSource?.invalidate()
        apods.value?.detach()
        thread {
            continuityDb.apodDao().deleteAll()
        }
        repoResult = fetchApods(SimpleDateFormat(APODService.QUERY_DATE_FORMAT).format(date))
        apods = Transformations.switchMap(repoResult) { it.pagedList }
        networkState = Transformations.switchMap(repoResult) { it.networkState }
    }
}