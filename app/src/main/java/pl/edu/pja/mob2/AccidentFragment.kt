package pl.edu.pja.mob2

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.edu.pja.mob2.databinding.AccidentCardBinding
import pl.edu.pja.mob2.databinding.FragmentAccidentBinding
import pl.edu.pja.mob2.databinding.FragmentAddAccidentBinding
import java.io.File
import java.time.Instant
import java.util.*

class AccidentFragment : Fragment() {
    private val binding by lazy { FragmentAccidentBinding.inflate(layoutInflater) }
    private val database by lazy { Firebase.firestore.collection("accidents") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(Firebase.auth.currentUser == null) {
            NavHostFragment.findNavController(this)?.navigate(AccidentFragmentDirections.actionAccidentFragmentToAuthenticationFragment())
        }

        displayAccidentList()

        binding.accidentAddAccidentButton.setOnClickListener() {
            view?.findNavController()
                ?.navigate(AccidentFragmentDirections.actionAccidentFragmentToAddAccidentFragment())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        displayAccidentList()
    }

    private fun displayAccidentList() {
        database.orderBy("date", Query.Direction.DESCENDING).get().addOnSuccessListener {
            binding.accidentList.apply {
                adapter = AccidentAdapter(it.documents.map { x ->
                    Accident(
                        x.id,
                        x["photoUri"].toString(),
                        x["name"].toString(),
                        Date.from(Instant.parse(x["date"].toString())),
                        x["user"].toString(),
                        x["userName"].toString(),
                        x["place"].toString()
                    )
                },
                    object : OnAccidentClick {
                        override fun onClick(accidentsId: String) {
                            view?.findNavController()
                                ?.navigate(
                                    AccidentFragmentDirections.actionAccidentFragmentToAccidentDetailsFragment(
                                        accidentsId
                                    )
                                )
                        }

                    })
                layoutManager = LinearLayoutManager(requireContext())
            }
        }
    }
}

class AccidentAdapter(
    private val accidents: List<Accident>,
    private val callback: OnAccidentClick
) :
    RecyclerView.Adapter<AccidentAdapter.AccidentViewHolder>() {


    class AccidentViewHolder(
        private val binding: AccidentCardBinding,
        private val callback: OnAccidentClick
    ) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(accident: Accident) {
            binding.accidentCardDate.text = accident.date.toString()
            binding.accidentCardName.text = accident.name
            binding.accidentCardPlace.text = accident.place

            val localFile = File.createTempFile(UUID.randomUUID().toString(), ".jpg")
            if (accident.photoUri != "")
                Firebase.storage.reference.child(accident.photoUri).getFile(localFile)
                    .addOnSuccessListener {
                        val bitmapOptions = BitmapFactory.Options().apply {
                            inJustDecodeBounds = true
                        }
                        BitmapFactory.decodeFile(localFile.absolutePath, bitmapOptions)

                        bitmapOptions.apply {
                            val photoW: Int = outWidth
                            val photoHeight: Int = outHeight

                            val scaleFactor: Int = 1.coerceAtLeast(
                                (photoW / binding.accidentCardImage.width).coerceAtMost(photoHeight / binding.accidentCardImage.height)
                            )

                            inJustDecodeBounds = false
                            inSampleSize = scaleFactor
                        }

                        binding.accidentCardImage.setImageBitmap(
                            BitmapFactory.decodeFile(
                                localFile.absolutePath,
                                bitmapOptions
                            )
                        )

                        binding.root.setOnClickListener() {
                            callback.onClick(accident.id)
                        }
                    }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccidentViewHolder {
        val binding =
            AccidentCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AccidentViewHolder(binding, callback)
    }

    override fun getItemCount(): Int {
        return accidents.size
    }

    override fun onBindViewHolder(holder: AccidentViewHolder, position: Int) {
        holder.bind(accidents[position])
    }
}


data class Accident(
    var id: String = "",
    var photoUri: String = "",
    var name: String = "",
    var date: Date = Date.from(Instant.now()),
    var user: String = "",
    var userName: String = "",
    var place: String = ""
)

interface OnAccidentClick {
    fun onClick(accidentsId: String)
}