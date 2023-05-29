package com.samilozturk.homemate_matching.ui.home.students

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.samilozturk.homemate_matching.R
import com.samilozturk.homemate_matching.data.model.Student
import com.samilozturk.homemate_matching.databinding.ItemStudentBinding

class StudentAdapter(
    private val students: List<Student>,
    private val onStudentClick: (Student) -> Unit
) : RecyclerView.Adapter<StudentAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemStudentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindTo(student: Student) {
            binding.apply {
                textViewName.text = student.fullName
                textViewState.text = student.type.toString()
                Glide.with(root).load(student.imageUrl).placeholder(R.drawable.image_placeholder).into(shapeableImageView)
                root.setOnClickListener {
                    onStudentClick(student)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStudentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val student = students[position]
        holder.bindTo(student)
    }

    override fun getItemCount() = students.size

}