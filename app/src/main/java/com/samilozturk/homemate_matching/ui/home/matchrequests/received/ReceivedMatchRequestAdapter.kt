package com.samilozturk.homemate_matching.ui.home.matchrequests.received

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.samilozturk.homemate_matching.R
import com.samilozturk.homemate_matching.data.model.MatchRequest
import com.samilozturk.homemate_matching.data.model.Student
import com.samilozturk.homemate_matching.databinding.ItemReceivedMatchRequestBinding

class ReceivedMatchRequestAdapter(
    private val requests: MutableList<MatchRequest.Received>,
    private val onStudentClick: (Student) -> Unit,
    private val onAccept: (MatchRequest.Received) -> Unit,
    private val onReject: (MatchRequest.Received) -> Unit,
) : RecyclerView.Adapter<ReceivedMatchRequestAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemReceivedMatchRequestBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindTo(request: MatchRequest.Received) {
            binding.apply {
                textViewName.text = request.targetStudent.fullName
                textViewEducation.isVisible = request.targetStudent.education != null
                textViewEducation.text = request.targetStudent.education?.toString()
                Glide.with(root).load(request.targetStudent.imageUrl).placeholder(R.drawable.image_placeholder).into(shapeableImageView)
                fabAccept.setOnClickListener {
                    onAccept(request)
                }
                fabReject.setOnClickListener {
                    onReject(request)
                }
                root.setOnClickListener {
                    onStudentClick(request.targetStudent)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReceivedMatchRequestBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val request = requests[position]
        holder.bindTo(request)
    }

    override fun getItemCount() = requests.size

    fun removeItem(request: MatchRequest.Received) {
        val position = requests.indexOf(request)
        requests.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, requests.size)
    }

}