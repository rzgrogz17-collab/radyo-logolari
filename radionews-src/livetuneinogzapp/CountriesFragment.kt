package com.globalradio.livetuneinogzapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.globalradio.livetuneinogzapp.adapter.CountryAdapter
import com.globalradio.livetuneinogzapp.databinding.FragmentCountriesBinding
import com.globalradio.livetuneinogzapp.viewmodel.MainViewModel

class CountriesFragment : Fragment() {

    private var _binding: FragmentCountriesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: CountryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCountriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = CountryAdapter { country ->
            val intent = Intent(requireContext(), CountryStationsActivity::class.java).apply {
                putExtra(CountryStationsActivity.EXTRA_COUNTRY, country.name)
                putExtra(CountryStationsActivity.EXTRA_COUNTRY_CODE, country.isoCode)
            }
            startActivity(intent)
        }

        binding.recyclerCountries.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@CountriesFragment.adapter
            setHasFixedSize(true)
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.countrySummaries.observe(viewLifecycleOwner) { countries ->
            adapter.submitList(countries)
            binding.tvEmptyCountries.visibility =
                if (countries.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
