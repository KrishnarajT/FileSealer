package com.krishnaraj.filesealer.ui.decrypt

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textview.MaterialTextView
import com.krishnaraj.filesealer.R
import com.krishnaraj.filesealer.databinding.FragmentDecryptBinding
import java.io.File
import java.security.Key
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import kotlin.system.exitProcess

private const val TRANSFORMATION = "AES"
private const val KEY_SIZE_BITS = 256

class DecryptFragment : Fragment() {

    private lateinit var keyTextBox: EditText
    private lateinit var openFileButton: Button
    private lateinit var decryptButton: Button

    private lateinit var decryptionKey: String
    private lateinit var fileUri: Uri

    private lateinit var decryptedFileContents: String

    private val PICK_FILE_REQUEST_CODE = 1
    private val REQUEST_PERMISSIONS_CODE = 123


    private var _binding: FragmentDecryptBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val galleryViewModel =
            ViewModelProvider(this).get(DecryptViewModel::class.java)

        _binding = FragmentDecryptBinding.inflate(inflater, container, false)
        val root: View = binding.root

        openFileButton = root.findViewById(R.id.open_file_btn)
        keyTextBox = root.findViewById(R.id.enter_key_txt_box)

        openFileButton.setOnClickListener {
            openFileExplorer()
        }

        decryptButton = root.findViewById(R.id.decrypt_btn)
        decryptButton.setOnClickListener {
            if (checkPermissions()) {
                decryptFile()
            } else {
                requestPermissions()
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun openFileExplorer() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "text/*" // Allow only text files
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(intent, PICK_FILE_REQUEST_CODE)
    }

    @SuppressLint("Recycle")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            val selectedFileUri = data?.data

            if (selectedFileUri != null) {
                val filePath = selectedFileUri.path

                if (filePath != null) {
                    fileUri = selectedFileUri
                } else {
                    val message: CharSequence = "File not found in file path. Please try again."
                    val duration = Toast.LENGTH_SHORT
                    val toast = Toast.makeText(context, message, duration)
                    toast.show()
                }
            } else {
                val message: CharSequence = "File not selected"
                val duration = Toast.LENGTH_SHORT
                val toast = Toast.makeText(context, message, duration)
                toast.show()
            }
        }
    }

    @SuppressLint("GetInstance")
    fun decrypt(encryptedFileContents: String?, encryptionKey: String): String {
        Log.d("DecryptFragment", "Decryption Key: $encryptionKey")
        Log.d("DecryptFragment", "File Contents: $encryptedFileContents")

        try {
            // Generate a secret key based on the provided encryptionKey
            val secretKey: Key = generateKey(encryptionKey)

            // Create a Cipher object for AES decryption
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey)

            // Decode the Base64-encoded encrypted string
            val encryptedBytes = Base64.decode(encryptedFileContents, Base64.DEFAULT)

            // Decrypt the bytes
            val decryptedBytes = cipher.doFinal(encryptedBytes)

            // Convert the decrypted bytes to a String
            return Base64.encodeToString(decryptedBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle decryption errors here
            Log.e("DecryptFragment", "Error during Decryption function", e)
            return ""
        }
    }

    private fun generateKey(decryptionKey: String): SecretKey {
        val keyBytes = decryptionKey.toByteArray(Charsets.UTF_8)

        if (keyBytes.size == KEY_SIZE_BITS / 8) {
            return SecretKeySpec(keyBytes, TRANSFORMATION)
        }

        val keyGenerator = KeyGenerator.getInstance("HmacSHA256")
        keyGenerator.init(KEY_SIZE_BITS)

        val secretKey = keyGenerator.generateKey()

        return SecretKeySpec(secretKey.encoded, TRANSFORMATION)
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_PERMISSIONS_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    decryptButton.setOnClickListener {
                        decryptFile()
                    }
                } else {
                    Log.d("DecryptFragment", "Permissions denied in request permissions")
                    Toast.makeText(
                        this.requireContext(),
                        "The app won't function properly without the required permissions.",
                        Toast.LENGTH_LONG
                    ).show()
                    requestPermissions()
                    exitProcess(0)
                }
            }
        }
    }

    @SuppressLint("Recycle")
    private fun decryptFile() {
        decryptionKey = keyTextBox.text.toString()

        if (!this::fileUri.isInitialized) {
            val message: CharSequence = "No file selected. Please select a file."
            val duration = Toast.LENGTH_SHORT
            val toast = Toast.makeText(context, message, duration)
            toast.show()
            return
        }

        val fileContents = context?.contentResolver?.openInputStream(fileUri)?.bufferedReader()
            .use { it?.readText() }

        if (fileContents != null) {
            try {
                decryptedFileContents = decrypt(fileContents, decryptionKey)

                val contentResolver = this.requireContext().contentResolver
                val uri = fileUri

                val fileName = getFileName(contentResolver, uri)

                if (fileName != null) {
                    Log.d("FileName", fileName)
                } else {
                    Log.e("FileName", "Unable to retrieve filename")
                    return
                }

                val decryptedFileName =
                    fileName.substringBeforeLast(".") + "_decrypted." + fileName.substringAfterLast(
                        "."
                    )

                Log.d("DecryptFragment", "Decrypted File Name: $decryptedFileName")

                if (checkPermissions()) {
                    val decryptedFile = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        decryptedFileName
                    )
                    decryptedFile.writeText(decryptedFileContents)

                    Log.d("DecryptFragment", "written file")
                    val decryptedFileLocation = decryptedFile.absolutePath

                    val message: CharSequence =
                        "File has been Decrypted at: \nFile Location: $decryptedFileLocation"

                    val duration = Toast.LENGTH_SHORT

                    val toast = Toast.makeText(context, message, duration)
                    toast.show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("DecryptFragment", "Error during Decryption function", e)
            }
        } else {
            val message: CharSequence = "No text in the file. Please try again."
            val duration = Toast.LENGTH_SHORT
            val toast = Toast.makeText(context, message, duration)
            toast.show()
        }
    }

    private fun checkPermissions(): Boolean {
        return (ContextCompat.checkSelfPermission(
            this.requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            this.requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this.requireActivity(), arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ), REQUEST_PERMISSIONS_CODE
        )
    }

    private fun getFileName(contentResolver: ContentResolver, uri: Uri): String? {
        var fileName: String? = null
        val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            it.moveToFirst()
            val displayNameIndex: Int = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (displayNameIndex != -1) {
                fileName = it.getString(displayNameIndex)
            }
        }
        return fileName
    }
}

