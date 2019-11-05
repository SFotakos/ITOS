package io.github.sfotakos.itos.presentation.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import io.github.sfotakos.itos.R
import io.github.sfotakos.itos.data.entities.APOD
import kotlinx.android.synthetic.main.item_apod.view.*
import kotlin.math.roundToInt

class ApodAdapter : PagedListAdapter<APOD, ApodAdapter.ApodViewHolder>(ApodDiffUtilCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApodViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_apod, parent, false)
        return ApodViewHolder(v)
    }

    override fun onBindViewHolder(holder: ApodViewHolder, position: Int) {
       getItem(position)?.let {
          holder.bind(it)
       }
    }

    class ApodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind (apod: APOD) {
            val context = itemView.context
            //TODO APOD API can return a video on occasion, as seen from 21/10
            Glide.with(context)
                .load(apod.url)
                .transform(CenterCrop(), RoundedCorners(
                    ScalingUtil.dpToPixel(
                        context,
                        8f
                    ).roundToInt()
                ))
                .into(itemView.apodPicture_imageView)
            itemView.apodTitle_textView.text = apod.title
            itemView.apodCopyright_textView.text = if(apod.copyright.isNullOrBlank()) {
                context.getString(R.string.copyright_format, context.getString(R.string.public_domain))
            } else {
                context.getString(R.string.copyright_format, apod.copyright)
            }
            itemView.apodDate_textView.text = apod.date
            itemView.apodDescription_textView.text = apod.explanation
        }
    }
}