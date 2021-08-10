package com.example.masstouring.ranking

import android.util.Log
import com.example.masstouring.database.DatabaseHelper
import com.google.gson.GsonBuilder
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class SynchronizationThread(private val oDatabaseHelper: DatabaseHelper) : Thread() {

    @Override
    override fun run() {
        val recordItems : List<Double> = oDatabaseHelper.records.map { record -> record.distance }
        val builder = GsonBuilder()
        builder.serializeNulls()
        val json = builder.create().toJson(recordItems)
        var con: HttpURLConnection? = null
        //post
        try {
            val url = URL("http://192.168.0.4:8080/demo1_war_exploded/hello")
            con = url.openConnection() as HttpURLConnection?
            con?.requestMethod = "POST"
            con?.doOutput = true
            con?.doInput = true
            con?.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            BufferedWriter(OutputStreamWriter(con?.outputStream, StandardCharsets.UTF_8)).use { writer -> writer.write(json) }
            if (con?.responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader(InputStreamReader(con.inputStream, StandardCharsets.UTF_8)).use { reader ->
                    val builder1 = StringBuilder()
                    var line: String? = null
                    while (reader.readLine().also { line = it } != null) {
                        builder1.append(line)
                    }
                    Log.e("test", "recv:${builder1}")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            con?.disconnect()
        }

        //input

        //display??
    }
}