package com.krishnaraj.filesealer.ui.encrypt

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
import com.google.android.material.textview.MaterialTextView
import com.krishnaraj.filesealer.R
import com.krishnaraj.filesealer.databinding.FragmentEncryptBinding
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.io.UnsupportedEncodingException
import java.security.InvalidKeyException
import java.security.Key
import java.security.NoSuchAlgorithmException
import java.security.Security
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.ShortBufferException
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.system.exitProcess


private const val TRANSFORMATION = "AES"
private const val KEY_SIZE_BITS = 256

class EncryptFragment : Fragment() {

    private var _binding: FragmentEncryptBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    // declare textbox
    private lateinit var keyTextBox: EditText

    // declare button
    private lateinit var openFileButton: Button

    // declare encryption key variable
    private lateinit var encryptionKey: String

    // declare button for encrypting
    private lateinit var encryptButton: Button

    // declare variable for storing uri of file
    private lateinit var fileUri: Uri

    // declare a string for holding encrypted file contents
    private lateinit var encryptedFileContents: String

    private val PICK_FILE_REQUEST_CODE = 1
    private val REQUEST_PERMISSIONS_CODE = 123

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        _binding = FragmentEncryptBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // get the open file button.
        openFileButton = root.findViewById(R.id.open_file_btn)
        keyTextBox = root.findViewById(R.id.enter_key_txt_box)
        openFileButton.setOnClickListener {
            openFileExplorer()
        }
        encryptButton = root.findViewById(R.id.encrypt_btn)
        encryptButton.setOnClickListener {
            if (checkPermissions()) {
                encryptFile()
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

            // Ensure that selectedFileUri is not null before proceeding
            if (selectedFileUri != null) {
                val filePath = selectedFileUri.path

                // Ensure that filePath is not null before proceeding
                if (filePath != null) {
                    fileUri = selectedFileUri

                } else {
                    // Handle the case where filePath is null
                    // Display an appropriate message or log an error
                    val message: CharSequence = "File not found in file path. Please try again."

                    // Set a duration of 2 seconds
                    val duration = Toast.LENGTH_SHORT

                    val toast = Toast.makeText(context, message, duration)
                    toast.show()
                }

            } else {
                // Handle the case where selectedFileUri is null
                // Display an appropriate message or log an error
                // show a toast
                val message: CharSequence = "File not selected"

                // Set a duration of 2 seconds
                val duration = Toast.LENGTH_SHORT

                val toast = Toast.makeText(context, message, duration)
                toast.show()
            }
        }
    }

    fun encrypt_bouncyCastle(strToEncrypt: String, secret_key: String): String {
        Security.addProvider(BouncyCastleProvider())
        var keyBytes: ByteArray
        Log.d("EncryptFragment", strToEncrypt)
        Log.d("EncryptFragment", secret_key)
        try {
            keyBytes = secret_key.toByteArray(charset("UTF8"))
            val skey = SecretKeySpec(keyBytes, "AES")
            val input = strToEncrypt.toByteArray(charset("UTF8"))

            synchronized(Cipher::class.java) {
                val cipher = Cipher.getInstance("AES/ECB/PKCS7Padding")
                cipher.init(Cipher.ENCRYPT_MODE, skey)

                val cipherText = ByteArray(cipher.getOutputSize(input.size))
                var ctLength = cipher.update(
                    input, 0, input.size,
                    cipherText, 0
                )
                ctLength += cipher.doFinal(cipherText, ctLength)
                Log.d("EncryptFragment", "ctLength: $ctLength")
                Log.d("EncryptFragment", "cipherText: $cipherText")
                return Base64.encodeToString(cipherText, Base64.DEFAULT)
            }
        } catch (uee: UnsupportedEncodingException) {
            uee.printStackTrace()
            Log.d("EncryptedFragment", "UnsupportedEncodingException: ${uee.message}")
        } catch (ibse: IllegalBlockSizeException) {
            ibse.printStackTrace()
            Log.d("EncryptedFragment", "IllegalBlockSizeException: ${ibse.message}")
        } catch (bpe: BadPaddingException) {
            bpe.printStackTrace()
            Log.d("EncryptedFragment", "BadPaddingException: ${bpe.message}")
        } catch (ike: InvalidKeyException) {
            ike.printStackTrace()
            Log.d("EncryptedFragment", "InvalidKeyException: ${ike.message}")
        } catch (nspe: NoSuchPaddingException) {
            nspe.printStackTrace()
            Log.d("EncryptedFragment", "NoSuchPaddingException: ${nspe.message}")
        } catch (nsae: NoSuchAlgorithmException) {
            nsae.printStackTrace()
            Log.d("EncryptedFragment", "NoSuchAlgorithmException: ${nsae.message}")
        } catch (e: ShortBufferException) {
            e.printStackTrace()
            Log.d("EncryptedFragment", "ShortBufferException: ${e.message}")
        }
        return ""
    }

