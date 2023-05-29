package com.samilozturk.homemate_matching.ui.home.matchrequests.matched

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.samilozturk.homemate_matching.R
import com.samilozturk.homemate_matching.data.model.MatchRequest
import com.samilozturk.homemate_matching.data.model.Student
import com.samilozturk.homemate_matching.databinding.ItemMatchedMatchRequestBinding
import com.samilozturk.homemate_matching.util.openEmail
import com.samilozturk.homemate_matching.util.openWhatsapp

class MatchedMatchRequestAdapter(
    private val requests: MutableList<MatchRequest.Matched>,
    private val onStudentClick: (Student) -> Unit,
    private val onAgree: (MatchRequest.Matched) -> Unit,
    private val onDisagree: (MatchRequest.Matched) -> Unit,
) : RecyclerView.Adapter<MatchedMatchRequestAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemMatchedMatchRequestBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindTo(request: MatchRequest.Matched) {
            binding.apply {
                textViewName.text = request.targetStudent.fullName
                textViewEducation.isVisible = request.targetStudent.education != null
                textViewEducation.text = request.targetStudent.education?.toString()
                Glide.with(root).load(request.targetStudent.imageUrl)
                    .placeholder(R.drawable.image_placeholder).into(shapeableImageView)
                textViewEmail.text = request.targetStudent.email
                textViewEmail.setOnClickListener {
                    binding.root.context.openEmail(request.targetStudent.email)
                }
                textViewPhone.isVisible = request.targetStudent.phone != null
                request.targetStudent.phone?.let { phone ->
                    textViewPhone.text = phone
                    textViewPhone.setOnClickListener {
                        binding.root.context.openWhatsapp(phone)
                    }
                }
                fabAgree.setOnClickListener {
                    onAgree(request)
                }
                fabDisagree.setOnClickListener {
                    onDisagree(request)
                }
                root.setOnClickListener {
                    onStudentClick(request.targetStudent)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMatchedMatchRequestBinding.inflate(
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

    fun removeItem(request: MatchRequest.Matched) {
        val position = requests.indexOf(request)
        requests.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, requests.size)
    }

}