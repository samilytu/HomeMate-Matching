package com.samilozturk.homemate_matching.ui.home.matchrequests.received

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
import com.samilozturk.homemate_matching.databinding.FragmentMatchRequestsReceivedPageBinding
import com.samilozturk.homemate_matching.ui.home.profile.ProfileActivity
import com.samilozturk.homemate_matching.util.snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MatchRequestsReceivedPageFragment : Fragment() {

    @Inject
    lateinit var dbRepository: DbRepository

    private lateinit var binding: FragmentMatchRequestsReceivedPageBinding

    private lateinit var adapter: ReceivedMatchRequestAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentMatchRequestsReceivedPageBinding.inflate(inflater, container, false)
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
                val requests = dbRepository.getReceivedMatchRequests()
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

    private fun listRequests(requests: List<MatchRequest.Received>) {
        binding.textViewEmpty.isVisible = requests.isEmpty()
        adapter = ReceivedMatchRequestAdapter(
            requests.toMutableList(),
            onStudentClick = { student ->
                // navigate to student profile
                val intent = Intent(requireContext(), ProfileActivity::class.java)
                intent.putExtra("student", student)
                startActivity(intent)
            },
            onAccept = { request ->
                // show confirmation dialog
                val dialog = MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Onayla")
                    .setMessage("${request.targetStudent.fullName} adlı öğrencinin eşleşme talebini kabul etmek istediğinize emin misiniz?")
                    .setPositiveButton("Evet") { _, _ ->
                        acceptMatchRequest(request)
                    }
                    .setNegativeButton("İptal", null)
                    .create()

                dialog.show()
            },
            onReject = { request ->
                // show confirmation dialog
                val dialog = MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Reddet")
                    .setMessage("${request.targetStudent.fullName} adlı öğrencinin eşleşme talebini reddetmek istediğinize emin misiniz?")
                    .setPositiveButton("Evet") { _, _ ->
                        rejectMatchRequest(request)
                    }
                    .setNegativeButton("İptal", null)
                    .create()

                dialog.show()
            },
        )
        binding.recyclerView.adapter = adapter
    }

    private fun acceptMatchRequest(request: MatchRequest.Received) {
        lifecycleScope.launch {
            try {
                dbRepository.acceptMatchRequest(request.uid)
                onMatchRequestAccepted(request)
            } catch (e: CancellationException) {
                // ignore
            } catch (e: Exception) {
                activity?.snackbar(e.message.toString(), isError = true)
            }
        }
    }

    private fun onMatchRequestAccepted(request: MatchRequest.Received) {
        // remove request from list
        adapter.removeItem(request)
        binding.textViewEmpty.isVisible = adapter.itemCount == 0
        // navigate to matched tab
        // send callback to parent fragment
        parentFragmentManager.setFragmentResult("matchRequestAccepted", Bundle())
        activity?.snackbar("Eşleşme talebi kabul edildi.")
    }

    private fun rejectMatchRequest(request: MatchRequest.Received) {
        lifecycleScope.launch {
            try {
                dbRepository.rejectMatchRequest(request.uid)
                onMatchRequestRejected(request)
            } catch (e: CancellationException) {
                // ignore
            } catch (e: Exception) {
                activity?.snackbar(e.message.toString(), isError = true)
            }
        }
    }

    private fun onMatchRequestRejected(request: MatchRequest.Received) {
        // remove request from list
        adapter.removeItem(request)
        binding.textViewEmpty.isVisible = adapter.itemCount == 0
        activity?.snackbar("Eşleşme talebi reddedildi.")
    }

}