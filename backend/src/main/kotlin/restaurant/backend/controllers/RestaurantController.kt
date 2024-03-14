package restaurant.backend.controllers

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange
import org.springframework.web.reactive.function.client.createExceptionAndAwait

@RestController
@RequestMapping("restaurant")
class RestaurantController(private val client: WebClient = WebClient.create("http://localhost:8080/restaurant")) {
    @GetMapping
    fun handle(): String = "String RestaurantController::handle() is called"

    @GetMapping("/imitate_job")
    suspend fun getAll(): String {
        delay(1000L)
        return "All"
    }

    @GetMapping("/start_imitation")
	suspend fun imitation(): String {
        val response = client.get()
            .uri("/imitate_job")
            .accept(MediaType.APPLICATION_JSON)
            .awaitExchange { response ->
                if (response.statusCode() == HttpStatus.OK) {
                    return@awaitExchange response.awaitBody<String>()
                } else {
                    throw response.createExceptionAndAwait()
                }
            }

        return response
    }
    
    @GetMapping("/start_imitation2")
    suspend fun anotherTest(): List<String> = coroutineScope {
        val deferedTask1 = async {
            delay(1L)
            return@async client.get()
                .uri("/imitate_job")
                .accept(MediaType.APPLICATION_JSON)
                .awaitExchange { it -> it.awaitBody<String>() }
        }
        val deferedTask2 = async {
            delay(1L)
            return@async client.get()
                .uri("/imitate_job")
                .accept(MediaType.APPLICATION_JSON)
                .awaitExchange { it -> it.awaitBody<String>() }
        }
        return@coroutineScope listOf(deferedTask1.await(), deferedTask2.await())
    }
}
