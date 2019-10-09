package io.github.sfotakos.itos.presentation.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import io.github.sfotakos.itos.R
import io.github.sfotakos.itos.data.entities.APOD
import kotlinx.android.synthetic.main.item_apod.view.*

class ApodAdapter : RecyclerView.Adapter<ApodAdapter.ApodViewHolder>() {
    private var apodList : ArrayList<APOD> = mutableListOf<APOD>() as ArrayList<APOD>

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApodViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_apod, parent, false)
        return ApodViewHolder(v)
    }

    override fun onBindViewHolder(holder: ApodViewHolder, position: Int) {
        holder.bind(apodList[position])
    }

    override fun getItemCount(): Int = apodList.size

    fun addApod(apod: APOD) {
        apodList.add(apod)
        apodList.add(apod)
        apodList.add(apod)
        apodList.add(apod)
        apodList.add(apod)
        apodList.add(apod)
        notifyDataSetChanged()
    }

    class ApodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind (apod: APOD) {
            val context = itemView.context
            Glide.with(context)
                .load(apod.url)
                .transform(CenterCrop(), RoundedCorners(Math.round(ScalingUtil.dpToPixel(context, 8f))))
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