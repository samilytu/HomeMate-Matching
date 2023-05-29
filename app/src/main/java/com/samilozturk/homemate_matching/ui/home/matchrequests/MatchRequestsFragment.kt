package com.samilozturk.homemate_matching.ui.home.matchrequests

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.samilozturk.homemate_matching.databinding.FragmentMatchRequestsBinding

class MatchRequestsFragment : Fragment() {

    private lateinit var binding: FragmentMatchRequestsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentMatchRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.pager.adapter = MatchRequestsPagerAdapter(childFragmentManager)
        binding.tabLayout.setupWithViewPager(binding.pager)

        childFragmentManager.setFragmentResultListener("matchRequestAccepted", viewLifecycleOwner) { _, bundle ->
            binding.pager.currentItem = 2
        }
    }

}