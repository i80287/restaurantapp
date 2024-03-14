package restaurant.interactor.services

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import restaurant.interactor.domain.JwtRequest
import restaurant.interactor.domain.JwtResponse

@Service
class BackendRequestService(webClientBuilder: WebClient.Builder) {
    private final val webClient: WebClient = webClientBuilder
        .baseUrl("http://localhost:8081/")
        .build()
    
    private final lateinit var accessToken: String
    private final lateinit var refreshToken: String

    fun loginUser(login: String, password: String): LoginResponseStatus {
        try {
            val response: JwtResponse = webClient
                .post()
                .uri("auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(JwtRequest(login, password)), JwtRequest::class.java)
                .retrieve()
                .bodyToMono(JwtResponse::class.java)
                .block()!!
            assert(response.type == "Bearer")
            accessToken = response.accessToken
            refreshToken = response.refreshToken
            return LoginResponseStatus.OK
        } catch (forbEx: org.springframework.web.reactive.function.client.WebClientResponseException) {
            if (forbEx.statusCode.value() == HttpStatus.FORBIDDEN.value()) {
                return LoginResponseStatus.FORBIDDEN
            }
        } catch (reqEx: org.springframework.web.reactive.function.client.WebClientRequestException) {
            val message = reqEx.message
            if (message != null && message.startsWith("Connection refused")) {
                return LoginResponseStatus.SERVER_IS_NOT_RUNNING
            }
        } catch (_: Throwable) {
        }
        return LoginResponseStatus.UNKNOWN
    }
}
