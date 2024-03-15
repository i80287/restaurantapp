package restaurant.interactor.services

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import restaurant.interactor.domain.JwtRequest
import restaurant.interactor.domain.JwtResponse
import restaurant.interactor.domain.RefreshJwtRequest
import restaurant.interactor.dto.DishDto

@Service
class BackendRequestService(webClientBuilder: WebClient.Builder) {
    private final val webClient: WebClient = webClientBuilder
        .baseUrl("http://localhost:8081/")
        .build()
    
    private final lateinit var cachedAccessToken: String
    private final lateinit var cachedRefreshToken: String
    private final lateinit var cachedLogin: String
    private final lateinit var cachedPassword: String

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
            cachedAccessToken = response.accessToken!!
            cachedRefreshToken = response.refreshToken!!
            cachedLogin = login
            cachedPassword = password
            return LoginResponseStatus.OK
        } catch (forbEx: WebClientResponseException.Forbidden) {
            return LoginResponseStatus.FORBIDDEN
        } catch (respEx: WebClientResponseException) {
            println(respEx)
        } catch (reqEx: WebClientRequestException) {
            if (isServerNotRunningOrUnreachable(reqEx)) {
                return LoginResponseStatus.SERVER_IS_NOT_RUNNING
            }
            println(reqEx)
        } catch (ex: Throwable) {
            println(ex)
        }
        return LoginResponseStatus.UNKNOWN
    }

    fun addDish(dishDto: DishDto): String {
        while (true) {
            try {
                return postJsonWithAccessToken(dishDto, "dishes/add")
            } catch (fbEx: WebClientResponseException.Forbidden) {
                if (updateAccessToken())
                    continue
            } catch (fbEx: WebClientResponseException.BadRequest) {
                return fbEx.responseBodyAsString
            } catch (reqEx: WebClientRequestException) {
                if (isServerNotRunningOrUnreachable(reqEx))
                    return "Server is not active or unreachable"
            } catch (ex: Throwable) {
                println(ex)
            }
            return "Unknown error occured while adding dish"
        }
    }

    private fun updateAccessToken(): Boolean {
        while (true) {
            val errorMessage = try {
                val response: JwtResponse = webClient
                    .post()
                    .uri("auth/token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Mono.just(RefreshJwtRequest(cachedRefreshToken)), RefreshJwtRequest::class.java)
                    .retrieve()
                    .bodyToMono(JwtResponse::class.java)
                    .block()!!
                cachedAccessToken = response.accessToken!!
                return true
            } catch (exBadReq: WebClientResponseException.BadRequest) {
                if (updateRefreshToken()) {
                    continue
                }
                "Both access and refresh tokens are expired and could not be updated"
            } catch (ex: WebClientRequestException) {
                if (isServerNotRunningOrUnreachable(ex)) {
                    "Server is not active or unreachable"
                } else {
                    "Unknown error occured while updating access token"
                }
            } catch (ex: Throwable) {
                "Unknown error occured while updating access token"
            }
            println(errorMessage)
            return false
        }
    }

    private final inline fun <reified SendType : Any, reified ResponseType : Any> postJsonWithAccessToken(sendObject: SendType, uri: String): ResponseType {
        assert(!uri.startsWith('/'))
        return webClient
            .post()
            .uri(uri)
            .headers { headers: HttpHeaders -> headers.setBearerAuth(cachedAccessToken) }
            .contentType(MediaType.APPLICATION_JSON)
            .body(Mono.just(sendObject), SendType::class.java)
            .retrieve()
            .bodyToMono(ResponseType::class.java)
            .block()!!
    }

    private final fun isServerNotRunningOrUnreachable(ex: WebClientRequestException): Boolean {
        val message = ex.message
        return message != null && message.startsWith("Connection refused")
    }

    private final fun updateRefreshToken(): Boolean {
        try {
            val response: JwtResponse = webClient
                .post()
                .uri("auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(RefreshJwtRequest(cachedRefreshToken)), RefreshJwtRequest::class.java)
                .retrieve()
                .bodyToMono(JwtResponse::class.java)
                .block()!!
            cachedAccessToken = response.accessToken!!
            cachedRefreshToken = response.refreshToken!!
            return true
        } catch (ex: WebClientRequestException) {
            if (isServerNotRunningOrUnreachable(ex))
                println("Server is not active or unreachable")
        } catch (_: Throwable) {
        }
        return false
    }
}
