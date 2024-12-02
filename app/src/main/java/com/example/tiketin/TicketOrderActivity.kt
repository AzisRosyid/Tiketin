package com.example.tiketin

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.tiketin.api.ApiRetrofit
import com.example.tiketin.model.BusSchedule
import com.example.tiketin.model.BusScheduleModel
import com.example.tiketin.model.MovieModel
import com.example.tiketin.model.OrderModel
import com.example.tiketin.model.ResponseModel
import com.example.tiketin.model.Schedule
import com.example.tiketin.model.Seat
import com.example.tiketin.model.SeatModel
import com.example.tiketin.model.UserModel
import com.example.tiketin.ui.theme.TiketinTheme
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.await
import retrofit2.awaitResponse
import java.io.Serializable
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.util.Date
import java.util.Locale

class TicketOrderActivity : ComponentActivity() {
    private val api by lazy { ApiRetrofit().apiEndPoint }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TiketinTheme {
                TicketOrderScreen()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun TicketOrderScreen() {
        val busSchedule = remember { getSerializable(this, "busSchedule", BusSchedule::class.java) }
        var busScheduleModel by remember { mutableStateOf<BusScheduleModel?>(null) }
        val viewModel = remember { TicketOrderViewModel() }
        var seatModel by remember { mutableStateOf<SeatModel?>(null) }

        LaunchedEffect(busSchedule) {
            busScheduleModel = showBusSchedule(busSchedule.id)
        }

        LaunchedEffect(viewModel.selectedDate) {
            getSeat(busSchedule.id, viewModel)?.let {
                seatModel = it
            }
        }

        busScheduleModel?.let { nonNullBusScheduleModel ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 42.dp)
                ) {
                    TicketOrderContent(nonNullBusScheduleModel, seatModel, viewModel)
                }

                CustomTopBar()

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(vertical = 8.dp, horizontal = 16.dp)
                        .fillMaxWidth()
                )
                {
                    Text(text = "Grand Total: ${Helper.currencyFormat(viewModel.selectedSeats.count() * busSchedule.bus.price * busSchedule.bus_departure.multiplier)}")


                    Button(
                        onClick = {
                            onFinish(viewModel)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                    ) {
                        Text("Finish", color = Color.White)
                    }
                }
            }
        } ?: run {
            // Show error message if busScheduleModel is null
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 42.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Error: Error", color = Color.Red)
                }
            }
        }
    }

    private fun onFinish(viewModel: TicketOrderViewModel) {
        if (viewModel.selectedSeats.isNotEmpty()) {
            var successfulApiCalls = 0
            val totalApiCalls = viewModel.selectedSeats.size

            api.createOrder().enqueue(object : Callback<OrderModel> {
                override fun onResponse(call: Call<OrderModel>, response: Response<OrderModel>) {
                    if (response.isSuccessful) {
                        val order = response.body()?.order

                        if (order != null) {
                            viewModel.selectedSeats.forEach { seat ->
                                api.createOrderDetail(
                                    order.id,
                                    viewModel.selectedTime.toInt(),
                                    seat.id,
                                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                                        SimpleDateFormat(
                                            Helper.DATE_PATTERN,
                                            Locale.getDefault()
                                        ).parse(viewModel.selectedDate)
                                    )
                                ).enqueue(object : Callback<ResponseModel> {
                                    override fun onResponse(
                                        call: Call<ResponseModel>,
                                        response: Response<ResponseModel>
                                    ) {
                                        if (response.isSuccessful) {
                                            successfulApiCalls++

                                            if (successfulApiCalls == totalApiCalls) {
                                                navigateToHomeActivity(viewModel)
                                            }
                                        }
                                    }

                                    override fun onFailure(
                                        call: Call<ResponseModel>,
                                        t: Throwable
                                    ) {
                                        Log.e("onFailure", t.message.toString())
                                    }
                                })
                            }
                        } else {
                            Log.e("onFinish", "Received null order response")
                        }
                    } else {
                        Log.e("onFinish", "Failed to create order")
                    }
                }

                override fun onFailure(call: Call<OrderModel>, t: Throwable) {
                    Log.e("onFailure", t.message.toString())
                }
            })
        } else {
            Helper.message("Fill all the required option", this)
        }
    }

    private fun navigateToHomeActivity(viewModel: TicketOrderViewModel) {
        val intent = Intent(this@TicketOrderActivity, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private suspend fun showBusSchedule(id: Int): BusScheduleModel? {
        return try {
            val response = api.showBusSchedule(id = id).awaitResponse()
            if (response.isSuccessful) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun getSeat(busScheduleId: Int, viewModel: TicketOrderViewModel): SeatModel? {
        return try {
            val selectedTime: Int = try {
                viewModel.selectedTime.toInt()
            } catch (e: NumberFormatException) {
                return null
            }

            val selectedDate: String? = try {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                    SimpleDateFormat(
                        Helper.DATE_PATTERN,
                        Locale.getDefault()
                    ).parse(viewModel.selectedDate)
                )
            } catch (e: java.text.ParseException) {
                return null
            }

            if (selectedTime != 0 && selectedDate != null) {
                val response = api.getSeat(id = selectedTime, date = selectedDate).awaitResponse()
                if (response.isSuccessful) {
                    response.body()
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview2() {
        TiketinTheme {
            TicketOrderScreen()
        }
    }
}

class TicketOrderViewModel {
    var selectedTime by mutableStateOf("Select time schedule")
    var selectedDate by mutableStateOf("Open date picker dialog")
    var selectedSeats by mutableStateOf<Set<Seat>>(setOf())
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun TicketOrderContent(
    busScheduleModel: BusScheduleModel,
    seatModel: SeatModel?,
    viewModel: TicketOrderViewModel
) {
    val busSchedule: BusSchedule = busScheduleModel.busSchedule
    // var selectedDate by remember { mutableStateOf(LocalDate.now()) }
//    viewModel.selectedDate = "Open date picker dialog"
//    var comboBoxValue by remember { mutableStateOf(busScheduleModel.schedules.firstOrNull()?.let { "${it.start_time} - ${it.end_time}" } ?: "00:00:00 - 00:00:00") }
    var comboBoxValue by remember { mutableStateOf(viewModel.selectedTime) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    val dropdownValues: List<BusSchedule> = busScheduleModel.busSchedules

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        var model: Any = R.drawable.baseline_movie_24
//        if (!busSchedule.bus.isNullOrEmpty()) {
//            model = "${Helper.BASE_IMAGE}${busSchedule.bus.price}"
//        }

        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
        )

        Text(
            text = busSchedule.description,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .fillMaxWidth()
        )

        val totalPrice = busSchedule.bus.price * busSchedule.bus_departure.multiplier

        TicketOrderRow("Price", Helper.currencyFormat(totalPrice))

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Option",
                        style = MaterialTheme.typography.titleLarge
                            .copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                        textAlign = TextAlign.Center
                    )
                }

//                ComboBox(
//                    label = "Time",
//                    selectedValue = comboBoxValue,
//                    values = dropdownValues,
//                    onValueChange = {
//                        viewModel.selectedTime = it.id.toString()
//                        comboBoxValue = "${it.start_time} - ${it.end_time}"
//                        viewModel.selectedSeats = setOf()
//                    },
//                    isDropdownExpanded = isDropdownExpanded,
//                    onToggleDropdown = { isDropdownExpanded = !isDropdownExpanded }
//                )

                Column(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                ) {
                    Text(text = "Screening Date", style = MaterialTheme.typography.bodyMedium)

                    MyDatePickerDialog(viewModel)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Select Seat",
                        style = MaterialTheme.typography.titleLarge
                            .copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                        textAlign = TextAlign.Center
                    )
                }


                seatModel?.let { nonNullSeatModel ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(top = 8.dp)
                    )
                    {

                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 60.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            itemsIndexed(nonNullSeatModel.seats) { s, seat ->
                                SeatButton(
                                    text = "${s + 1}",
                                    seat = seat,
                                    selectedSeats = viewModel.selectedSeats,
                                    onSeatSelected = { selectedSeat ->
                                        viewModel.selectedSeats =
                                            if (viewModel.selectedSeats.contains(selectedSeat)) {
                                                viewModel.selectedSeats - selectedSeat
                                            } else {
                                                viewModel.selectedSeats + selectedSeat
                                            }
                                    })
                            }
                        }
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(MaterialTheme.colorScheme.background)
                .padding(8.dp)
        )
    }
}

@Composable
fun SeatButton(text: String, seat: Seat, selectedSeats: Set<Seat>, onSeatSelected: (Seat) -> Unit) {
    val buttonColor = when (seat.status) {
        "Booked" -> Color.Yellow
        "Ordered" -> Color.Green
        "Available" -> {
            if (selectedSeats.contains(seat)) Color.Black else MaterialTheme.colorScheme.primary
        }

        else -> Color.Gray // Default color for unknown status
    }

    Button(
        onClick = {
            if (seat.status == "Available") {
                onSeatSelected(seat)
            }
        },
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor,
            contentColor = if (seat.status == "Available") Color.White else Color.Black
        )
    ) {
        Text(text = text)
    }
}


//@Composable
//fun ComboBox(
//    label: String,
//    selectedValue: String,
//    values: List<Schedule>,
//    onValueChange: (Schedule) -> Unit,
//    isDropdownExpanded: Boolean,
//    onToggleDropdown: () -> Unit
//) {
//    Box(
//        modifier = Modifier
//            .fillMaxWidth()
//    ) {
//        Column {
//            Text(
//                text = label,
//                style = MaterialTheme.typography.bodyMedium,
//                modifier = Modifier.padding(top = 8.dp)
//            )
//
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .background(Color.White)
//                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
//                    .clickable { onToggleDropdown() }
//                    .padding(16.dp)
//            ) {
//                BasicTextField(
//                    value = selectedValue,
//                    onValueChange = {},
//                    keyboardOptions = KeyboardOptions.Default.copy(
//                        imeAction = ImeAction.Done,
//                        keyboardType = KeyboardType.Text
//                    ),
//                    keyboardActions = KeyboardActions(
//                        onDone = {
//                            onToggleDropdown()
//                        }
//                    ),
//                    textStyle = LocalTextStyle.current.copy(color = Color.Black),
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .clickable { onToggleDropdown() }
//                        .padding(16.dp)
//                )
//
//                if (isDropdownExpanded) {
//                    DropdownMenu(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .background(Color.White)
//                            .border(
//                                1.dp,
//                                MaterialTheme.colorScheme.primary,
//                                RoundedCornerShape(4.dp)
//                            ),
//                        expanded = true,
//                        onDismissRequest = { onToggleDropdown() }
//                    ) {
//                        values.forEach { value ->
//                            DropdownMenuItem(
//                                text = {
//                                    Text(
//                                        "${value.start_time} - ${value.end_time}",
//                                        color = Color.Black
//                                    )
//                                },
//                                onClick = {
//                                    onValueChange(value)
//                                    onToggleDropdown()
//                                }
//                            )
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
//
@Composable
fun MyDatePickerDialog(viewModel: TicketOrderViewModel) {

    var showDatePicker by remember {
        mutableStateOf(false)
    }

    Box(contentAlignment = Alignment.Center) {
        Button(onClick = { showDatePicker = true }) {
            Text(text = viewModel.selectedDate)
        }
    }

    if (showDatePicker) {
        MyDatePickerDialog(
            onDateSelected = {
                viewModel.selectedDate = it
                viewModel.selectedSeats = setOf()
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDatePickerDialog(
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(selectableDates = object : SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
            return utcTimeMillis >= System.currentTimeMillis()
        }
    })

    val selectedDate = datePickerState.selectedDateMillis?.let {
        convertMillisToDate(it)
    } ?: ""

    DatePickerDialog(
        modifier = Modifier.width(200.dp),
        onDismissRequest = { onDismiss() },
        confirmButton = {
            Button(onClick = {
                onDateSelected(selectedDate)
                onDismiss()
            }

            ) {
                Text(text = "OK")
            }
        },
        dismissButton = {
            Button(onClick = {
                onDismiss()
            }) {
                Text(text = "Cancel")
            }
        }
    ) {
        DatePicker(
            state = datePickerState
        )
    }
}

private fun convertMillisToDate(millis: Long): String {
    val formatter = SimpleDateFormat(Helper.DATE_PATTERN)
    return formatter.format(Date(millis))
}

@Composable
private fun TicketOrderRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
        )
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
            text = "Ticket Order",
            style = MaterialTheme.typography.headlineMedium.copy(
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

private fun <T : Serializable?> getSerializable(
    activity: Activity,
    name: String,
    clazz: Class<T>
): T {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        activity.intent.getSerializableExtra(name, clazz)!!
    else
        activity.intent.getSerializableExtra(name) as T
}