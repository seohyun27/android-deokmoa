package com.example.deokmoa

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.deokmoa.data.api.Movie
import com.example.deokmoa.databinding.ItemSearchResultBinding


class SearchAdapter(
    private val onItemClicked: (Movie) -> Unit
) : RecyclerView.Adapter<SearchAdapter.SearchViewHolder>() {

    private var items: List<Movie> = emptyList()

    fun setItems(items: List<Movie>) {
        this.items = items
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val binding = ItemSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SearchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
        holder.itemView.setOnClickListener {
            onItemClicked(item)
        }
    }

    override fun getItemCount(): Int = items.size
    //영화 제목, 개봉일, 포스터 불러오기
    class SearchViewHolder(private val binding: ItemSearchResultBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(movie: Movie) {
            binding.tvMovieTitle.text = movie.title
            binding.tvMovieReleaseDate.text = movie.releaseDate?:"개봉일 정보 없음"
            val posterPath = movie.posterPath
            if(posterPath!= null) {
                val posterUrl = "https://image.tmdb.org/t/p/w500${movie.posterPath}"
                binding.ivMoviePoster.load(posterUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_launcher_background)
                    error(R.drawable.ic_launcher_background)
                }
            }else {
                binding.ivMoviePoster.setImageResource(R.drawable.ic_launcher_background)
            }
        }
    }
}
