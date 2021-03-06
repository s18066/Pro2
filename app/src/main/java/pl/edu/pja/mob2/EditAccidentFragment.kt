package pl.edu.pja.mob2

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import pl.edu.pja.mob2.databinding.FragmentAccidentDetailsBinding
import pl.edu.pja.mob2.databinding.FragmentEditAccidentBinding
import java.io.File
import java.io.IOException
import java.time.Instant
import java.util.*

class EditAccidentFragment : Fragment() {
    private val binding by lazy { FragmentEditAccidentBinding.inflate(layoutInflater) }
    private val database by lazy { Firebase.firestore.collection("accidents") }
    private val storage by lazy { Firebase.storage.reference }

    private lateinit var marker: Marker
    private lateinit var map: GoogleMap
    private val geocoder by lazy { Geocoder(requireContext()) }


    private val args: EditAccidentFragmentArgs by navArgs()
    private lateinit var photo: File
    private val photoCaptureIntentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.editAccidentMap.onCreate(null)

        database.document(args.accidentId).get(Source.CACHE).addOnSuccessListener {
            binding.editAccidentDescription.setText(it["description"].toString())
            binding.editAccidentAccidentName.setText(it["name"].toString())

            photo = File(args.accidentPhoto)

            val bitmapOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(args.accidentPhoto, bitmapOptions)

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
                    args.accidentPhoto,
                    bitmapOptions
                )
            )

            val position = LatLng(it["lat"].toString().toDouble(), it["lng"].toString().toDouble())
            binding.editAccidentMap.getMapAsync { map ->
                marker = map.addMarker(MarkerOptions().position(position))
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 17f))
                map.setOnMapClickListener {
                    marker.position = it
                }
            }
        }

        binding.accidentPreview.setOnClickListener() {
            val photoCaptureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
                photo.also {
                    val uri = it.absoluteFile
                    photo = it
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                }
            }
            photoCaptureIntentLauncher.launch(photoCaptureIntent)
        }

        binding.editAccidentSubmitButton.setOnClickListener() {
            onEditButtonClick()
        }
    }

    override fun onStart() {
        super.onStart()
        binding.editAccidentMap.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.editAccidentMap.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.editAccidentMap.onPause()
    }

    override fun onStop() {
        super.onStop()
        binding.editAccidentMap.onStop()
    }

    private fun onEditButtonClick() {
        if(!(validateField(binding.editAccidentAccidentName) and validateField(binding.editAccidentDescription)))
            return

        val name = binding.editAccidentAccidentName.text
        val description = binding.editAccidentDescription.text

        val user = Firebase.auth.currentUser!!

        storage.child("photos/${photo.name}")
            .putFile(Uri.fromFile(photo))
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
                    "photoUri" to "photos/${photo.name}",
                    "userName" to user.displayName,
                    "date" to Instant.now().toString(),
                    "lat" to marker.position.latitude.toString(),
                    "lng" to marker.position.longitude.toString(),
                    "place" to geocoder.getFromLocation(
                        marker.position.latitude,
                        marker.position.longitude,
                        1
                    ).first().getAddressLine(0)
                )

                database.document(args.accidentId).set(accident)
                    .addOnSuccessListener {
                        Snackbar.make(requireView(), "Zaktualizowano", 5)
                        view?.findNavController()?.popBackStack()
                    }
            }.addOnFailureListener() {
                Snackbar.make(requireView(), "uploadFailed", 5)
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return binding.root
    }

    private fun validateField(toCheck: EditText): Boolean {
        if (toCheck.text.isEmpty()) {
            toCheck.error = "Musi by?? wype??ninone"
            return false
        }

        return true
    }
}