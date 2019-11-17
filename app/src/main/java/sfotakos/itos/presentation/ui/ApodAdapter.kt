package sfotakos.itos.presentation.ui

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import sfotakos.itos.R
import sfotakos.itos.data.entities.APOD
import sfotakos.itos.data.entities.MediaType
import sfotakos.itos.network.NetworkState
import sfotakos.itos.network.Status
import kotlinx.android.synthetic.main.item_apod.view.*
import kotlin.math.roundToInt

//TODO proper error and APOD layout
//TODO separate view holders from adapter
class ApodAdapter(
    private val listener: ApodAdapterListener,
    private val retryCallback: () -> Unit
) :
    PagedListAdapter<APOD, RecyclerView.ViewHolder>(ApodDiffUtilCallback()) {

    private var networkState: NetworkState? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            //TODO justifiedText not working on lower API levels
            R.layout.item_apod -> {
                ApodViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_apod, parent, false), listener
                )
            }
            R.layout.network_state_item -> {
                NetworkStateItemViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.network_state_item, parent, false), retryCallback
                )
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
            R.layout.network_state_item -> (holder as NetworkStateItemViewHolder).bindTo(
                networkState
            )
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

    class ApodViewHolder(itemView: View, private val listener: ApodAdapterListener) :
        RecyclerView.ViewHolder(itemView) {

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

        fun bind(apod: APOD) {
            val context = itemView.context
            //TODO APOD API can return a video on occasion, as seen from 21/10
            itemView.apodPicture_imageView.visibility = View.INVISIBLE
            itemView.imageLoading.visibility = View.VISIBLE
            if (apod.mediaType == MediaType.IMAGE.mediaTypeValue) {
                Glide.with(context)
                    .load(apod.url)
                    .listener(requestListener)
                    .error(ContextCompat.getDrawable(context, R.drawable.ic_asteroid))
                    .transform(
                        FitCenter(), RoundedCorners(
                            ScalingUtil.dpToPixel(context, 8f).roundToInt()
                        )
                    )
                    .into(itemView.apodPicture_imageView)

                itemView.apodPicture_imageView.setOnClickListener {
                    ViewCompat.setTransitionName(itemView.apodPicture_imageView, apod.date)
                    listener.zoomImageFromThumb(itemView.apodPicture_imageView, apod.url)
                }
            } else {
                itemView.apodPicture_imageView.setImageDrawable(
                    ContextCompat.getDrawable(context, android.R.drawable.ic_media_play))
                itemView.apodPicture_imageView.setOnClickListener {
                    context.startActivity(Intent(ACTION_VIEW, Uri.parse(apod.url)))
                }
                itemView.apodPicture_imageView.visibility = View.VISIBLE
                itemView.imageLoading.visibility = View.GONE
            }
            itemView.apodTitle_textView.text = apod.title
            itemView.apodCopyright_textView.text = if (apod.copyright.isNullOrBlank()) {
                context.getString(
                    R.string.copyright_format,
                    context.getString(R.string.public_domain)
                )
            } else {
                context.getString(R.string.copyright_format, apod.copyright)
            }
            itemView.apodDate_textView.text = apod.date
            itemView.apodDescription_textView.text = apod.explanation
        }
    }

    class NetworkStateItemViewHolder(
        view: View,
        private val retryCallback: () -> Unit
    ) : RecyclerView.ViewHolder(view) {
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
            fun toVisibility(constraint: Boolean): Int {
                return if (constraint) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }
        }
    }

    interface ApodAdapterListener {
        fun zoomImageFromThumb(thumbView: View, imageUrl: String)
    }
}