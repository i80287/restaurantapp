package restaurant.interactor

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

import kotlinx.serialization.json.Json.Default.parseToJsonElement
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray

fun String.utf8(): String = URLEncoder.encode(this, "UTF-8")

fun main(args: Array<String>) {
	val request = HttpRequest.newBuilder()
		.uri(URI.create("http://localhost:8080/users/get"))
		.build()
	val response: HttpResponse<String> = HttpClient
		.newBuilder()
		.build()
		.send(request, HttpResponse.BodyHandlers.ofString())
	val users: JsonArray = parseToJsonElement(response.body()).jsonArray
	print(users.size)
}
