package com.krishnaraj.filesealer.ui.text

import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.krishnaraj.filesealer.R
import com.krishnaraj.filesealer.databinding.FragmentEncDecTextBinding
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.UnsupportedEncodingException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.Security
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import javax.crypto.ShortBufferException
import javax.crypto.spec.SecretKeySpec

class TextFragment : Fragment() {

    private var _binding: FragmentEncDecTextBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var encryption_text_box: EditText
    private lateinit var output_text_box: EditText
    private lateinit var key_text_box: EditText
    private lateinit var encbtn: Button
    private lateinit var decbtn: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val slideshowViewModel =
            ViewModelProvider(this).get(TextViewModel::class.java)

        _binding = FragmentEncDecTextBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // start here
        encryption_text_box = root.findViewById(R.id.encryption_text_box)
        output_text_box = root.findViewById(R.id.output_text_box)
        key_text_box = root.findViewById(R.id.enter_key_here)
        encbtn = root.findViewById(R.id.encbtn)
        decbtn = root.findViewById(R.id.decbtn)

        // when the encrypt button is clicked
        encbtn.setOnClickListener {
            // get the text from the encryption text box
            val text = encryption_text_box.text.toString()
            // get the key from the key text box
            val key = key_text_box.text.toString()
            // encrypt the text with the key
            val encryptedText = encrypt_bouncyCastle(text, key)
            // set the output text box to the encrypted text
            output_text_box.setText(encryptedText)
        }

        // when the decrypt button is clicked
        decbtn.setOnClickListener {
            // get the text from the output text box
            val text = encryption_text_box.text.toString()
            // get the key from the key text box
            val key = key_text_box.text.toString()
            // decrypt the text with the key
            val decryptedText = decryptWithAES(key, text)
            // set the encryption text box to the decrypted text
            output_text_box.setText(decryptedText)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun encrypt_bouncyCastle(strToEncrypt: String, secret_key: String): String {

        var encryptionKey = secret_key
        // if the key is empty then return and show toast
        if (encryptionKey.isEmpty()) {
            val message: CharSequence = "Please enter a key."

            // Set a duration of 2 seconds
            val duration = Toast.LENGTH_SHORT

            val toast = Toast.makeText(context, message, duration)
            toast.show()
            return ""
        }
        // if the encryption key isnt 32 characters long, then turn it into one by repeating the key until it is
        if (encryptionKey.length < 32) {
            // get the length of the key
            val keyLength = encryptionKey.length
            // get the number of times the key needs to be repeated
            val repeatKey = 32 / keyLength
            // repeat the key
            encryptionKey = encryptionKey.repeat(repeatKey)
            // get the length of the new key
            val newKeyLength = encryptionKey.length
            // get the number of characters that need to be added to the key
            val addKey = 32 - newKeyLength
            // add the characters to the key
            encryptionKey += encryptionKey.substring(0, addKey)
        }
        // print the new one
        Log.d("TextFragment", "Encryption Key: $encryptionKey")


        Security.addProvider(BouncyCastleProvider())
        var keyBytes: ByteArray
        Log.d("TextFragment", strToEncrypt)
        Log.d("TextFragment", encryptionKey)
        try {
            keyBytes = encryptionKey.toByteArray(charset("UTF8"))
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
                Log.d("TextFragment", "ctLength: $ctLength")
                Log.d("TextFragment", "cipherText: $cipherText")
                return Base64.encodeToString(cipherText, Base64.DEFAULT)
            }
        } catch (uee: UnsupportedEncodingException) {
            uee.printStackTrace()
            Log.d("TextFragment", "UnsupportedEncodingException: ${uee.message}")
        } catch (ibse: IllegalBlockSizeException) {
            ibse.printStackTrace()
            Log.d("TextFragment", "IllegalBlockSizeException: ${ibse.message}")
        } catch (bpe: BadPaddingException) {
            bpe.printStackTrace()
            Log.d("TextFragment", "BadPaddingException: ${bpe.message}")
        } catch (ike: InvalidKeyException) {
            ike.printStackTrace()
            Log.d("TextFragment", "InvalidKeyException: ${ike.message}")
        } catch (nspe: NoSuchPaddingException) {
            nspe.printStackTrace()
            Log.d("TextFragment", "NoSuchPaddingException: ${nspe.message}")
        } catch (nsae: NoSuchAlgorithmException) {
            nsae.printStackTrace()
            Log.d("TextFragment", "NoSuchAlgorithmException: ${nsae.message}")
        } catch (e: ShortBufferException) {
            e.printStackTrace()
            Log.d("TextFragment", "ShortBufferException: ${e.message}")
        } catch(e: Exception) {
            e.printStackTrace()
            Log.d("TextFragment", "Exception: ${e.message}")

            // show a toast
            Toast.makeText(this.requireContext(), "Error: Unable to Encode this Text", Toast.LENGTH_LONG).show()
        }
        return ""
    }

