package io.github.sfotakos.itos.presentation.ui

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import io.github.sfotakos.itos.R
import io.github.sfotakos.itos.data.entities.APOD
import io.github.sfotakos.itos.network.NetworkState
import io.github.sfotakos.itos.network.Status
import kotlinx.android.synthetic.main.item_apod.view.*
import kotlin.math.roundToInt

//TODO proper error and APOD layout
//TODO separate view holders from adapter
class ApodAdapter(private val retryCallback: () -> Unit):
    PagedListAdapter<APOD, RecyclerView.ViewHolder>(ApodDiffUtilCallback()) {

    private var networkState: NetworkState? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            //TODO justifiedText not working on lower API levels
            R.layout.item_apod -> {
                ApodViewHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_apod, parent, false))
            }
            R.layout.network_state_item -> {
                NetworkStateItemViewHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.network_state_item, parent, false), retryCallback)
            }
            else -> throw IllegalArgumentException("unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            R.layout.item_apod -> {
                getItem(position)?.let {
                    (holder as ApodViewHolder).bind(it)
                }
            }
            R.layout.network_state_item -> (holder as NetworkStateItemViewHolder).bindTo(networkState)
        }
    }

    override fun getItemCount(): Int {
        return super.getItemCount() + if (hasExtraRow()) 1 else 0
    }

    override fun getItemViewType(position: Int): Int {
        return if (hasExtraRow() && position == itemCount - 1) {
            R.layout.network_state_item
        } else {
            return R.layout.item_apod
        }
    }

    private fun hasExtraRow() = networkState != null && networkState != NetworkState.LOADED

    fun setNetworkState(newNetworkState: NetworkState?) {
        val previousState = this.networkState
        val hadExtraRow = hasExtraRow()
        this.networkState = newNetworkState
        val hasExtraRow = hasExtraRow()
        if (hadExtraRow != hasExtraRow) {
            if (hadExtraRow) {
                notifyItemRemoved(super.getItemCount())
            } else {
                notifyItemInserted(super.getItemCount())
            }
        } else if (hasExtraRow && previousState != newNetworkState) {
            notifyItemChanged(itemCount - 1)
        }
    }

    class ApodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val requestListener = object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>?,
                isFirstResource: Boolean
            ): Boolean {
                itemView.apodPicture_imageView.visibility = View.VISIBLE
                itemView.imageLoading.visibility = View.GONE
                return false
            }

            override fun onResourceReady(
                resource: Drawable?,
                model: Any?,
                target: Target<Drawable>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                itemView.apodPicture_imageView.visibility = View.VISIBLE
                itemView.imageLoading.visibility = View.GONE
                return false
            }
        }

        fun bind (apod: APOD) {
            val context = itemView.context
            //TODO APOD API can return a video on occasion, as seen from 21/10
            itemView.apodPicture_imageView.visibility = View.INVISIBLE
            itemView.imageLoading.visibility = View.VISIBLE
            Glide.with(context)
                .load(apod.url)
                .listener(requestListener)
                .error(ContextCompat.getDrawable(context, R.drawable.ic_asteroid))
                .transform(CenterCrop(), RoundedCorners(
                    ScalingUtil.dpToPixel(context,8f).roundToInt()
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

    class NetworkStateItemViewHolder(view: View,
                                     private val retryCallback: () -> Unit)
        : RecyclerView.ViewHolder(view) {
        private val progress = view.findViewById<LottieAnimationView>(R.id.progress)
        private val retry = view.findViewById<Button>(R.id.retry_button)
        private val errorMsg = view.findViewById<TextView>(R.id.error_msg)
        init {
            retry.setOnClickListener {
                retryCallback()
            }
        }
        fun bindTo(networkState: NetworkState?) {
            progress.visibility = toVisibility(networkState?.status == Status.RUNNING)
            retry.visibility = toVisibility(networkState?.status == Status.FAILED)
            errorMsg.visibility = toVisibility(networkState?.msg != null)
            errorMsg.text = networkState?.msg
        }

        companion object {
            fun toVisibility(constraint : Boolean): Int {
                return if (constraint) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }
        }
    }
}