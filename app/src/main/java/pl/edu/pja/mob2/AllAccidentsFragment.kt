package pl.edu.pja.mob2

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.location.Criteria
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class AllAccidentsFragment : Fragment() {
    private val database by lazy { Firebase.firestore.collection("accidents") }
    private val locationManager by lazy { requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager }

    @SuppressLint("MissingPermission")
    private val callback = OnMapReadyCallback { googleMap ->
        locationManager.getBestProvider(Criteria(), false)?.let {
            locationManager.getLastKnownLocation(it)?.let {
                googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(
                            it.latitude,
                            it.longitude
                        ), 13f
                    )
                )

                val cameraPosition = CameraPosition.Builder()
                    .target(LatLng(it.latitude, it.longitude))
                    .zoom(17f)
                    .build()

                googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            }
        }

        googleMap.isMyLocationEnabled = true

        placePins(googleMap)

        googleMap.setOnCircleClickListener {
            view?.findNavController()
                ?.navigate(AllAccidentsFragmentDirections.actionAllAccidentsFragmentToAccidentDetailsFragment(it.tag.toString()))
        }

        googleMap.setOnMarkerClickListener {
            view?.findNavController()
                ?.navigate(AllAccidentsFragmentDirections.actionAllAccidentsFragmentToAccidentDetailsFragment(it.tag.toString()))
            return@setOnMarkerClickListener true;
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_all_accidents, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }

    private fun placePins(map: GoogleMap) {
        database.get().addOnSuccessListener {
            it.documents.map { x ->
                val position = LatLng(x["lat"].toString().toDouble(), x["lng"].toString().toDouble())
                val circleOptions = CircleOptions().radius(1000.0).center(position).clickable(true)
                        .strokeColor(Color.TRANSPARENT)
                        .fillColor(0x220000FF)
                        .strokeWidth(5f)
                map.addCircle(circleOptions).tag = x.id

                val markerOptions = MarkerOptions().position(position).title(x["name"].toString())
                map.addMarker(markerOptions).tag = x.id

            }
        }
    }
}