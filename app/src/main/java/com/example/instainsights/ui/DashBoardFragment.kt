package com.example.instainsights.ui

// ui/DashboardFragment.kt

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.instainsights.R
import com.example.instainsights.adapter.PostsAdapter
import com.example.instainsights.databinding.FragmentDashboardBinding
import com.example.instainsights.models.UserSettings
import com.example.instainsights.viewmodel.CaptionState
import com.example.instainsights.viewmodel.DashboardData
import com.example.instainsights.viewmodel.DashboardViewModel
import com.example.instainsights.viewmodel.DashboardViewModelFactory
import com.example.instainsights.viewmodel.SaveState
import com.example.instainsights.viewmodel.UiState
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {
    private lateinit var imagePickerLauncher: ActivityResultLauncher<String>

// Null means no image has been picked yet
    private var selectedImageBase64: String? = null
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val autoDmCard get() = binding.cardAutoDm

    // Scoped to fragment, factory provides context-aware repo
    private val viewModel: DashboardViewModel by viewModels {
        DashboardViewModelFactory(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ) = FragmentDashboardBinding.inflate(inflater, container, false)
        .also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        registerImagePicker()

        setupAutoDmCard()
        setupCaptionSuggester()
        setupRecyclerView()
        observeUiState()
        observeSaveState()
        observeCaptionState()
        viewModel.loadDashboard()   // kick off parallel API calls
    }

    private fun setupRecyclerView() {
        // Horizontal scroll for post thumbnails
        binding.rvPosts.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is UiState.Loading -> showLoading(true)

                    is UiState.Success -> {
                        showLoading(false)
                        bindData(state.data)
                    }

                    is UiState.Error -> {
                        showLoading(false)
                        showError(state.message)
                    }
                }
            }
        }
    }

    private fun showLoading(loading: Boolean) {
        binding.progressBar.isVisible = loading
        binding.layoutProfile.isVisible = !loading
    }

    private fun bindAutoDmCard(settings: UserSettings) {

        // Check the right radio button based on saved mode
        when (settings.enableAutoDM) {
            "static" -> {
                autoDmCard.rgAutoDmMode.check(R.id.rbStatic)
                autoDmCard.tilStaticMessage.visibility = View.VISIBLE
                autoDmCard.etStaticMessage.setText(settings.message ?: "")
            }
            "ai" -> {
                autoDmCard.rgAutoDmMode.check(R.id.rbAi)
                autoDmCard.tilAiPrompt.visibility = View.VISIBLE
                autoDmCard.etAiPrompt.setText(settings.message ?: "")
            }
            else -> {
                // "no" — default, no inputs shown
                autoDmCard.rgAutoDmMode.check(R.id.rbNo)
                autoDmCard.tilStaticMessage.visibility = View.GONE
                autoDmCard.tilAiPrompt.visibility      = View.GONE
            }
        }
    }

    private fun bindData(data: DashboardData) {
        // --- Profile ---
        binding.tvUsername.text = "@${data.profile.username}"

        // --- Stat cards ---
        // Each card_stat layout has tvStatLabel, tvStatValue, tvStatSub
        binding.cardReach.apply {
            binding.cardReach.tvStatLabel.text  = "Reach"
            binding.cardReach.tvStatValue.text  = formatCount(data.insightsreach.data[0].values[0].value)
            binding.cardReach.tvStatSub.text    = "accounts · 24h"
        }
        binding.cardLikes.apply {
            binding.cardLikes.tvStatLabel.text  = "Likes"
            binding.cardLikes.tvStatValue.text  = formatCount(data.insightslikes.data[0].total_value.value)
            binding.cardLikes.tvStatSub.text    = "today"
        }


        // --- Posts RecyclerView ---
        binding.rvPosts.adapter = PostsAdapter(data.allPosts)

        // --- Demographics ---
        // Clear any old views first (handles refresh / re-observe)

        // Each demographic metric (age/gender/city/country) becomes a row

//        AutoDM binding
        bindAutoDmCard(data.userSettings)   // ← add at the end
    }
    private fun setupAutoDmCard() {

        // Show/hide input fields based on selected radio option
        autoDmCard.rgAutoDmMode.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbNo -> {
                    autoDmCard.tilStaticMessage.visibility = View.GONE
                    autoDmCard.tilAiPrompt.visibility      = View.GONE
                }
                R.id.rbStatic -> {
                    autoDmCard.tilStaticMessage.visibility = View.VISIBLE
                    autoDmCard.tilAiPrompt.visibility      = View.GONE
                    autoDmCard.etStaticMessage.requestFocus()
                }
                R.id.rbAi -> {
                    autoDmCard.tilStaticMessage.visibility = View.GONE
                    autoDmCard.tilAiPrompt.visibility      = View.VISIBLE
                    autoDmCard.etAiPrompt.requestFocus()
                }
            }
        }

        // Save button — read current selections and push to server
        autoDmCard.btnSaveAutoDm.setOnClickListener {
            val mode = when (autoDmCard.rgAutoDmMode.checkedRadioButtonId) {
                R.id.rbStatic -> "static"
                R.id.rbAi     -> "ai"
                else          -> "no"
            }

            // Grab the relevant message; empty string for "no"
            val message = when (mode) {
                "static" -> autoDmCard.etStaticMessage.text?.toString()?.trim()
                "ai"     -> autoDmCard.etAiPrompt.text?.toString()?.trim()
                else     -> null
            }

            // Basic validation before hitting the network
            if (mode == "static" && message.isNullOrEmpty()) {
                autoDmCard.tilStaticMessage.error = "Please enter a reply message"
                return@setOnClickListener
            }
            if (mode == "ai" && message.isNullOrEmpty()) {
                autoDmCard.tilAiPrompt.error = "Please enter an AI prompt"
                return@setOnClickListener
            }

            // Clear any previous error
            autoDmCard.tilStaticMessage.error = null
            autoDmCard.tilAiPrompt.error      = null

            viewModel.saveAutoDmSettings(mode, message)
        }
    }
    private fun observeSaveState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.saveState.collect { state ->
                when (state) {
                    is SaveState.Saving -> {
                        autoDmCard.btnSaveAutoDm.isEnabled = false
                        autoDmCard.btnSaveAutoDm.text      = "Saving…"
                    }
                    is SaveState.Saved -> {
                        autoDmCard.btnSaveAutoDm.isEnabled = true
                        autoDmCard.btnSaveAutoDm.text      = "Save settings"
                        // Show the green "Saved" badge briefly
                        autoDmCard.tvSavedBadge.visibility = View.VISIBLE
                    }
                    is SaveState.Error -> {
                        autoDmCard.btnSaveAutoDm.isEnabled = true
                        autoDmCard.btnSaveAutoDm.text      = "Save settings"
                        Snackbar.make(binding.root, state.message, Snackbar.LENGTH_SHORT).show()
                    }
                    is SaveState.Idle -> {
                        // Hide the saved badge when state resets
                        autoDmCard.tvSavedBadge.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun registerImagePicker() {
        imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            // uri is null if the user cancelled the picker
            uri ?: return@registerForActivityResult
            handleSelectedImage(uri)
        }
    }

    private fun handleSelectedImage(uri: Uri) {
        val card = binding.cardCaptionSuggester

        try {
            // Open an InputStream from the content URI
            val inputStream = requireContext().contentResolver.openInputStream(uri)
                ?: return

            // Read all bytes and encode to base64
            val bytes     = inputStream.readBytes()
            inputStream.close()
            selectedImageBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

            // Show the preview image using Glide
            card.ivImagePreview.visibility  = View.VISIBLE
            card.layoutPickerEmpty.visibility = View.GONE
            card.tvChangeImage.visibility   = View.VISIBLE

            Glide.with(this)
                .load(uri)
                .centerCrop()
                .into(card.ivImagePreview)

            // Enable generate button now that an image is ready
            card.btnGenerateCaption.isEnabled = true

            // Reset any previous result
            card.layoutCaptionResult.visibility  = View.GONE
            card.layoutCaptionLoading.visibility = View.GONE
            viewModel.resetCaptionState()

        } catch (e: Exception) {
            Snackbar.make(binding.root, "Failed to load image", Snackbar.LENGTH_SHORT).show()
        }
    }
    private fun setupCaptionSuggester() {
        val card = binding.cardCaptionSuggester

        // Tapping the picker area or the Change chip both open the picker
        card.cardImagePicker.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }
        card.tvChangeImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        // Generate button
        card.btnGenerateCaption.setOnClickListener {
            val base64 = selectedImageBase64
            if (base64 == null) {
                Snackbar.make(binding.root, "Please select an image first",
                    Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.suggestCaption(base64)
        }

        // Copy icon
        card.ivCopyCaption.setOnClickListener {
            val caption = card.tvGeneratedCaption.text.toString()
            if (caption.isNotEmpty()) {
                val clipboard = requireContext()
                    .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("caption", caption))
                Snackbar.make(binding.root, "Caption copied!", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    // ── Observe caption generation state ──
    private fun observeCaptionState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.captionState.collect { state ->
                val card = binding.cardCaptionSuggester

                // Reset both overlays first then show the right one
                card.layoutCaptionLoading.visibility = View.GONE
                card.layoutCaptionResult.visibility = View.GONE
                card.btnGenerateCaption.isEnabled = selectedImageBase64 != null

                when (state) {
                    is CaptionState.Idle -> {
                        // Nothing to show — initial state or after reset
                    }

                    is CaptionState.Loading -> {
                        card.layoutCaptionLoading.visibility = View.VISIBLE
                        card.btnGenerateCaption.isEnabled = false
                    }

                    is CaptionState.Success -> {
                        card.layoutCaptionResult.visibility = View.VISIBLE
                        card.tvGeneratedCaption.text = state.caption
                    }

                    is CaptionState.Error -> {
                        Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
    private fun showError(message: String) {
        // Replace with a proper Snackbar or error state view in production
        binding.tvUsername.text = "Error: $message"
    }

    // Compact number formatter shared across the fragment
    private fun formatCount(count: Int): String = when {
        count >= 1_000_000 -> "%.1fM".format(count / 1_000_000f)
        count >= 1_000     -> "%.1fK".format(count / 1_000f)
        else               -> count.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null   // prevent memory leak
    }
}