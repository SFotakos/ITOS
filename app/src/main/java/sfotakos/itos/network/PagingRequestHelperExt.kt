package sfotakos.itos.network

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.PagingRequestHelper

private fun getErrorMessage(report: PagingRequestHelper.StatusReport): String {
    return PagingRequestHelper.RequestType.values().mapNotNull {
        report.getErrorFor(it)?.message
    }.first()
}

fun PagingRequestHelper.createStatusLiveData(): LiveData<PagingRequestHelper.StatusReport> {
    val liveData = MutableLiveData<PagingRequestHelper.StatusReport>()
    addListener {
        liveData.postValue(it)
    }
    return liveData
}