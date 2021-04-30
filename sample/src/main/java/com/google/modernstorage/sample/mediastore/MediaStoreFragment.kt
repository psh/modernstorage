package com.google.modernstorage.sample.mediastore

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.modernstorage.media.CustomTakeVideo
import com.google.modernstorage.media.CustomTakePicture
import com.google.modernstorage.sample.R
import com.google.modernstorage.sample.databinding.FragmentMediastoreBinding

class MediaStoreFragment : Fragment() {
    private var _binding: FragmentMediastoreBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MediaStoreViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMediastoreBinding.inflate(inflater, container, false)

        setupButtonGroups()

        binding.addMedia.setOnClickListener {
            val type = when (binding.mediastoreType.checkedButtonId) {
                R.id.type_image -> MediaType.IMAGE
                R.id.type_video -> MediaType.VIDEO
                else -> throw Exception("This is not supposed to happen")
            }

            when (binding.mediastoreSource.checkedButtonId) {
                R.id.source_internet -> downloadMedia(type)
                R.id.source_camera -> captureMedia(type)
                else -> throw Exception("This is not supposed to happen")
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.addMedia.isEnabled = !isLoading
        }

        viewModel.currentMedia.observe(viewLifecycleOwner) { currentMedia ->
            currentMedia ?: return@observe

            binding.details.text = currentMedia.toString()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupButtonGroups() {
        binding.mediastoreType.clearChecked()
        binding.mediastoreSource.clearChecked()

        binding.mediastoreType.isSelectionRequired = true
        binding.mediastoreSource.isSelectionRequired = true

        binding.mediastoreType.check(R.id.type_image)
        binding.mediastoreSource.check(R.id.source_internet)
    }

    private fun downloadMedia(type: MediaType) {
        viewModel.setLoadingStatus(true)

        when (type) {
            MediaType.IMAGE -> {

                viewModel.saveRandomImageFromInternet { imageUri ->
                    viewModel.setLoadingStatus(false)
                    viewModel.setCurrentMedia(imageUri)
                }
            }
            MediaType.VIDEO -> viewModel.saveRandomVideoFromInternet { videoUri ->
                viewModel.setLoadingStatus(false)
                viewModel.setCurrentMedia(videoUri)
            }
        }
    }

    private fun captureMedia(type: MediaType) {
        viewModel.setLoadingStatus(true)

        viewModel.createMediaUriForCamera(type) { uri ->
            when (type) {
                MediaType.IMAGE -> actionTakeImage.launch(uri)
                MediaType.VIDEO -> actionTakeVideo.launch(uri)
            }
        }
    }

    private val actionTakeImage = registerForActivityResult(CustomTakePicture()) { success ->
        viewModel.setLoadingStatus(false)

        if (!success) {
            Log.e(tag, "Image taken FAIL")
            return@registerForActivityResult
        }

        Log.d(tag, "Image taken SUCCESS")

        if(viewModel.temporaryCameraImageUri == null) {
            Log.e(tag, "Can't find previously saved temporary Camera Image URI")
        } else {
            viewModel.setCurrentMedia(viewModel.temporaryCameraImageUri!!)
            viewModel.clearTemporaryCameraImageUri()
        }

    }

    private val actionTakeVideo = registerForActivityResult(CustomTakeVideo()) { uri ->
        viewModel.setLoadingStatus(false)

        if (uri == null) {
            Log.e(tag, "Video taken FAIL")
            return@registerForActivityResult
        }

        Log.d(tag, "Video taken SUCCESS")
        viewModel.setCurrentMedia(uri)
    }
}