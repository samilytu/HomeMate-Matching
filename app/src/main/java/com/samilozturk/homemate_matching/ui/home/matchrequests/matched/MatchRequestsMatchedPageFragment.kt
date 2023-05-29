package com.samilozturk.homemate_matching.ui.home.matchrequests.matched

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.samilozturk.homemate_matching.data.model.MatchRequest
import com.samilozturk.homemate_matching.data.source.db.DbRepository
import com.samilozturk.homemate_matching.databinding.FragmentMatchRequestsMatchedPageBinding
import com.samilozturk.homemate_matching.ui.home.profile.ProfileActivity
import com.samilozturk.homemate_matching.util.snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MatchRequestsMatchedPageFragment : Fragment() {

    @Inject
    lateinit var dbRepository: DbRepository

    private lateinit var binding: FragmentMatchRequestsMatchedPageBinding

    private lateinit var adapter: MatchedMatchRequestAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMatchRequestsMatchedPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetchMatchRequests()
    }

    private fun fetchMatchRequests() {
        lifecycleScope.launch {
            binding.recyclerView.isVisible = false
            binding.textViewEmpty.isVisible = false
            try {
                val requests = dbRepository.getMatchedRequests()
                listRequests(requests)
            } catch (e: CancellationException) {
                // ignore
            } catch (e: Exception) {
                activity?.snackbar(e.message.toString(), isError = true)
            }
            binding.progressBar.hide()
            binding.recyclerView.isVisible = true
        }
    }

    private fun listRequests(requests: List<MatchRequest.Matched>) {
        binding.textViewEmpty.isVisible = requests.isEmpty()
        adapter = MatchedMatchRequestAdapter(
            requests.toMutableList(),
            onStudentClick = { student ->
                // navigate to student profile
                val intent = Intent(requireContext(), ProfileActivity::class.java)
                intent.putExtra("student", student)
                startActivity(intent)
            },
            onAgree = { request ->
                // show confirmation dialog
                val dialog = MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Anlaş")
                    .setMessage("${request.targetStudent.fullName} adlı öğrenci ile anlaşmak istediğinize emin misiniz?")
                    .setPositiveButton("Evet") { _, _ ->
                        agreeMatchRequest(request)
                    }
                    .setNegativeButton("İptal", null)
                    .create()

                dialog.show()
            },
            onDisagree = { request ->
                // show confirmation dialog
                val dialog = MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Reddet")
                    .setMessage("${request.targetStudent.fullName} adlı öğrenci ile anlaşmayı reddetmek istediğinize emin misiniz?")
                    .setPositiveButton("Evet") { _, _ ->
                        disagreeMatchRequest(request)
                    }
                    .setNegativeButton("İptal", null)
                    .create()

                dialog.show()
            },
        )
        binding.recyclerView.adapter = adapter
    }

    private fun agreeMatchRequest(request: MatchRequest.Matched) {
        lifecycleScope.launch {
            try {
                dbRepository.agreeMatchRequest(request.uid)
                onMatchRequestAgreed(request)
            } catch (e: CancellationException) {
                // ignore
            } catch (e: Exception) {
                activity?.snackbar(e.message.toString(), isError = true)
            }
        }
    }

    private fun onMatchRequestAgreed(request: MatchRequest.Matched) {
        // remove request from list
        adapter.removeItem(request)
        binding.textViewEmpty.isVisible = adapter.itemCount == 0
        activity?.snackbar("Kullanıcı ile anlaşıldı.")
    }

    private fun disagreeMatchRequest(request: MatchRequest.Matched) {
        lifecycleScope.launch {
            try {
                dbRepository.disagreeMatchRequest(request.uid)
                onMatchRequestDisagreed(request)
            } catch (e: CancellationException) {
                // ignore
            } catch (e: Exception) {
                activity?.snackbar(e.message.toString(), isError = true)
            }
        }
    }

    private fun onMatchRequestDisagreed(request: MatchRequest.Matched) {
        // remove request from list
        adapter.removeItem(request)
        binding.textViewEmpty.isVisible = adapter.itemCount == 0
        activity?.snackbar("Kullanıcı ile anlaşma reddedildi.")
    }


}