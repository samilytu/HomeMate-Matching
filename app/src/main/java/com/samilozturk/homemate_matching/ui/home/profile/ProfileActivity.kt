package com.samilozturk.homemate_matching.ui.home.profile

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.ktx.addMarker
import com.google.maps.android.ktx.awaitMap
import com.samilozturk.homemate_matching.R
import com.samilozturk.homemate_matching.data.model.Student
import com.samilozturk.homemate_matching.data.model.isOppositeType
import com.samilozturk.homemate_matching.data.source.auth.AuthRepository
import com.samilozturk.homemate_matching.data.source.db.DbRepository
import com.samilozturk.homemate_matching.data.source.localstorage.LocalStorageRepository
import com.samilozturk.homemate_matching.databinding.ActivityProfileBinding
import com.samilozturk.homemate_matching.ui.MainActivity
import com.samilozturk.homemate_matching.ui.home.profile.edit.EditProfileActivity
import com.samilozturk.homemate_matching.util.openEmail
import com.samilozturk.homemate_matching.util.openWhatsapp
import com.samilozturk.homemate_matching.util.snackbar
import com.samilozturk.homemate_matching.util.toast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ProfileActivity : AppCompatActivity() {

    @Inject
    lateinit var dbRepository: DbRepository

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var localStorageRepository: LocalStorageRepository

    private lateinit var binding: ActivityProfileBinding
    private var isEditable = false

    private lateinit var googleMap: GoogleMap
    private var homeAddressMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topAppBar)

        // show back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        isEditable = intent.getBooleanExtra("isEditable", false)

        supportActionBar?.title = if (isEditable) "Profilim" else "Öğrenci Profili"

        val student = intent.getParcelableExtra<Student>("student")!!
        populate(student)

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapViewHome) as SupportMapFragment

        lifecycleScope.launch {
            googleMap = mapFragment.awaitMap()
            addLocationMarker(student.homeAddress)
        }
    }

    private fun addLocationMarker(homeAddress: Student.HomeAddress?) {
        homeAddressMarker?.remove()
        if (homeAddress == null) {
            return
        }
        homeAddressMarker = googleMap.addMarker {
            position(homeAddress.location)
            title(homeAddress.address)
        }
        googleMap.animateCamera(
            com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
                homeAddress.location,
                15f
            )
        )
    }

    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val student = result.data?.getParcelableExtra<Student>("student")!!
                populate(student)
                addLocationMarker(student.homeAddress)
            }
        }

    private fun populate(student: Student) {
        if (isEditable) {
            binding.buttonEditProfile.setOnClickListener {
                val intent = Intent(this, EditProfileActivity::class.java)
                intent.putExtra("student", student)
                resultLauncher.launch(intent)
            }
            binding.buttonLogout.setOnClickListener {
                onClickLogout()
            }
            binding.buttonSendRequest.isVisible = false
        } else {
            binding.buttonEditProfile.isVisible = false
            binding.buttonLogout.isVisible = false
            binding.buttonSendRequest.isVisible =
                localStorageRepository.getStudent()!!.isOppositeType(student)
            binding.buttonSendRequest.text = student.matchingStatus.toString()
            binding.buttonSendRequest.isEnabled = student.matchingStatus == Student.MatchingStatus.NoRequest
            binding.buttonSendRequest.setOnClickListener {
                onClickSendRequest(student)
            }
        }

        Glide.with(this).load(student.imageUrl).placeholder(R.drawable.image_placeholder)
            .into(binding.shapeableImageView)
        binding.textViewName.text = student.fullName
        binding.textViewState.text = student.type.toString()

        if (student.education != null) {
            binding.textViewEducation.text = student.education.toString()
            binding.textViewEducation.isVisible = true
        } else {
            binding.textViewEducation.isVisible = false
        }

        // show map fragment if student is provider, else hide it
        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapViewHome) as SupportMapFragment
        mapFragment.view?.isVisible = student is Student.Provider
        binding.textViewHomeAddress.isVisible = student is Student.Provider
        binding.textViewHomeAddress.text = student.homeAddress?.address

        when (student) {
            is Student.Seeker, is Student.Provider -> {
                binding.textViewHomeTitle.isVisible = true
                binding.textViewHomeTitle.text =
                    if (student is Student.Seeker) "Kalacağı Ev"
                    else "Paylaşacağı Ev"
                binding.textViewHomeDistance.text =
                    if (student is Student.Seeker) "Kampüse ${student.availability.distanceToUniversity} km uzaklıktaki bir evde"
                    else "Kampüse ${student.availability!!.distanceToUniversity} km uzaklıktaki evini"
                binding.textViewHomeDistance.isVisible = true
                binding.textViewHomeTime.text =
                    if (student is Student.Seeker) "${student.availability.availableTime} aylığına kalacak"
                    else "${student.availability!!.availableTime} aylığına paylaşacak"
                binding.textViewHomeTime.isVisible = true
            }

            else -> {
                binding.textViewHomeTitle.isVisible = false
                binding.textViewHomeDistance.isVisible = false
                binding.textViewHomeTime.isVisible = false
            }
        }

        if (isEditable || student.matchingStatus is Student.MatchingStatus.MatchedRequest) {
            binding.textViewContactEmail.text = student.email
            binding.textViewContactEmail.setOnClickListener {
                openEmail(student.email)
            }

            if (student.phone != null) {
                binding.textViewContactPhone.text = student.phone
                binding.textViewContactPhone.isVisible = true
                binding.textViewContactPhone.setOnClickListener {
                    openWhatsapp(student.phone!!)
                }
            } else {
                binding.textViewContactPhone.isVisible = false
            }
        } else {
            binding.textViewContactTitle.isVisible = false
            binding.textViewContactEmail.isVisible = false
            binding.textViewContactPhone.isVisible = false
        }
    }

    private fun onClickSendRequest(student: Student) {
        lifecycleScope.launch {
            try {
                dbRepository.sendMatchRequestTo(student.uid)
                snackbar("Eşleşme talebi başarıyla gönderildi.")
                binding.buttonSendRequest.text = Student.MatchingStatus.SentRequest.toString()
                binding.buttonSendRequest.isEnabled = false
            } catch (e: CancellationException) {
                // ignore
            } catch (e: Exception) {
                snackbar("Eşleşme talebi gönderilemedi.", isError = true)
            }
        }
    }

    private fun onClickLogout() {
        lifecycleScope.launch {
            try {
                // remove fcm token from db
                dbRepository.removeFcmToken()
                authRepository.logout()
                localStorageRepository.clearStudent()
                toast("Çıkış yapıldı")
                val intent = Intent(this@ProfileActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            } catch (e: CancellationException) {
                // ignore
            } catch (e: Exception) {
                snackbar("Çıkış yapılamadı", isError = true)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}