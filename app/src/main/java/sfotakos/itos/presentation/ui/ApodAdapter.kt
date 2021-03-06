package sfotakos.itos.presentation.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.DrawableImageViewTarget
import com.bumptech.glide.request.transition.Transition
import sfotakos.itos.ApodDateUtils.localizedDateString
import sfotakos.itos.R
import sfotakos.itos.data.entities.APOD
import sfotakos.itos.data.entities.MediaType
import sfotakos.itos.databinding.ItemApodBinding
import sfotakos.itos.databinding.NetworkStateItemBinding
import sfotakos.itos.network.NetworkState
import sfotakos.itos.network.Status
import kotlin.math.roundToInt

//TODO proper error layout
//TODO separate view holders from adapter
class ApodAdapter(
    private val listener: ApodAdapterListener,
    private val retryCallback: () -> Unit
) :
    PagedListAdapter<APOD, RecyclerView.ViewHolder>(ApodDiffUtilCallback()) {

    private var networkState: NetworkState? = null

    private lateinit var apodBinding: ItemApodBinding
    private lateinit var networkStateBinding: NetworkStateItemBinding

    companion object {
        const val ICON_MIN_SIZE = 96f
        const val LOADING_MIN_SIZE = 190f
        const val CROSSFADE_DURATION = 450
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.item_apod -> {
                apodBinding = ItemApodBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ApodViewHolder(apodBinding, listener)
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

    class ApodViewHolder(private val itemBinding: ItemApodBinding, private val listener: ApodAdapterListener) :
        RecyclerView.ViewHolder(itemBinding.root) {

        private val apodTarget = ApodTarget(itemBinding, itemBinding.apodPictureImageView)

        @SuppressLint("SimpleDateFormat")
        fun bind(apod: APOD) {
            val context = itemView.context
            itemBinding.apodPictureImageView.visibility = View.INVISIBLE
            itemBinding.imageLoading.visibility = View.VISIBLE

            when {
                apod.mediaType == MediaType.IMAGE.mediaTypeValue -> {
                    itemBinding.apodPictureImageView.adjustViewBounds = true
                    itemBinding.apodPictureImageView.minimumHeight =
                        ScalingUtil.dpToPixel(context, LOADING_MIN_SIZE).roundToInt()
                    itemBinding.apodPictureImageView.minimumWidth =
                        ScalingUtil.dpToPixel(context, LOADING_MIN_SIZE).roundToInt()
                    val layoutParams = itemBinding.apodPictureImageView.layoutParams
                    layoutParams.width = MATCH_PARENT
                    itemBinding.apodPictureImageView.layoutParams = layoutParams
                    Glide.with(context)
                        .load(apod.url)
                        .transition(DrawableTransitionOptions.withCrossFade(CROSSFADE_DURATION))
                        .error(ContextCompat.getDrawable(context, R.drawable.ic_destroyed_planet))
                        .transform(
                            RoundedCorners(
                                ScalingUtil.dpToPixel(context, 8f).roundToInt()
                            )
                        )
                        .into(apodTarget)

                    itemBinding.apodPictureImageView.setOnClickListener {
                        ViewCompat.setTransitionName(itemBinding.apodPictureImageView, apod.date)
                        listener.expandImage(itemBinding.apodPictureImageView, apod)
                    }
                }
                apod.mediaType == MediaType.VIDEO.mediaTypeValue -> {
                    itemBinding.apodPictureImageView.isClickable = true
                    itemBinding.apodPictureImageView.adjustViewBounds = false
                    itemBinding.apodPictureImageView.minimumHeight =
                        ScalingUtil.dpToPixel(context, ICON_MIN_SIZE).roundToInt()
                    itemBinding.apodPictureImageView.minimumWidth =
                        ScalingUtil.dpToPixel(context, ICON_MIN_SIZE).roundToInt()
                    val layoutParams = itemBinding.apodPictureImageView.layoutParams
                    layoutParams.width = WRAP_CONTENT
                    itemBinding.apodPictureImageView.setImageDrawable(
                        ContextCompat.getDrawable(context, R.drawable.ic_play_white_24dp)
                    )
                    itemBinding.apodPictureImageView.setOnClickListener {
                        context.startActivity(Intent(ACTION_VIEW, Uri.parse(apod.url)))
                    }
                    itemBinding.apodPictureImageView.visibility = View.VISIBLE
                    itemBinding.imageLoading.visibility = View.GONE
                }
                else -> throw IllegalStateException()
            }
            itemBinding.apodTitleTextView.text = apod.title
            itemBinding.apodCopyrightTextView.text = if (apod.copyright.isNullOrBlank()) {
                context.getString(
                    R.string.copyright_format,
                    context.getString(R.string.public_domain)
                )
            } else {
                context.getString(R.string.copyright_format, apod.copyright)
            }

            if (apod.explanation.isEmpty())
                itemBinding.apodDescriptionTextView.visibility = View.GONE
            else
                itemBinding.apodDescriptionTextView.visibility = View.VISIBLE

            itemBinding.apodDateTextView.text = localizedDateString(apod.date)
            itemBinding.apodDescriptionTextView.text = apod.explanation
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
        fun expandImage(apodPicture: View, apod: APOD)
    }

    class ApodTarget(private val itemBinding: ItemApodBinding, imageView: ImageView) :
        DrawableImageViewTarget(imageView) {

        override fun onLoadStarted(placeholder: Drawable?) {
            itemBinding.apodPictureImageView.visibility = View.INVISIBLE
            itemBinding.imageLoading.visibility = View.VISIBLE
            itemBinding.imageLoading.playAnimation()
            super.onLoadStarted(placeholder)
        }

        override fun onLoadFailed(errorDrawable: Drawable?) {
            itemBinding.apodPictureImageView.visibility = View.VISIBLE
            itemBinding.imageLoading.visibility = View.GONE
            val layoutParams = itemBinding.apodPictureImageView.layoutParams
            val context = itemBinding.root.context
            layoutParams.height = ScalingUtil.dpToPixel(context, LOADING_MIN_SIZE).roundToInt()
            layoutParams.width = ScalingUtil.dpToPixel(context, LOADING_MIN_SIZE).roundToInt()
            itemBinding.apodPictureImageView.layoutParams = layoutParams
            itemBinding.apodPictureImageView.setImageDrawable(
                ContextCompat.getDrawable(context, R.drawable.ic_destroyed_planet)
            )
            itemBinding.apodPictureImageView.isClickable = false
            super.onLoadFailed(errorDrawable)
        }

        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
            itemBinding.apodPictureImageView.minimumHeight = 0
            itemBinding.apodPictureImageView.minimumWidth = 0
            itemBinding.apodPictureImageView.visibility = View.VISIBLE
            itemBinding.imageLoading.visibility = View.GONE
            val layoutParams = itemBinding.apodPictureImageView.layoutParams
            layoutParams.height = WRAP_CONTENT
            layoutParams.width = MATCH_PARENT
            itemBinding.apodPictureImageView.layoutParams = layoutParams
            itemBinding.apodPictureImageView.setImageDrawable(resource)
            itemBinding.apodPictureImageView.isClickable = true
            super.onResourceReady(resource, transition)
        }
    }
}