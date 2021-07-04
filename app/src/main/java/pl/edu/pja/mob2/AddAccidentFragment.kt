package pl.edu.pja.mob2

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Criteria
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.navigation.findNavController
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.service.Common
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import pl.edu.pja.mob2.databinding.FragmentAddAccidentBinding
import java.io.File
import java.io.IOException
import java.time.Instant
import java.util.*
import kotlin.jvm.Throws

class AddAccidentFragment : Fragment() {
    private val binding by lazy { FragmentAddAccidentBinding.inflate(layoutInflater) }
    private val storage by lazy { Firebase.storage.reference }
    private val database by lazy { Firebase.firestore }
    private lateinit var photo: File

    private lateinit var marker: Marker
    private val locationManager by lazy { requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    private val geocoder by lazy { Geocoder(requireContext()) }

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

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.accidentPreview.setOnClickListener() { onPhotoButtonClick() }
        binding.addAccidentSubmitButton.setOnClickListener() { onSaveButtonClick() }
        binding.progressBar.visibility = View.INVISIBLE

        binding.addAccidentMap.onCreate(null)
        binding.addAccidentMap.getMapAsync { map ->
            map.isMyLocationEnabled = true

            locationManager.getBestProvider(Criteria(), false)?.let {
                locationManager.getLastKnownLocation(it)?.let {
                    val position = LatLng(it.latitude, it.longitude)

                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 17f))

                    val markerOptions = MarkerOptions().let {
                        it.visible(true)
                        it.position(position)
                    }
                    marker = map.addMarker(markerOptions)
                }

                map.setOnMapClickListener {
                    marker.position = it
                    marker.isVisible = true
                }
            }
        }

    }

    override fun onStart() {
        super.onStart()
        binding.addAccidentMap.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.addAccidentMap.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.addAccidentMap.onPause()
    }

    override fun onStop() {
        super.onStop()
        binding.addAccidentMap.onStop()
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
        if (!(validateField(binding.addAccidentAccidentName) and validateField(binding.addAccidentDescription) and validatePhoto()))
            return

        val name = binding.addAccidentAccidentName.text
        val description = binding.addAccidentDescription.text

        val user = Firebase.auth.currentUser!!

        storage.child("photos/${photo.name}")
            .putFile(FileProvider.getUriForFile(requireContext(), "pl.edu.pja.mob2", photo))
            .addOnProgressListener {
                binding.progressBar.visibility = View.VISIBLE
                binding.progressBar.min = 0
                binding.progressBar.max = 100
                binding.progressBar.progress =
                    ((binding.progressBar.max * it.bytesTransferred) / it.totalByteCount).toInt()
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

    private fun validateField(toCheck: EditText): Boolean {
        if (toCheck.text.isEmpty()) {
            toCheck.error = "Musi być wypełninone"
            return false
        }

        return true
    }

    private fun validatePhoto(): Boolean {
        if (!this::photo.isInitialized) {
            binding.addAccidentNoPhoto.visibility = View.VISIBLE
            return false
        }
        binding.addAccidentNoPhoto.visibility = View.INVISIBLE
        return true
    }
}