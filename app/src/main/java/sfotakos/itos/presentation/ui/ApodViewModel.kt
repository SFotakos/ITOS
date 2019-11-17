package sfotakos.itos.presentation.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import sfotakos.itos.data.entities.APOD
import sfotakos.itos.data.repositories.ApodBoundaryCallback
import sfotakos.itos.data.repositories.db.ApodDb
import sfotakos.itos.network.NetworkState
import sfotakos.itos.network.ResponseWrapper

class ApodViewModel(private val db: ApodDb) : ViewModel() {

    private val repoResult: MutableLiveData<ResponseWrapper<APOD>> = fetchApods()

    val apods: LiveData<PagedList<APOD>> = Transformations.switchMap(repoResult) { it.pagedList }
    val networkState: LiveData<NetworkState> =
        Transformations.switchMap(repoResult) { it.networkState }

    fun retry() {
        val listing = repoResult.value
        listing?.retry?.invoke()
    }

    //TODO remove magic numbers
    private fun fetchApods(): MutableLiveData<ResponseWrapper<APOD>> {
        val config = PagedList.Config.Builder()
            .setInitialLoadSizeHint(8)
            .setPageSize(4)
            .setEnablePlaceholders(false)
            .build()
        val livePageListBuilder = LivePagedListBuilder<Int, APOD>(
            db.apodDao().queryAllApods(),
            config
        )
        val boundaryCallback = ApodBoundaryCallback(db)
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
}