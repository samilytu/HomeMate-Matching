package com.samilozturk.homemate_matching.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.google.firebase.messaging.FirebaseMessaging
import com.samilozturk.homemate_matching.R
import com.samilozturk.homemate_matching.data.source.db.DbRepository
import com.samilozturk.homemate_matching.data.source.localstorage.LocalStorageRepository
import com.samilozturk.homemate_matching.databinding.ActivityHomeBinding
import com.samilozturk.homemate_matching.ui.home.matchrequests.MatchRequestsFragment
import com.samilozturk.homemate_matching.ui.home.profile.ProfileActivity
import com.samilozturk.homemate_matching.ui.home.students.StudentsFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {

    @Inject
    lateinit var localStorageRepository: LocalStorageRepository

    @Inject
    lateinit var dbRepository: DbRepository
    private lateinit var binding: ActivityHomeBinding

    // Declare the launcher at the top of your Activity/Fragment:
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
        } else {
            // TODO: Inform user that that your app will not show notifications.
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // configure bottom navigation
        binding.navView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_students -> {
                    replaceFragment(StudentsFragment())
                    true
                }

                R.id.navigation_requests -> {
                    replaceFragment(MatchRequestsFragment())
                    true
                }

                R.id.navigation_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    intent.putExtra("isEditable", true)
                    val student = localStorageRepository.getStudent()
                        ?: error("Student must not be null on HomeActivity")
                    intent.putExtra("student", student)
                    startActivity(intent)
                    false
                }

                else -> error("Unknown navigation item")
            }
        }

        // set default fragment
        binding.navView.selectedItemId = R.id.navigation_students

        askNotificationPermission()

        getFirebaseToken()

        getUptoDateProfile()
    }

    private fun getFirebaseToken() {
        lifecycleScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                Log.d("FirebaseToken", token)
                dbRepository.updateFcmToken(token)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.commit {
            replace(R.id.fragment_container, fragment)
        }
    }

    private fun getUptoDateProfile() {
        lifecycleScope.launch {
            try {
                val profile = dbRepository.getCurrentStudent()
                localStorageRepository.saveStudent(profile)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
