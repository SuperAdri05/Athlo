package com.example.athlo.controlador

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object ChatIAController {

    private val client = OkHttpClient()
    private const val apiKey = "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" // Sustituye por tu clave real
    private const val url = "https://openrouter.ai/api/v1/chat/completions"

    suspend fun enviarMensaje(mensajeUsuario: String, contextoResumen: String? = null): String {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = buildString {
                    append("Eres un asistente fitness. Contesta con precisión en una sola frase.\n")
                    if (!contextoResumen.isNullOrBlank()) append("Datos del usuario: $contextoResumen\n")
                    append("Usuario: $mensajeUsuario")
                }


                val jsonBody = JSONObject()
                jsonBody.put("model", "mistralai/mistral-7b-instruct")
                jsonBody.put("stream", false)

                val messagesArray = org.json.JSONArray()
                messagesArray.put(JSONObject().put("role", "user").put("content", prompt))
                jsonBody.put("messages", messagesArray)

                val mediaType = "application/json".toMediaTypeOrNull()
                val body = jsonBody.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    return@withContext "Error: código ${response.code} - ${response.message}"
                }

                val jsonResponse = response.body?.string() ?: return@withContext "Error: sin respuesta"

                val content = JSONObject(jsonResponse)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")

                content.trim()
            } catch (e: Exception) {
                "Ocurrió un error: ${e.message}"
            }
        }
    }

}
