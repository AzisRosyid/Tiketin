package com.example.menuapp.Api

import com.example.tiketin.model.BusDeparture
import com.example.tiketin.model.BusDepartureModel
import com.example.tiketin.model.Checkpoint
import com.example.tiketin.model.CheckpointModel
import com.example.tiketin.model.MovieModel
import com.example.tiketin.model.OrderModel
import com.example.tiketin.model.UserModel
import com.example.tiketin.model.ResponseModel
import com.example.tiketin.model.SeatModel
import com.example.tiketin.model.TicketModel
import com.example.tiketin.model.User
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*
import java.util.Date

interface ApiEndPoint {

    @FormUrlEncoded
    @POST("auth")
    fun auth(@Field("pin") pin: String, @Field("name") name: String, @Field("email") email: String, @Field("password") password: String): Call<UserModel>

    @FormUrlEncoded
    @GET("refresh")
    fun refresh(): Call<UserModel>

    @GET("checkpoint")
    fun getCheckpoint(): Call<CheckpointModel>

    @GET("bus/departure")
    fun getBusDeparture(): Call<BusDepartureModel>

    @GET("movie/{id}")
    fun showMovie(@Path("id") id: Int): Call<MovieModel>

    @GET("seat")
    fun getSeat(
        @Query("movie_schedule_id") id: Int,
        @Query("screening_date") date: String
    ): Call<SeatModel>

    @POST("order")
    fun createOrder(): Call<OrderModel>

    @FormUrlEncoded
    @POST("order/detail")
    fun createOrderDetail(
        @Field("order_id") order: Int,
        @Field("movie_schedule_id") schedule: Int,
        @Field("seat_id") seat: Int,
        @Field("date_screening") date: String
    ): Call<ResponseModel>

    @GET("ticket")
    fun getTicket(): Call<TicketModel>
}