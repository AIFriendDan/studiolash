package com.studiolash.duplicatecleaner.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.studiolash.duplicatecleaner.R
import com.studiolash.duplicatecleaner.model.DuplicateGroup
import com.studiolash.duplicatecleaner.model.Photo

class DuplicateGroupAdapter(
    private val groups: List<DuplicateGroup>
) : RecyclerView.Adapter<DuplicateGroupAdapter.GroupViewHolder>() {

    private var editMode = false
    private val selectedToDelete = mutableSetOf<Long>()

    fun setEditMode(enabled: Boolean) {
        editMode = enabled
        notifyDataSetChanged()
    }

    inner class GroupViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvGroupLabel: TextView = view.findViewById(R.id.tvGroupLabel)
        val recyclerPhotos: RecyclerView = view.findViewById(R.id.recyclerPhotos)
        val tvWasted: TextView = view.findViewById(R.id.tvWasted)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_duplicate_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groups[position]
        val wastedKb = group.wastedBytes / 1024
        holder.tvGroupLabel.text = "Group ${position + 1} · ${group.photos.size} copies"
        holder.tvWasted.text = if (wastedKb > 1024) "${wastedKb / 1024}MB wasted" else "${wastedKb}KB wasted"

        val photoAdapter = PhotoThumbnailAdapter(
            group.photos,
            keepPhotoId = group.keepPhoto.id,
            editMode = editMode,
            selectedToDelete = selectedToDelete
        )
        holder.recyclerPhotos.layoutManager =
            LinearLayoutManager(holder.itemView.context, LinearLayoutManager.HORIZONTAL, false)
        holder.recyclerPhotos.adapter = photoAdapter
    }

    override fun getItemCount() = groups.size
}

class PhotoThumbnailAdapter(
    private val photos: List<Photo>,
    private val keepPhotoId: Long,
    private val editMode: Boolean,
    private val selectedToDelete: MutableSet<Long>
) : RecyclerView.Adapter<PhotoThumbnailAdapter.PhotoViewHolder>() {

    inner class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.ivPhoto)
        val tvLabel: TextView = view.findViewById(R.id.tvPhotoLabel)
        val card: CardView = view.findViewById(R.id.cardPhoto)
        val ivDeleteMark: ImageView = view.findViewById(R.id.ivDeleteMark)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo_thumbnail, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photo = photos[position]
        holder.imageView.load(photo.uri) {
            crossfade(true)
            placeholder(R.drawable.ic_photo_placeholder)
        }

        val isKeep = photo.id == keepPhotoId
        holder.tvLabel.text = if (isKeep) "KEEP" else "DUPLICATE"
        holder.tvLabel.setTextColor(
            holder.itemView.context.getColor(
                if (isKeep) R.color.keep_green else R.color.duplicate_red
            )
        )

        if (editMode && !isKeep) {
            val isMarked = photo.id in selectedToDelete
            holder.ivDeleteMark.visibility = if (isMarked) View.VISIBLE else View.GONE
            holder.card.alpha = if (isMarked) 0.5f else 1.0f
            holder.card.setOnClickListener {
                if (isMarked) selectedToDelete.remove(photo.id)
                else selectedToDelete.add(photo.id)
                notifyItemChanged(position)
            }
        } else {
            holder.ivDeleteMark.visibility = View.GONE
            holder.card.alpha = 1.0f
            holder.card.setOnClickListener(null)
        }
    }

    override fun getItemCount() = photos.size
}
