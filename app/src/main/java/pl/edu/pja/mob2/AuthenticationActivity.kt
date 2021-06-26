package pl.edu.pja.mob2

import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class AuthenticationActivity : AppCompatActivity(R.layout.authentication_fragment) {
    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest
    private lateinit var registerRequest: BeginSignInRequest
    private lateinit var oneTapLauncher: ActivityResultLauncher<IntentSenderRequest>

    private var oneTapDeclined = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        oneTapClient = Identity.getSignInClient(this)
        signInRequest = BeginSignInRequest.builder().setGoogleIdTokenRequestOptions(
            BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                .setSupported(true)
                .setServerClientId(getString(R.string.default_web_client_id))
                .setFilterByAuthorizedAccounts(true)
                .build()
        ).build()

        registerRequest = BeginSignInRequest.builder().setGoogleIdTokenRequestOptions(
            BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                .setSupported(true)
                .setServerClientId(getString(R.string.default_web_client_id))
                .setFilterByAuthorizedAccounts(false)
                .build()
        ).build()

        oneTapLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { activityResult ->
                try {
                    oneTapClient.getSignInCredentialFromIntent(activityResult.data).googleIdToken?.let {
                        val credentials = GoogleAuthProvider.getCredential(it, null)
                        FirebaseAuth.getInstance().signInWithCredential(credentials)
                            .addOnSuccessListener { finish() }
                            .addOnFailureListener { Log.wtf("asdfasdf", "asdfasdfasdf") }
                    }

                } catch (exception: ApiException) {
                    when (exception.statusCode) {
                        CommonStatusCodes.CANCELED -> {
                            oneTapDeclined = true
                        }

                        CommonStatusCodes.NETWORK_ERROR -> {
                            //smth
                        }
                    }
                }
            }
    }

    override fun onStart() {
        super.onStart()

        if (!oneTapDeclined) {
            oneTapClient.beginSignIn(signInRequest).addOnSuccessListener { result ->
                    oneTapLauncher.launch(
                        IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                    )
            }.addOnFailureListener { failure ->

                oneTapClient.beginSignIn(registerRequest).addOnSuccessListener { result ->
                    oneTapLauncher.launch(
                        IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                    )
                }.addOnFailureListener { it ->
                    failure.toString()
                    Log.println(Log.INFO, "asdf", failure.toString())
                }

            }
        }
    }
}



