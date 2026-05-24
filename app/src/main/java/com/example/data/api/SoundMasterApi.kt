package com.example.data.api

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface SoundMasterApi {

    @GET("status")
    suspend fun checkStatus(): Response<StatusResponse>

    @GET("health")
    suspend fun checkHealth(): Response<StatusResponse>

    // Fallback simple root check
    @GET(".")
    suspend fun checkRoot(): Response<ResponseBody>

    @Multipart
    @POST("process")
    suspend fun processAudio(
        @Part file: MultipartBody.Part,
        @Part("effect") effect: RequestBody,
        @Part("intensity") intensity: RequestBody? = null
    ): Response<ResponseBody>

    @Multipart
    @POST("enhance")
    suspend fun enhanceAudio(
        @Part file: MultipartBody.Part,
        @Part("effect") effect: RequestBody
    ): Response<ResponseBody>

    @Multipart
    @POST("transcribe")
    suspend fun transcribeAudio(
        @Part file: MultipartBody.Part
    ): Response<TranscriptionResponse>
}

data class StatusResponse(
    val status: String = "online",
    val version: String? = null,
    val message: String? = null
)

data class TranscriptionResponse(
    val text: String,
    val language: String? = null
)

object SoundMasterApiClient {
    private var currentRetrofit: Retrofit? = null
    private var currentApi: SoundMasterApi? = null
    private var currentBaseUrl: String? = null

    fun getClient(baseUrl: String): SoundMasterApi {
        // Clean URL trailing slash
        val cleanedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        
        if (currentApi != null && currentBaseUrl == cleanedUrl) {
            return currentApi!!
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(cleanedUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

        val api = retrofit.create(SoundMasterApi::class.java)
        currentRetrofit = retrofit
        currentApi = api
        currentBaseUrl = cleanedUrl
        return api
    }
}
