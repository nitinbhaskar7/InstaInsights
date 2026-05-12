package com.example.instainsights.adapter
// adapter/PostsAdapter.kt

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.instainsights.PostInsightsActivity
import com.example.instainsights.R
import com.example.instainsights.databinding.ItemPostBinding
import com.example.instainsights.models.DataXX

class PostsAdapter(private val posts: List<DataXX>) :
    RecyclerView.Adapter<PostsAdapter.PostViewHolder>() {

    inner class PostViewHolder(val binding: ItemPostBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        PostViewHolder(
            ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun getItemCount() = posts.size

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        with(holder.binding) {
            // Load thumbnail with Glide; show placeholder if URL is null
            Glide.with(root.context)
                .load(post.media_url)
                .centerCrop()
                .placeholder(R.drawable.ic_comment)
                .into(ivThumbnail)

            tvLikes.text    = formatCount(post.like_count)
            tvComments.text = formatCount(post.comments_count)

            root.setOnClickListener {
                val intent = Intent(root.context, PostInsightsActivity::class.java).apply {
                    // Pass the full Post object — works because Post is Parcelable
                    putExtra(PostInsightsActivity.EXTRA_POST, post)
                }
                root.context.startActivity(intent)
            }
        }
    }

    // Converts 1500 → "1.5K", 1200000 → "1.2M"
    private fun formatCount(count: Int): String = when {
        count >= 1_000_000 -> "%.1fM".format(count / 1_000_000f)
        count >= 1_000     -> "%.1fK".format(count / 1_000f)
        else               -> count.toString()
    }
}