    @SuppressLint("GetInstance")
    fun encrypt(fileContents: String?, encryptionKey: String): String {
        try {
            // Generate a secret key based on the provided encryptionKey
            val secretKey: Key = generateKey(encryptionKey)

            // Log this
            Log.d("EncryptFragment",  "Secret Key: ${secretKey.hashCode()}")

            // Create a Cipher object for AES encryption
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            // Encrypt the fileContents
            val encryptedBytes = cipher.doFinal(fileContents?.toByteArray(Charsets.UTF_8))
            val encryptedString = Base64.encodeToString(encryptedBytes, Base64.DEFAULT)

            // print the encrypted string
            Log.d("EncryptFragment", "Encrypted String: $encryptedString")

            // Convert the encrypted bytes to a Base64-encoded string
            return encryptedString
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle encryption errors here
            // log the error using log.e
            Log.e("EncryptFragment", "Error during Encryption function", e)
            return ""
        }
    }
    private val salt: ByteArray = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08)
    private val iterationCount = 10000

    private fun generateKey(secretKey: String): SecretKey {
        val keySpec = SecretKeySpec(secretKey.toByteArray(Charsets.UTF_8), "AES")
        return keySpec
    }
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_PERMISSIONS_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permissions granted, you can perform your file-related operations here.
                    encryptButton.setOnClickListener {
                        encryptFile()
                    }
                } else {
                    // Permissions denied, handle accordingly.
                    // You might want to inform the user that the app won't function properly without the required permissions.
                    Log.d("EncryptFragment", "Permissions denied in requst permissions")
                    Toast.makeText(
                        this.requireContext(),
                        "The app won't function properly without the required permissions.",
                        Toast.LENGTH_LONG
                    ).show()
                    // Request permissions again
                    requestPermissions()

                    exitProcess(0)
                }
            }
        }
    }

    @SuppressLint("Recycle")
    private fun encryptFile() {

        // Get the key from the key textbox
        encryptionKey = keyTextBox.text.toString()

        // handle if there is no file selected
        if (!this::fileUri.isInitialized) {
            val message: CharSequence = "No file selected. Please select a file."

            // Set a duration of 2 seconds
            val duration = Toast.LENGTH_SHORT

            val toast = Toast.makeText(context, message, duration)
            toast.show()
            return
        }

        // Get the contents of the text file
        val fileContents = context?.contentResolver?.openInputStream(fileUri)?.bufferedReader()
            .use { it?.readText() }

        // Ensure that fileContents is not null before proceeding
        if (fileContents != null) {
            try {
                // Encrypt the file contents
                encryptedFileContents = encrypt_bouncyCastle(fileContents, encryptionKey)

                val contentResolver = this.requireContext().contentResolver
                val uri = fileUri

                val fileName = getFileName(contentResolver, uri)

                if (fileName != null) {
                    // Use the filename as needed
                    Log.d("FileName", fileName)
                } else {
                    // Handle the case where the filename couldn't be retrieved
                    Log.e("FileName", "Unable to retrieve filename")
                    return
                }

                // add _encrypted to the filename to make the encrypted file name, add it only to the name, and not extention
                val encryptedFileName =
                    fileName.substringBeforeLast(".") + "_encrypted." + fileName.substringAfterLast(
                        "."
                    )
                Log.d("EncryptFragment", "Encrypted File Name: $encryptedFileName")

                if (checkPermissions()) {
//                    encryptedFile.writeText(encryptedFileContents)
                    Log.d("EncryptFragment", "Permissions granted in encrypt file")
                    val encryptedFile = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        encryptedFileName
                    )
                    encryptedFile.writeText(encryptedFileContents)
                    // log this
                    Log.d("EncryptFragment", "written file")
                    // Get location of newly written encrypted file
                    val encryptedFileLocation = encryptedFile.absolutePath

                    // Inform the user that encryption is complete using a toast
                    val message: CharSequence =
                        "File has been Encrypted at: \nFile Location: $encryptedFileLocation"

                    // Set a duration of 2 seconds
                    val duration = Toast.LENGTH_SHORT

                    val toast = Toast.makeText(context, message, duration)
                    toast.show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                // Handle encryption errors here
                // log the error using log.e
                Log.e("EncryptFragment", "Error during Encryption function", e)
            }
        } else {
            // Handle the case where fileContents is null
            // Display an appropriate message or log an error
            val message: CharSequence = "No text in the file. Please try again."

            // Set a duration of 2 seconds
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