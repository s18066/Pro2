package pl.edu.pja.mob2

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.navigation.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import pl.edu.pja.mob2.databinding.FragmentAddAccidentBinding
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.jvm.Throws

class AddAccidentFragment : Fragment() {
    private val binding by lazy { FragmentAddAccidentBinding.inflate(layoutInflater) }
    private val storage by lazy { Firebase.storage.reference }
    private val database by lazy { Firebase.firestore }
    private lateinit var photo: File
    private val photoCaptureIntentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val bitmapOptions = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }

                BitmapFactory.decodeFile(photo.absolutePath, bitmapOptions)

                bitmapOptions.apply {
                    val photoW: Int = outWidth
                    val photoHeight: Int = outHeight

                    val scaleFactor: Int = 1.coerceAtLeast(
                        (photoW / binding.accidentPreview.width).coerceAtMost(photoHeight / binding.accidentPreview.height)
                    )

                    inJustDecodeBounds = false
                    inSampleSize = scaleFactor
                }

                binding.accidentPreview.setImageBitmap(
                    BitmapFactory.decodeFile(
                        photo.absolutePath,
                        bitmapOptions
                    )
                )
            }
        }

    private val uploadPhotoIntentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {

        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.accidentPreview.setOnClickListener() { onPhotoButtonClick() }
        binding.addAccidentSubmitButton.setOnClickListener() { onSaveButtonClick() }
        binding.progressBar.visibility = View.INVISIBLE
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return binding.root
    }

    private fun onPhotoButtonClick() {
        val photoCaptureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
            val file: File? = try {
                createImageFile()
            } catch (exception: IOException) {
                // ERROR
                null
            }

            file?.also {
                val uri = FileProvider.getUriForFile(requireContext(), "pl.edu.pja.mob2", it)
                photo = it
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
            }
        }
        photoCaptureIntentLauncher.launch(photoCaptureIntent)
    }

    private fun onSaveButtonClick() {
        val name = binding.addAccidentAccidentName.text
        val description = binding.addAccidentDescription.text

        val user = Firebase.auth.currentUser!!

        storage.child("photos/${photo.name}")
            .putFile(FileProvider.getUriForFile(requireContext(), "pl.edu.pja.mob2", photo))
            .addOnProgressListener {
                binding.progressBar.visibility = View.VISIBLE
                binding.progressBar.min = 0
                binding.progressBar.max = 100
                binding.progressBar.progress = ((binding.progressBar.max * it.bytesTransferred) / it.totalByteCount).toInt()
            }
            .addOnCompleteListener {
                val accident = hashMapOf(
                    "description" to description.toString(),
                    "name" to name.toString(),
                    "user" to user.uid,
                    "photoUri" to "photos/${photo.name}"
                )

                database.collection("accidents").add(accident)
                    .addOnSuccessListener {
                        Snackbar.make(requireView(), "Dodano zgłoszenie", 5)
                        view?.findNavController()?.popBackStack()
                    }
            }.addOnFailureListener() {
                Snackbar.make(requireView(), "uploadFailed", 5)
            }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(UUID.randomUUID().toString(), ".jpg", storageDir)
    }
}