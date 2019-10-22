package io.github.sfotakos.itos.presentation.ui

import androidx.recyclerview.widget.DiffUtil
import io.github.sfotakos.itos.data.entities.APOD
import io.github.sfotakos.itos.network.ResponseWrapper

class ApodDiffUtilCallback : DiffUtil.ItemCallback<ResponseWrapper<APOD>>() {
    override fun areItemsTheSame(oldItem: ResponseWrapper<APOD>, newItem: ResponseWrapper<APOD>): Boolean {
        return oldItem.data != null && newItem.data != null
                && oldItem.data.date == newItem.data.date
    }

    override fun areContentsTheSame(oldItem: ResponseWrapper<APOD>, newItem: ResponseWrapper<APOD>): Boolean {
        return oldItem.data != null && newItem.data != null
                && oldItem.data.title == newItem.data.title
                && oldItem.data.explanation == newItem.data.explanation
                && oldItem.data.hdurl == newItem.data.hdurl
                && oldItem.data.copyright == newItem.data.copyright
                && oldItem.data.service_version == newItem.data.service_version
    }
}