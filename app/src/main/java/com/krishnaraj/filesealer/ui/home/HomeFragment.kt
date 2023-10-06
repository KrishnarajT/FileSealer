package com.krishnaraj.filesealer.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.krishnaraj.filesealer.databinding.FragmentHomeBinding
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.widget.Button
import com.krishnaraj.filesealer.R


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // get the open file button.
        val openFileButton: Button = root.findViewById(R.id.open_file_btn)
        // attach an OnClickListener
        openFileButton.setOnClickListener {
            openFileExplorer()
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private val PICK_FILE_REQUEST_CODE = 1

    private fun openFileExplorer() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*" // Allow all file types
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(intent, PICK_FILE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            val selectedFileUri = data?.data
            // Now you have the URI of the selected file, you can proceed with encryption.
            // Example: val filePath = selectedFileUri?.path
        }
    }


}