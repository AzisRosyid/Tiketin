package com.example.tiketin

import android.annotation.SuppressLint
import android.content.Context
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.tiketin.Helper.firebaseAuth
import com.example.tiketin.Helper.id
import com.example.tiketin.Helper.token
import com.example.tiketin.api.ApiRetrofit
import com.example.tiketin.model.BusDeparture
import com.example.tiketin.model.BusSchedule
import com.example.tiketin.model.Checkpoint
import com.example.tiketin.model.Movie
import com.example.tiketin.model.MovieModel
import com.example.tiketin.ui.theme.TiketinTheme
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.awaitResponse

class HomeActivity : ComponentActivity() {

    private val api by lazy { ApiRetrofit().apiEndPoint }
    private var checkpointStart by mutableStateOf<Checkpoint?>(null)
    private var checkpointEnd by mutableStateOf<Checkpoint?>(null)
    private var busSchedule by mutableStateOf<BusSchedule?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel = remember {
                HomeViewModel()
            }

            TiketinTheme {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    BottomNavigationBar(context = this@HomeActivity, viewModel)
                }
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

                MovieGrid()
            }
        }
    }

    private fun openMapsActivity(code: Int) {
        val intent = Intent(this, MapsActivity::class.java)
        if (code == REQUEST_CODE_MAP_START){
            intent.putExtra("checkpoint_end", checkpointEnd?.id)
        } else if (code == REQUEST_CODE_MAP_END) {
            intent.putExtra("checkpoint_start", checkpointStart?.id)
        }
        startActivityForResult(intent, code)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_MAP_START && resultCode == RESULT_OK) {
            checkpointStart = data?.getSerializableExtra("checkpoint") as Checkpoint?
            // viewModel.checkpointStart = checkpointStart
        } else if (requestCode == REQUEST_CODE_MAP_END && resultCode == RESULT_OK) {
            checkpointEnd = data?.getSerializableExtra("checkpoint") as Checkpoint?
            // viewModel.checkpointEnd = checkpointEnd
        }
    }

    companion object {
        const val REQUEST_CODE_MAP_START = 1001
        const val REQUEST_CODE_MAP_END = 1002
    }

    class HomeViewModel {
        private val _isLoading = mutableStateOf(false)
        val isLoading: Boolean get() = _isLoading.value
    }

    private suspend fun getBusSchedules(checkpointStart: Int, checkpointEnd: Int): List<BusSchedule>? {
        return try {
            val response = api.getBusSchedule(checkpointStart, checkpointEnd).awaitResponse()
            if (response.isSuccessful) {
                response.body()?.busSchedules
            } else {
                null // Handle error
            }
        } catch (e: Exception) {
            null // Handle exception
        }
    }


    @Composable
    fun HomeScreen(navController: NavController, context: Context) {
        TiketinTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(15.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(horizontal = 15.dp, vertical = 10.dp)
                            .clip(MaterialTheme.shapes.large)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_map),
                            contentDescription = "home_screen_bg",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Text(
                        "Home Screen",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                }
            }
        }
    }

    @Composable
    fun OrderScreen(navController: NavController, context: Context) {
        var busSchedules by remember { mutableStateOf<List<BusSchedule>>(emptyList()) }
        val coroutineScope = rememberCoroutineScope()

        // Fetch bus schedules when checkpoints are selected
        LaunchedEffect(checkpointStart, checkpointEnd) {
            if (checkpointStart != null && checkpointEnd != null) {
                coroutineScope.launch {
                    val result = getBusSchedules(checkpointStart!!.id, checkpointEnd!!.id) ?: emptyList()

                    if (result.isNotEmpty()) {
                        busSchedules = result
                    }
                }
            }
        }

        TiketinTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.primary // Adjust color as per your theme
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Main card container
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            // From location
                            Surface(onClick = {
                                openMapsActivity(REQUEST_CODE_MAP_START)
                            }) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_home),
                                            contentDescription = "Location icon",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "From",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = checkpointStart?.name ?: "Select Location",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Divider(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                    )
                                }
                            }
                            // To location
                            Surface(onClick = {
                                openMapsActivity(REQUEST_CODE_MAP_END)
                            }) {
                                Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_home),
                                    contentDescription = "Location icon",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "To",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = checkpointEnd?.name ?: "Select Location",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Divider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            )
                                    }
                            }





                            // Travel date
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_map),
                                    contentDescription = "Calendar icon",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Bus Schedule",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            // Bus Schedule Dropdown
                            BusScheduleDropdown(busSchedules = busSchedules) { selectedSchedule ->
                                busSchedule = selectedSchedule
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            // Buttons

                            Spacer(modifier = Modifier.height(16.dp))

                            // Clear button
                            Button(
                                onClick = {
                                    checkpointStart = null
                                    checkpointEnd = null
                                    busSchedule = null
                                },
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Text("Clear", style = MaterialTheme.typography.bodyMedium)
                            }

                        }
                    }

                    // Search button at the bottom
                    Button(
                        onClick = {
                            if (busSchedule != null) {
                                val intent = Intent(context, TicketOrderActivity::class.java)
                                intent.apply {
                                    intent.putExtra("busSchedule", busSchedule)
                                }
                                startActivity(intent)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .align(Alignment.BottomCenter),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_map),
                            contentDescription = "Order",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pesan Bus", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun BusScheduleDropdown(busSchedules: List<BusSchedule>, onScheduleSelected: (BusSchedule) -> Unit) {
        var expanded by remember { mutableStateOf(false) }
        var selectedSchedule by remember { mutableStateOf<BusSchedule?>(null) }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            // Text field showing the selected item
            TextField(
                value = selectedSchedule?.let { "${it.bus.name} - Day ${it.day}, ${it.time}" } ?: "Select Bus Schedule",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                label = { Text("Bus Schedule") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            )

            // Dropdown menu with the list of schedules
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                busSchedules.forEach { schedule ->
                    val totalPrice = schedule.bus.price * schedule.bus_departure.multiplier
                    DropdownMenuItem(
                        onClick = {
                            selectedSchedule = schedule
                            expanded = false
                            onScheduleSelected(schedule)
                        },
                        text = {
                            Column(modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp)) {
                                Text("Bus Name: ${schedule.bus.name}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 4.dp))
                                Text("Class: ${schedule.bus.`class`}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp))
                                Text("Departure: Day ${schedule.day}, ${schedule.time}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp))
                                Text("Price: Rp$totalPrice", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 4.dp))

                                HorizontalDivider()
                            }
                        }
                    )
                }
            }
        }
    }


    fun logout(context: Context) {
        firebaseAuth.signOut()
        id = 0
        token = ""

        getSharedPreferences("MyPrefs", MODE_PRIVATE).edit().clear().apply()

        // Navigate to LoginActivity
        val intent = Intent(context, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear the activity stack
        context.startActivity(intent)
    }


    @Composable
    fun ProfileScreen(navController: NavController, context: Context) {
        TiketinTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(15.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                )  {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(horizontal = 15.dp, vertical = 10.dp)
                            .clip(MaterialTheme.shapes.large)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_profile),
                            contentDescription = "profile_screen_bg",
                            contentScale = ContentScale.Crop
                        )
                    }
                    Text(
                        "Profile Screen",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )

                    Button(
                        onClick = {
                            logout(context = context)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Logout")
                    }
                }
            }
        }
    }

    @SuppressLint("UnrememberedMutableState")
    @Composable
    fun BottomNavigationBar(context: Context, viewModel: HomeViewModel) {
        var navigationSelectedItem by remember { mutableStateOf(0) }
        val navController = rememberNavController()
        val titles = listOf("Home", "My Order", "Profile")
        var currentTitle by remember { mutableStateOf(titles[0]) }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = { CustomTopBar(title = currentTitle) },
            bottomBar = {
                NavigationBar {
                    BottomNavigationItem().bottomNavigationItems().forEachIndexed { index, navigationItem ->
                        NavigationBarItem(
                            selected = index == navigationSelectedItem,
                            label = { Text(navigationItem.label) },
                            icon = {
                                Icon(
                                    navigationItem.icon,
                                    contentDescription = navigationItem.label
                                )
                            },
                            onClick = {
                                navigationSelectedItem = index
                                currentTitle = titles[index]
                                navController.navigate(navigationItem.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = Screens.Home.route,
                modifier = Modifier.padding(paddingValues = paddingValues)
            ) {
                composable(Screens.Home.route) { HomeScreen(navController, context) }
                composable(Screens.Order.route) { OrderScreen(navController, context) }
                composable(Screens.Profile.route) { ProfileScreen(navController, context) }
            }
        }
    }

    data class BottomNavigationItem(
        val label : String = "",
        val icon : ImageVector = Icons.Filled.Home,
        val route : String = ""
    ) {

        fun bottomNavigationItems() : List<BottomNavigationItem> {
            return listOf(
                BottomNavigationItem(
                    label = "Home",
                    icon = Icons.Filled.Home,
                    route = Screens.Home.route
                ),
                BottomNavigationItem(
                    label = "Order",
                    icon = Icons.Filled.AddCircle,
                    route = Screens.Order.route
                ),
                BottomNavigationItem(
                    label = "Profile",
                    icon = Icons.Filled.AccountCircle,
                    route = Screens.Profile.route
                ),
            )
        }
    }
}

sealed class Screens(val route : String) {
    object Home : Screens("home_route")
    object Order : Screens("order_route")
    object Profile : Screens("profile_route")
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
private fun CustomTopBar(title: String) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val isWideScreen = screenWidthDp > 600.dp

    val topBarHeight = if (isWideScreen) 110.dp else 70.dp

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
            text = title,
            style = typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            modifier = Modifier
                .padding(bottom = 8.dp, top = 24.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}


