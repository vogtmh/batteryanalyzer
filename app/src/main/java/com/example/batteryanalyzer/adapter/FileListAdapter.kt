package com.example.batteryanalyzer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.batteryanalyzer.databinding.ItemFileBinding
import com.example.batteryanalyzer.model.FileInfo

class FileListAdapter(
    private val onFileClick: (FileInfo) -> Unit
) : ListAdapter<FileInfo, FileListAdapter.FileViewHolder>(FileDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = ItemFileBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FileViewHolder(
        private val binding: ItemFileBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(fileInfo: FileInfo) {
            binding.tvFileName.text = fileInfo.name
            binding.tvFileSize.text = fileInfo.sizeFormatted
            binding.tvFileDate.text = fileInfo.dateFormatted

            binding.root.setOnClickListener {
                onFileClick(fileInfo)
            }
        }
    }

    private class FileDiffCallback : DiffUtil.ItemCallback<FileInfo>() {
        override fun areItemsTheSame(oldItem: FileInfo, newItem: FileInfo): Boolean {
            return oldItem.path == newItem.path
        }

        override fun areContentsTheSame(oldItem: FileInfo, newItem: FileInfo): Boolean {
            return oldItem == newItem
        }
    }
}
