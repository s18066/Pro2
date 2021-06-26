package pl.edu.pja.mob2

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity(R.layout.activity_main) {
    private var authenticationIntentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                // Go to app
            }
        }


    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)


    }

    override fun onStart() {
        super.onStart()

        Firebase.auth.signOut()

        if (Firebase.auth.currentUser == null) {
            val authenticationIntent = Intent(this, AuthenticationActivity::class.java)
            authenticationIntentLauncher.launch(authenticationIntent)
        }

//        supportFragmentManager.commit {
//            setReorderingAllowed(true)
//            add<AuthenticationActivity>(R.id.fragment_container_view)
    }
}