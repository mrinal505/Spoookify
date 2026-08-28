package com.spoookify

import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class YoutubeExtractorHelper : Downloader() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .writeTimeout(4, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .connectionPool(okhttp3.ConnectionPool(10, 5, TimeUnit.MINUTES))
        .build()

    override fun execute(request: Request): Response {
        val method = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val data = request.dataToSend()

        val okHttpRequest = okhttp3.Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
            .apply {
                headers.forEach { (key, values) ->
                    values.forEach { addHeader(key, it) }
                }
                if (method == "POST") {
                    post(data?.toRequestBody() ?: "".toRequestBody())
                }
            }
            .build()

        val okHttpResponse = client.newCall(okHttpRequest).execute()
        return Response(
            okHttpResponse.code,
            okHttpResponse.message,
            okHttpResponse.headers.toMultimap(),
            okHttpResponse.body?.string(),
            okHttpResponse.request.url.toString()
        )
    }

    companion object {
        private var instance: YoutubeExtractorHelper? = null
        fun getInstance(): YoutubeExtractorHelper {
            if (instance == null) instance = YoutubeExtractorHelper()
            return instance!!
        }
    }
}
