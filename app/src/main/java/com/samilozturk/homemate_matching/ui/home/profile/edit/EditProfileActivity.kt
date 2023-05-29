package com.samilozturk.homemate_matching.ui.home.profile.edit

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.samilozturk.homemate_matching.R
import com.samilozturk.homemate_matching.data.model.Student
import com.samilozturk.homemate_matching.data.model.credentials.EditProfileData
import com.samilozturk.homemate_matching.data.model.validation.EditProfileValidator
import com.samilozturk.homemate_matching.data.source.auth.AuthRepository
import com.samilozturk.homemate_matching.data.source.db.DbRepository
import com.samilozturk.homemate_matching.data.source.localstorage.LocalStorageRepository
import com.samilozturk.homemate_matching.data.source.remotestorage.RemoteStorageRepository
import com.samilozturk.homemate_matching.databinding.ActivityEditProfileBinding
import com.samilozturk.homemate_matching.ui.home.profile.edit.selectlocation.SelectLocationActivity
import com.samilozturk.homemate_matching.util.getFileName
import com.samilozturk.homemate_matching.util.snackbar
import com.samilozturk.homemate_matching.util.toUri
import com.samilozturk.homemate_matching.util.toast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class EditProfileActivity : AppCompatActivity(), Toolbar.OnMenuItemClickListener {

    @Inject
    lateinit var validator: EditProfileValidator

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var dbRepository: DbRepository

    @Inject
    lateinit var localStorageRepository: LocalStorageRepository

    @Inject
    lateinit var remoteStorageRepository: RemoteStorageRepository
    private lateinit var binding: ActivityEditProfileBinding

    private val progressDialog by lazy {
        ProgressDialog(this).apply {
            setCancelable(false)
            setCanceledOnTouchOutside(false)
        }
    }

    private lateinit var student: Student
    private var imageUri: Uri? = null

    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            // Callback is invoked after the user selects a media item or closes the
            // photo picker.
            if (uri != null) {
                imageUri = uri
                binding.imageView.setImageURI(uri)
                binding.buttonPickImage.text = "Resmi Değiştir"
                binding.buttonRemoveImage.isVisible = true
            }
        }

    private val stateValues = Student.StudentType.values().map { it.toString() }

    private var selectedHomeAddress: Student.HomeAddress? = null

    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                selectedHomeAddress = result.data?.getParcelableExtra("address")!!
                binding.textInputEditTextHomeLocation.setText(selectedHomeAddress!!.address)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.topAppBar.setNavigationOnClickListener {
            finish()
        }

        binding.topAppBar.setOnMenuItemClickListener(this)

        student = intent.getParcelableExtra("student")!!

        binding.buttonRemoveImage.isVisible = student.imageUrl != null

        student.imageUrl?.let {
            Glide.with(this)
                .load(it)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean,
                    ): Boolean {
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean,
                    ): Boolean {
                        val bitmap = (resource as BitmapDrawable).bitmap
                        imageUri = bitmap.toUri(this@EditProfileActivity)
                        return false
                    }

                })
                .into(binding.imageView)
            binding.buttonPickImage.text = "Resmi Değiştir"
        }

        binding.buttonPickImage.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.buttonRemoveImage.setOnClickListener {
            imageUri = null
            binding.imageView.setImageResource(R.drawable.image_placeholder)
            binding.buttonPickImage.text = "Profil Resmi Seç"
            binding.buttonRemoveImage.isVisible = false
        }

        binding.textInputEditTextFirstName.setText(student.firstName)
        binding.textInputEditTextLastName.setText(student.lastName)
        student.education?.let { (department, grade) ->
            binding.textInputEditTextDepartment.setText(department)
            binding.textInputEditTextGrade.setText(grade.toString())
        }

        binding.spinnerState.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, stateValues)

        selectedHomeAddress = student.homeAddress
        binding.textInputEditTextHomeLocation.setText(student.homeAddress?.address)
        binding.textInputEditTextHomeDistance.setText(student.availability?.distanceToUniversity?.toString())
        binding.textInputEditTextHomeTime.setText(student.availability?.availableTime?.toString())

        binding.spinnerState.onItemSelectedListener = onItemSelectedListener

        binding.spinnerState.setSelection(stateValues.indexOf(student.type.toString()))

        binding.textInputEditTextEmail.setText(student.email)
        binding.textInputEditTextPhone.setText(student.phone)

        binding.textInputEditTextHomeLocation.setOnClickListener {
            val intent = Intent(this, SelectLocationActivity::class.java)
            resultLauncher.launch(intent)
        }
    }

    private val onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(
            parent: AdapterView<*>?,
            view: View?,
            position: Int,
            id: Long,
        ) {
            when (val selectedType =
                Student.StudentType.values().find { it.toString() == stateValues[position] }) {
                Student.StudentType.SEEKER, Student.StudentType.PROVIDER -> {
                    binding.textViewHomeTitle.isVisible = true
                    binding.textViewHomeTitle.text =
                        if (selectedType == Student.StudentType.SEEKER) "Kalacağı Ev"
                        else "Paylaşacağı Ev"
                    binding.textInputLayoutHomeLocation.isVisible =
                        selectedType == Student.StudentType.PROVIDER
                    binding.textInputLayoutHomeDistance.isVisible = true
                    binding.textInputLayoutHomeDistance.hint =
                        if (selectedType == Student.StudentType.SEEKER) "Kampüse Olması Gereken Uzaklık (km)"
                        else "Kampüse Olan Uzaklık (km)"
                    binding.textInputLayoutHomeTime.isVisible = true
                    binding.textInputLayoutHomeTime.hint =
                        if (selectedType == Student.StudentType.SEEKER) "Kalacağı Süre (ay)"
                        else "Paylaşacağı Süre (ay)"
                }

                else -> {
                    binding.textViewHomeTitle.isVisible = false
                    binding.textInputLayoutHomeLocation.isVisible = false
                    binding.textInputLayoutHomeDistance.isVisible = false
                    binding.textInputLayoutHomeTime.isVisible = false
                }
            }
        }

        override fun onNothingSelected(parent: AdapterView<*>?) = Unit
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_save -> {
                try {
                    val firstName = binding.textInputEditTextFirstName.text.toString()
                    val lastName = binding.textInputEditTextLastName.text.toString()
                    val department = binding.textInputEditTextDepartment.text.toString()
                    val grade = binding.textInputEditTextGrade.text.toString()
                    val state = Student.StudentType.values()
                        .find { it.toString() == binding.spinnerState.selectedItem.toString() }!!
                    val distanceToUniversity = binding.textInputEditTextHomeDistance.text.toString()
                    val availableTime = binding.textInputEditTextHomeTime.text.toString()
                    val email = binding.textInputEditTextEmail.text.toString()
                    val phone = binding.textInputEditTextPhone.text.toString()
                    val data = EditProfileData(
                        firstName = firstName,
                        lastName = lastName,
                        department = department,
                        grade = grade,
                        state = state,
                        homeAddress = selectedHomeAddress,
                        distanceToUniversity = distanceToUniversity,
                        availableTime = availableTime,
                        email = email,
                        phone = phone
                    )
                    validator.validate(data)
                    saveProfile(data)
                } catch (e: Exception) {
                    snackbar(e.message.toString(), isError = true)
                }

            }
        }
        return true
    }

    private fun saveProfile(data: EditProfileData) {
        lifecycleScope.launch {
            try {
                progressDialog.setMessage("Profil güncelleniyor...")
                progressDialog.show()
                val newImagePath = imageUri?.let {
                    remoteStorageRepository.uploadImage(it, it.getFileName(contentResolver)!!)
                }
                val newStudent = Student.Factory.create(
                    uid = student.uid.value,
                    firstName = data.firstName,
                    lastName = data.lastName,
                    email = data.email,
                    imageUrl = newImagePath?.value,
                    phone = data.phone.takeIf { it.isNotBlank() },
                    department = data.department,
                    grade = data.grade.toInt(),
                    homeAddress = selectedHomeAddress?.address?.takeIf { data.state == Student.StudentType.PROVIDER },
                    homeLocation = selectedHomeAddress?.location?.takeIf { data.state == Student.StudentType.PROVIDER },
                    distanceToUniversity = data.distanceToUniversity.toFloat(),
                    availableTime = data.availableTime.toInt(),
                    isProvider = data.state == Student.StudentType.PROVIDER,
                    isSeeker = data.state == Student.StudentType.SEEKER,
                )
                dbRepository.updateStudent(newStudent)
                val newImageUrl = newImagePath?.let {
                    remoteStorageRepository.getDownloadUrl(it)
                }
                val newStudentFinal = newStudent.clone(imageUrl = newImageUrl)
                localStorageRepository.saveStudent(newStudentFinal)
                val intent = Intent().apply { putExtra("student", newStudentFinal) }
                setResult(Activity.RESULT_OK, intent)
                toast("Profil güncellendi")
                finish()
            } catch (e: CancellationException) {
                // ignore
            } catch (e: Exception) {
                snackbar(e.message.toString(), isError = true)
            } finally {
                progressDialog.dismiss()
            }
        }

    }

}