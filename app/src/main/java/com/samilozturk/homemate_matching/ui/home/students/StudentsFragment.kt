package com.samilozturk.homemate_matching.ui.home.students

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.samilozturk.homemate_matching.R
import com.samilozturk.homemate_matching.data.model.Student
import com.samilozturk.homemate_matching.data.model.StudentFilter
import com.samilozturk.homemate_matching.data.source.db.DbRepository
import com.samilozturk.homemate_matching.databinding.DialogStudentFilterBinding
import com.samilozturk.homemate_matching.databinding.FragmentStudentsBinding
import com.samilozturk.homemate_matching.ui.home.profile.ProfileActivity
import com.samilozturk.homemate_matching.ui.home.students.map.MapActivity
import com.samilozturk.homemate_matching.util.snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class StudentsFragment : Fragment(), Toolbar.OnMenuItemClickListener {

    @Inject
    lateinit var dbRepository: DbRepository
    private lateinit var binding: FragmentStudentsBinding

    private val stateValues = listOf("Seçiniz") + Student.StudentType.values().map { it.toString() }

    private var badge: BadgeDrawable? = null
    private var filter: StudentFilter? = null
        @OptIn(ExperimentalBadgeUtils::class)
        set(value) {
            field = value
            // set badge to menu filter icon
            if (value == null) {
                BadgeUtils.detachBadgeDrawable(badge, binding.topAppBar, R.id.action_filter)
            } else {
                badge = BadgeDrawable.create(requireContext()).apply { number = 1 }
                BadgeUtils.attachBadgeDrawable(badge!!, binding.topAppBar, R.id.action_filter)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentStudentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    private var students: ArrayList<Student>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetchStudents()

        binding.topAppBar.setOnMenuItemClickListener(this)

        binding.buttonShowInMap.setOnClickListener {
            if (filteredStudents != null) {
                val intent = Intent(requireContext(), MapActivity::class.java)
                intent.putExtra("students", filteredStudents)
                startActivity(intent)
            }
        }
    }

    private fun fetchStudents() {
        lifecycleScope.launch {
            binding.recyclerView.isVisible = false
            binding.buttonShowInMap.isVisible = false
            binding.textViewEmpty.isVisible = false
            binding.progressBar.show()
            try {
                students = ArrayList(dbRepository.getAllStudents())
                listStudents()
            } catch (e: CancellationException) {
                // ignore
            } catch (e: Exception) {
                activity?.snackbar(e.message.toString(), isError = true)
            }
            binding.progressBar.hide()
            binding.recyclerView.isVisible = true
        }
    }

    private fun listStudents() {
        binding.textViewEmpty.isVisible = filteredStudents!!.isEmpty()
        binding.buttonShowInMap.isVisible =
            filteredStudents!!.any { it.type == Student.StudentType.PROVIDER }
        val adapter = StudentAdapter(filteredStudents!!) { student ->
            val intent = Intent(requireContext(), ProfileActivity::class.java)
            intent.putExtra("student", student)
            startActivity(intent)
        }
        binding.recyclerView.adapter = adapter
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_filter -> onFilterMenuItemClick()
        }
        return true
    }

    private fun onFilterMenuItemClick() {
        val dialogBinding = DialogStudentFilterBinding.inflate(layoutInflater)

        dialogBinding.spinnerState.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            stateValues
        )

        dialogBinding.rangeSliderDistance.setLabelFormatter { value ->
            "${"%.1f".format(value)} km"
        }

        dialogBinding.rangeSliderDistance.addOnChangeListener { slider, _, _ ->
            // set textViewDistance range value
            dialogBinding.textViewDistance.text =
                "${"%.1f".format(slider.values[0])} - ${"%.1f".format(slider.values[1])} km"
        }

        dialogBinding.rangeSliderTime.setLabelFormatter { value ->
            "${value.toInt()} ay"
        }

        dialogBinding.rangeSliderTime.addOnChangeListener { slider, _, _ ->
            // set textViewTime range value
            dialogBinding.textViewTime.text =
                "${slider.values[0].toInt()} - ${slider.values[1].toInt()} ay"
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Filtrele")
            .setView(dialogBinding.root)
            .setPositiveButton("Uygula") { dialog, _ ->
                onFilterSelect(dialogBinding)
            }
            .setNegativeButton("İptal", null)
            .setNeutralButton("Sıfırla") { dialog, _ ->
                filter = null
                listStudents()
            }
            .create()

        dialogBinding.spinnerState.onItemSelectedListener =
            onItemSelectedListener(dialog, dialogBinding)

        filter?.let { (type, distance, time) ->
            dialogBinding.spinnerState.setSelection(stateValues.indexOf(type.toString()))
            dialogBinding.rangeSliderDistance.values =
                listOf(distance.start, distance.endInclusive)
            dialogBinding.rangeSliderTime.values =
                listOf(time.first.toFloat(), time.last.toFloat())
        }

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).isEnabled = filter != null
    }

    private fun onItemSelectedListener(
        dialog: AlertDialog,
        binding: DialogStudentFilterBinding,
    ): AdapterView.OnItemSelectedListener {
        return object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long,
            ) {
                val selectedType =
                    Student.StudentType.values().find { it.toString() == stateValues[position] }
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = selectedType != null
                when (selectedType) {
                    Student.StudentType.SEEKER, Student.StudentType.PROVIDER -> {
                        binding.groupSliders.isVisible = true
                        binding.textView0.text =
                            if (selectedType == Student.StudentType.SEEKER) "Kalacağı Ev"
                            else "Paylaşacağı Ev"
                        binding.textView2.text =
                            if (selectedType == Student.StudentType.SEEKER) "Kampüse Olması Gereken Uzaklık (km)"
                            else "Kampüse Olan Uzaklık (km)"
                        binding.textView3.text =
                            if (selectedType == Student.StudentType.SEEKER) "Kalacağı Süre (ay)"
                            else "Paylaşacağı Süre (ay)"
                    }

                    else -> {
                        binding.groupSliders.isVisible = false
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun onFilterSelect(binding: DialogStudentFilterBinding) {
        filter = StudentFilter(
            type = Student.StudentType.values()
                .find { it.toString() == binding.spinnerState.selectedItem }!!,
            distanceToUniversity = binding.rangeSliderDistance.values.let { it[0]..it[1] },
            availableTime = binding.rangeSliderTime.values.let { it[0].toInt()..it[1].toInt() }
        )
        listStudents()
    }

    private val filteredStudents: ArrayList<Student>?
        get() {
            if (students == null) {
                return null
            }
            if (filter == null) {
                return students
            }
            return ArrayList(students!!.filter {
                when (it.type) {
                    Student.StudentType.PROVIDER, Student.StudentType.SEEKER -> filter!!.type == it.type && it.availability!!.distanceToUniversity in filter!!.distanceToUniversity && it.availability!!.availableTime in filter!!.availableTime
                    Student.StudentType.IDLE -> filter!!.type == Student.StudentType.IDLE
                }
            })
        }

}