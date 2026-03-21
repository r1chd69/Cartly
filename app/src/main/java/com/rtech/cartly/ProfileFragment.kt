package com.rtech.cartly

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val db = FirebaseFirestore.getInstance()

    private lateinit var signedOutLayout: LinearLayout
    private lateinit var signedInLayout: LinearLayout
    private lateinit var profileName: TextView
    private lateinit var profileEmail: TextView
    private lateinit var basketCount: TextView
    private lateinit var savingsCount: TextView

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(requireContext(), "Sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("179083447885-u0hm3n142q61hgbovbgudlfanqs55sgn.apps.googleusercontent.com")
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        signedOutLayout = view.findViewById(R.id.signedOutLayout)
        signedInLayout = view.findViewById(R.id.signedInLayout)
        profileName = view.findViewById(R.id.profileName)
        profileEmail = view.findViewById(R.id.profileEmail)
        basketCount = view.findViewById(R.id.basketCount)
        savingsCount = view.findViewById(R.id.savingsCount)

        val btnSignIn = view.findViewById<TextView>(R.id.btnSignIn)
        btnSignIn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            signInLauncher.launch(signInIntent)
        }

        val btnSignOut = view.findViewById<TextView>(R.id.btnSignOut)
        btnSignOut.setOnClickListener {
            auth.signOut()
            googleSignInClient.signOut()
            updateUI()
        }

        val btnSettings = view.findViewById<TextView>(R.id.btnProfileSettings)
        btnSettings.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }

        updateUI()
        loadStats()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
        loadStats()
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "Signed in successfully!", Toast.LENGTH_SHORT).show()
                    updateUI()
                    loadStats()
                } else {
                    Toast.makeText(requireContext(), "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun updateUI() {
        val user = auth.currentUser
        if (user != null) {
            signedOutLayout.visibility = View.GONE
            signedInLayout.visibility = View.VISIBLE
            profileName.text = user.displayName ?: "Cartly User"
            profileEmail.text = user.email ?: ""
        } else {
            signedOutLayout.visibility = View.VISIBLE
            signedInLayout.visibility = View.GONE
        }
    }

    private fun loadStats() {
        db.collection("basket").get().addOnSuccessListener { result ->
            val count = result.documents.filter {
                it.getString("name") != "placeholder"
            }.size
            basketCount.text = "$count items in basket"
        }

        db.collection("favourites").get().addOnSuccessListener { result ->
            val count = result.documents.filter {
                it.getString("name") != "placeholder"
            }.size
            savingsCount.text = "$count deals saved"
        }
    }
}