    private fun decryptWithAES(key: String, strToDecrypt: String?): String {
        Security.addProvider(BouncyCastleProvider())
        var keyBytes: ByteArray


        var encryptionKey = key

        // if the key is empty then return and show toast
        if (encryptionKey.isEmpty()) {
            val message: CharSequence = "Please enter a key."

            // Set a duration of 2 seconds
            val duration = Toast.LENGTH_SHORT

            val toast = Toast.makeText(context, message, duration)
            toast.show()
            return ""
        }

        // if the encryption key isnt 32 characters long, then turn it into one by repeating the key until it is
        if (encryptionKey.length < 32) {
            // get the length of the key
            val keyLength = encryptionKey.length
            // get the number of times the key needs to be repeated
            val repeatKey = 32 / keyLength
            // repeat the key
            encryptionKey = encryptionKey.repeat(repeatKey)
            // get the length of the new key
            val newKeyLength = encryptionKey.length
            // get the number of characters that need to be added to the key
            val addKey = 32 - newKeyLength
            // add the characters to the key
            encryptionKey += encryptionKey.substring(0, addKey)
        }
        // print the new one
        Log.d("TextFragment", "Encryption Key: $encryptionKey")



        try {
            keyBytes = encryptionKey.toByteArray(charset("UTF8"))
            val skey = SecretKeySpec(keyBytes, "AES")
            val input = org.bouncycastle.util.encoders.Base64
                .decode(strToDecrypt?.trim { it <= ' ' }?.toByteArray(charset("UTF8")))

            synchronized(Cipher::class.java) {
                val cipher = Cipher.getInstance("AES/ECB/PKCS7Padding")
                cipher.init(Cipher.DECRYPT_MODE, skey)

                val plainText = ByteArray(cipher.getOutputSize(input.size))
                var ptLength = cipher.update(input, 0, input.size, plainText, 0)
                ptLength += cipher.doFinal(plainText, ptLength)
                val decryptedString = String(plainText)
                return decryptedString.trim { it <= ' ' }
            }
        } catch (uee: UnsupportedEncodingException) {
            uee.printStackTrace()
            Log.d("TextFragment", "UnsupportedEncodingException: ${uee.message}")
        } catch (ibse: IllegalBlockSizeException) {
            ibse.printStackTrace()
            Log.d("TextFragment", "IllegalBlockSizeException: ${ibse.message}")
        } catch (bpe: BadPaddingException) {
            bpe.printStackTrace()
            Log.d("TextFragment", "BadPaddingException: ${bpe.message}")
        } catch (ike: InvalidKeyException) {
            ike.printStackTrace()
            Log.d("TextFragment", "InvalidKeyException: ${ike.message}")
        } catch (nspe: NoSuchPaddingException) {
            nspe.printStackTrace()
            Log.d("TextFragment", "NoSuchPaddingException: ${nspe.message}")
        } catch (nsae: NoSuchAlgorithmException) {
            nsae.printStackTrace()
            Log.d("TextFragment", "NoSuchAlgorithmException: ${nsae.message}")
        } catch (e: ShortBufferException) {
            e.printStackTrace()
            Log.d("TextFragment", "ShortBufferException: ${e.message}")
        } catch(e: Exception) {
            e.printStackTrace()
            Log.d("TextFragment", "Exception: ${e.message}")

            // show a toast
             Toast.makeText(this.requireContext(), "Error: Unable to Decode this Text", Toast.LENGTH_LONG).show()
        }

        return ""
    }


}