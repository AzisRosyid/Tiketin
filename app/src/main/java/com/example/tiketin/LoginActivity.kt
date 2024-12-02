package com.example.tiketin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tiketin.api.ApiRetrofit
import com.example.tiketin.model.User
import com.example.tiketin.model.UserModel
import com.example.tiketin.ui.theme.TiketinTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : ComponentActivity() {
    private val api by lazy { ApiRetrofit().apiEndPoint }
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth
        Helper.firebaseAuth = FirebaseAuth.getInstance()

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val viewModel =  LoginViewModel()

        val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account, viewModel)
            } catch (e: ApiException) {
                Log.e("GoogleSignIn", "Sign-in failed: ${e.message}")
            }
        }


        setContent {
            TiketinTheme {
                viewModel.email = "azisrosyid@gmail.com"
                viewModel.password = "12345678"

                requestLocationPermission()

                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LoginScreen(
                        onGoogleSignInClick = {
                            viewModel.setLoading(true)
                            val signInIntent = googleSignInClient.signInIntent
                            signInLauncher.launch(signInIntent)
                        },
                        isLoading = viewModel.isLoading
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    fun requestLocationPermission() {
        val context = LocalContext.current
        val permissionState = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)

        LaunchedEffect(Unit) {
            permissionState.launchPermissionRequest()
        }
    }


    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount, viewModel: LoginViewModel) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        Helper.firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    api.auth(
                        Helper.AUTH_PIN,
                        Helper.firebaseAuth.currentUser!!.displayName!!,
                        Helper.firebaseAuth.currentUser!!.email!!,
                        Helper.firebaseAuth.currentUser!!.uid,
                    ).enqueue(object : Callback<UserModel> {
                        override fun onResponse(call: Call<UserModel>, response: Response<UserModel>) {
                            if (!response.isSuccessful) {
                                val errors = JSONObject(response.errorBody()!!.string())
                                Helper.message(errors.getString("errors"), this@LoginActivity, false)
                            } else {
                                Helper.id = response.body()!!.user.id
                                Helper.token = response.body()!!.token
                                saveToken(Helper.token)
                                startActivity(Intent(applicationContext, HomeActivity::class.java))
                                finish()
                            }
                        }

                        override fun onFailure(call: Call<UserModel>, t: Throwable) {
                            viewModel.setLoading(false) // Hide loading
                            Log.e("onFailure", t.message.toString())
                        }
                    })
                } else {
                    viewModel.setLoading(false) // Hide loading
                    Log.e("FirebaseAuth", "Authentication failed: ${task.exception?.message}")
                }
            }
    }


    private fun saveToken(token: String) {
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("token", token)

        editor.apply()
    }

    private fun loadToken() {
        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        Helper.token = sharedPreferences.getString("token", "") ?: ""
        refresh()
    }

    private fun refresh() {
        api.refresh().enqueue(object : Callback<UserModel> {
            override fun onResponse(call: Call<UserModel>, response: Response<UserModel>) {
                if (!response.isSuccessful) {
                    val errors = JSONObject(response.errorBody()!!.string())
                    Helper.message(errors.getString("errors"), this@LoginActivity, false)
                } else {
                    Helper.id = response.body()!!.user.id
                    Helper.token = response.body()!!.token
                    startActivity(Intent(applicationContext, HomeActivity::class.java))
                    finish()
                }
            }

            override fun onFailure(call: Call<UserModel>, t: Throwable) {
                Log.e("onFailure", t.message.toString())
            }
        })
    }
}

class LoginViewModel {
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    private val _isLoading = mutableStateOf(false)
    val isLoading: Boolean get() = _isLoading.value

    fun setLoading(isLoading: Boolean) {
        _isLoading.value = isLoading
    }
}

@Composable
private fun LoginScreen(onGoogleSignInClick: () -> Unit, isLoading: Boolean) {
    if (isLoading) {
        LoadingScreen()
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Gambar bus
            Image(
                painter = painterResource(id = R.drawable.bus),
                contentDescription = "Bus Image",
                modifier = Modifier
                    .padding(top = 180.dp)
                    .padding(bottom = 16.dp)
                    .fillMaxWidth(0.5f)
            )

            Text(
                text = "TIKETIN",
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Tiket bus lebih mudah",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Tombol Google dengan ikon
            Button(
                onClick = onGoogleSignInClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.google),
                    contentDescription = "Google Icon",
                    modifier = Modifier .size(24.dp)
                        .padding(end = 8.dp)
                )
                Text("Sign in with Google")
            }
        }
    }
}


@Composable
fun LoadingScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.padding(16.dp))
            Text(text = "Loading...", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TiketinTheme {

        val viewModel = remember { LoginViewModel() }

        viewModel.email = "azisrosyid@gmail.com"
        viewModel.password = "12345678"

        // A surface container using the 'background' color from the theme
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
        }
    }
}