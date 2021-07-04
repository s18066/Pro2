package pl.edu.pja.mob2

import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Criteria
import android.opengl.Visibility
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import pl.edu.pja.mob2.databinding.FragmentAccidentBinding
import pl.edu.pja.mob2.databinding.FragmentAccidentDetailsBinding
import java.io.File
import java.time.Instant
import java.util.*

class AccidentDetailsFragment : Fragment() {
    private val binding by lazy { FragmentAccidentDetailsBinding.inflate(layoutInflater) }
    private val database by lazy { Firebase.firestore.collection("accidents") }
    private val args: AccidentDetailsFragmentArgs by navArgs()
    private lateinit var marker: Marker
    private lateinit var map: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.accidentDetailsLocation.onCreate(null)
        binding.accidentDetailsLocation.getMapAsync { map ->
                marker = map.addMarker(MarkerOptions().position(LatLng(0.0,0.0)).visible(false))
                this.map = map
            }
        }


    override fun onResume() {
        super.onResume()
        displayDetails()
        binding.accidentDetailsLocation.onResume()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        binding.accidentDetailsLocation.onStart()
    }

    override fun onPause() {
        super.onPause()
        binding.accidentDetailsLocation.onPause()
    }

    override fun onStop() {
        super.onStop()
        binding.accidentDetailsLocation.onStop()
    }

    private fun displayDetails() {
        database.document(args.accidentId).get(Source.CACHE).addOnSuccessListener {
            binding.accidentDetailsDate.text = Date.from(Instant.parse(it["date"].toString())).toString()
            binding.accidentDetailsDescription.text = it["description"].toString()
            binding.accidentDetailsName.text = it["name"].toString()
            binding.accidentDetailsUserName.text = it["userName"].toString()

            val position = LatLng(it["lat"].toString().toDouble(), it["lng"].toString().toDouble())
            marker.position = position
            marker.isVisible = true

            val circleOptions = CircleOptions().radius(1000.0).center(position).clickable(true)
                .strokeColor(Color.TRANSPARENT)
                .fillColor(0x220000FF)
                .strokeWidth(5f)

            map.addCircle(circleOptions)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 17f))

            val bitmapOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            val localFile = File.createTempFile(UUID.randomUUID().toString(), ".jpg")
            Firebase.storage.reference.child(it["photoUri"].toString()).getFile(localFile).addOnSuccessListener {
                BitmapFactory.decodeFile(localFile.absolutePath, bitmapOptions)

                bitmapOptions.apply {
                    val photoW: Int = outWidth
                    val photoHeight: Int = outHeight

                    val scaleFactor: Int = 1.coerceAtLeast(
                        (photoW / binding.accidentDetailsImage.width).coerceAtMost(photoHeight / binding.accidentDetailsImage.height)
                    )

                    inJustDecodeBounds = false
                    inSampleSize = scaleFactor
                }

                binding.accidentDetailsImage.setImageBitmap(
                    BitmapFactory.decodeFile(
                        localFile.absolutePath,
                        bitmapOptions
                    )
                )

            }

            if(it["user"].toString() == Firebase.auth.uid) {
                binding.accidentsDetailsEdit.visibility = View.VISIBLE
                binding.accidentsDetailsEdit.setOnClickListener() {
                    view?.findNavController()
                        ?.navigate(AccidentDetailsFragmentDirections.actionAccidentDetailsFragmentToEditAccidentFragment(args.accidentId, localFile.absolutePath))
                }
            }
            else
            {
                binding.accidentsDetailsEdit.visibility = View.INVISIBLE
            }
        }
    }
}