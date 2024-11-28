package com.example.tiketin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardElevation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.ShapeDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.tiketin.api.ApiRetrofit
import com.example.tiketin.model.BusDeparture
import com.example.tiketin.model.Movie
import com.example.tiketin.model.MovieModel
import com.example.tiketin.ui.theme.TiketinTheme
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.awaitResponse

class HomeActivity : ComponentActivity() {

    private val api by lazy { ApiRetrofit().apiEndPoint }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        setContent {
            TiketinTheme {
                HomeActivityScreen()
//                Box(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .background(MaterialTheme.colorScheme.background)
//                ) {
//                    MovieGrid()
//
//                    CustomTopBar()
//
//                    Button(
//                        onClick = { openMapsActivity() },
//                        modifier = Modifier
//                            .align(Alignment.BottomCenter)
//                            .padding(vertical = 8.dp, horizontal = 16.dp)
//                            .fillMaxWidth(),
//                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
//                    ) {
//                        Text("Open Google Map to Select Bus Departure", color = Color.White)
//                    }
//                }
            }
        }
    }

    @Composable
    private fun MovieGrid() {
        var busDeparture by remember { mutableStateOf<List<BusDeparture>>(emptyList()) }

        LaunchedEffect(key1 = Unit) {
            getBusDeparture()?.let {
                busDeparture = it
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .padding(16.dp)
                .padding(vertical = 40.dp)
        ) {
//            items(movies) { movie ->
//                MovieCard(movie = movie)
//            }
        }
    }

    private suspend fun getBusDeparture(): List<BusDeparture>? {
        return try {
            val response = api.getBusDeparture().awaitResponse()
            if (response.isSuccessful) {
                response.body()?.busDepartures
            } else {
                // Handle error
                null
            }
        } catch (e: Exception) {
            // Handle exception
            null
        }
    }

    @Preview(showBackground = true)
    @Composable
    private fun GreetingPreviews() {
        TiketinTheme {
            // A surface container using the 'background' color from the theme
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background
            ) {
                CustomTopBar()

                MovieGrid()
            }
        }
    }

    private fun openMapsActivity() {
        val intent = Intent(this, MapsActivity::class.java)
        startActivityForResult(intent, REQUEST_CODE_MAP)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_MAP && resultCode == RESULT_OK) {
            val checkpoints = data?.getStringArrayListExtra("CHECKPOINTS")
            Log.d("HomeActivity", "Selected Checkpoints: $checkpoints")
            // Handle received checkpoints (e.g., update UI or save data)
        }
    }

    companion object {
        const val REQUEST_CODE_MAP = 1001
    }
}

@Composable
private fun MovieCard(movie: Movie) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = ShapeDefaults.Medium,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable {

                }
        ) {

            var model: Any = R.drawable.baseline_movie_24
            if (!movie.image.isNullOrEmpty()) {
                model = "${Helper.BASE_IMAGE}${movie.image}"
            }

            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = movie.title,
                    style = typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(text = "Genre: ${movie.genre}", style = typography.bodyMedium)
                Text(
                    text = "Price: ${Helper.currencyFormat(movie.price)}",
                    style = typography.bodyMedium
                )
            }
        }
    }


}

@Composable
private fun CustomTopBar() {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val isWideScreen = screenWidthDp > 600.dp

    val topBarHeight = if (isWideScreen) 110.dp else 50.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(topBarHeight)
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(
                    bottomStart = 30.dp,
                    bottomEnd = 30.dp
                )
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Text(
            text = "My Order",
            style = typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            modifier = Modifier
                .padding(bottom = 8.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}


@Composable
fun BottomBar(
    selectedItem: Int,
    onItemSelected: (Int) -> Unit
) {
    val items = listOf("Home", "Order", "Profile")

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = Color.White,
    ) {
        items.forEachIndexed { index, label ->
            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(
                            id = when (index) {
                                0 -> R.drawable.ic_home // Replace with your drawable
                                1 -> R.drawable.ic_map // Replace with your drawable
                                else -> R.drawable.ic_profile // Replace with your drawable
                            }
                        ),
                        contentDescription = null
                    )
                },
                label = {
                    Text(
                        text = label,
                        color = if (selectedItem == index) Color.Black else Color.Gray,
                        fontSize = 10.sp
                    )
                },
                selected = selectedItem == index,
                onClick = { onItemSelected(index) },
                alwaysShowLabel = true
            )
        }
    }
}

@Composable
fun MainContent(
    selectedIndex: Int,
    onSelectedChange: (Int) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (selectedIndex) {
            0 -> {
                // Replace with your Home content
                Text(text = "Order Screen", modifier = Modifier.align(Alignment.Center))
            }
            1 -> {
                // Replace with your Order content
                Text(text = "Order Screen", modifier = Modifier.align(Alignment.Center))
            }
            2 -> {
                // Replace with your Profile content
                Text(text = "Profile Screen", modifier = Modifier.align(Alignment.Center))
            }
        }

        BottomBar(selectedItem = selectedIndex, onItemSelected = onSelectedChange)

    }
}

@Composable
fun HomeActivityScreen() {
    var selectedIndex by remember { mutableStateOf(0) }

    TiketinTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            MainContent(
                selectedIndex = selectedIndex,
                onSelectedChange = { selectedIndex = it }
            )
        }
    }
}

