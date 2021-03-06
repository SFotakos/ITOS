package sfotakos.itos.presentation.ui

import androidx.recyclerview.widget.DiffUtil
import sfotakos.itos.data.entities.APOD

class ApodDiffUtilCallback : DiffUtil.ItemCallback<APOD>() {
    override fun areItemsTheSame(oldItem: APOD, newItem: APOD): Boolean {
        return oldItem.date == newItem.date
    }

    override fun areContentsTheSame(oldItem: APOD, newItem: APOD): Boolean {
        return oldItem.title == newItem.title
                && oldItem.explanation == newItem.explanation
                && oldItem.hdurl == newItem.hdurl
                && oldItem.copyright == newItem.copyright
                && oldItem.serviceVersion == newItem.serviceVersion
    }
}