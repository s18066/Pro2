package pl.edu.pja.mob2

import android.graphics.BitmapFactory
import android.opengl.Visibility
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import pl.edu.pja.mob2.databinding.FragmentAccidentBinding
import pl.edu.pja.mob2.databinding.FragmentAccidentDetailsBinding
import java.time.Instant
import java.util.*

class AccidentDetailsFragment : Fragment() {
    private val binding by lazy { FragmentAccidentDetailsBinding.inflate(layoutInflater) }
    private val database by lazy { Firebase.firestore.collection("accidents") }
    private val args: AccidentDetailsFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        displayDetails()
    }

    override fun onResume() {
        super.onResume()
        displayDetails()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return binding.root
    }

    private fun displayDetails() {
        database.document(args.accidentId).get(Source.CACHE).addOnSuccessListener {
            binding.accidentDetailsDate.text = Date.from(Instant.parse(it["date"].toString())).toString()
            binding.accidentDetailsDescription.text = it["description"].toString()
            binding.accidentDetailsName.text = it["name"].toString()
            binding.accidentDetailsUserName.text = it["userName"].toString()

            val bitmapOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(args.accidentPhoto, bitmapOptions)

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
                    args.accidentPhoto,
                    bitmapOptions
                )
            )

            if(it["user"].toString() == Firebase.auth.uid) {
                binding.accidentsDetailsEdit.visibility = View.VISIBLE
                binding.accidentsDetailsEdit.setOnClickListener() {
                    view?.findNavController()
                        ?.navigate(AccidentDetailsFragmentDirections.actionAccidentDetailsFragmentToEditAccidentFragment(args.accidentId, args.accidentPhoto))
                }
            }
        }
    }
}