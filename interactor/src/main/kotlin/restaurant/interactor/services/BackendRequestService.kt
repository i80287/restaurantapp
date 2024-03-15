package restaurant.interactor.services

import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import restaurant.interactor.domain.JwtRequest
import restaurant.interactor.domain.JwtResponse
import restaurant.interactor.domain.RefreshJwtRequest
import restaurant.interactor.dto.*

@Service
class BackendRequestService(webClientBuilder: WebClient.Builder) {
    private final val webClient: WebClient = webClientBuilder
        .baseUrl("http://localhost:8081/")
        .build()
    
    private final lateinit var cachedAccessToken: String
    private final lateinit var cachedRefreshToken: String
    private final lateinit var cachedLogin: String
    private final lateinit var cachedPassword: String

    final fun loginUser(login: String, password: String): LoginResponseStatus {
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
        } catch (badReqEx: WebClientResponseException.BadRequest) {
            return LoginResponseStatus.INCORRECT_LOGIN_OR_PASSWORD
        } catch (forbEx: WebClientResponseException.Forbidden) {
            return LoginResponseStatus.FORBIDDEN
        } catch (reqEx: WebClientRequestException) {
            if (isServerNotRunningOrUnreachable(reqEx)) {
                return LoginResponseStatus.SERVER_IS_NOT_RUNNING
            }
            print("BackendRequestService::loginUser(): 1\n")
            println(reqEx)
        } catch (ex: Throwable) {
            print("BackendRequestService::loginUser(): 2\n")
            println(ex)
        }
        return LoginResponseStatus.UNKNOWN
    }

    final fun addDish(dishName: String, quantity: Int, cookTimeInMilliseconds: Long, price: Int): String =
        makeRequestHandleTokensUpdate("Unknown error occured while adding new dish") {
            postWithJsonBody(DishDto(
                dishId = null,
                name = dishName,
                quantity = quantity,
                cookTime = cookTimeInMilliseconds,
                price = price), "dishes/add")
        }

    final fun removeDish(dishName: String): String =
        makeRequestHandleTokensUpdate("Unknown error occured while deleting the dish") {
            deleteWithEmptyBody("dishes/delete/byname/$dishName")
        }

    final fun updateDishPrice(dishName: String, newPrice: Int): String =
        updateDishComponent(UpdateDishPriceDto(dishName, newPrice), "price")

    final fun updateDishCookTime(dishName: String, newCookTime: Long): String =
        updateDishComponent(UpdateDishCookTimeDto(dishName, newCookTime), "price")

    final fun updateDishQuantity(dishName: String, newQuantity: Int): String =
        updateDishComponent(UpdateDishQuantityDto(dishName, newQuantity), "price")

    final fun updateDishName(dishName: String, newName: String): String =
        updateDishComponent(UpdateDishNameDto(dishName, newName), "price")

    final fun getAllUsers(): Array<UserDto>? {
        val (userList, errorMessage) = makeRequestHandleTokensUpdate {
            getListWithEmptyBody<UserDto>("users/get/all")
        }
        if (userList == null) {
            println(errorMessage)
        }
        return userList
    }

    private final inline fun <reified UpdateDtoType : Any> updateDishComponent(newDtoObject: UpdateDtoType, componentName: String): String =
        makeRequestHandleTokensUpdate("Unknown error occured while updating dish's $componentName") {
            patchWithJsonBody(newDtoObject, "/dishes/update/$componentName")
        }

    private final fun updateTokens(): String? {
        return try {
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
            null
        } catch (ex: WebClientResponseException.Forbidden) {
            "Access to the resource denied"
        } catch (exBadReq: WebClientResponseException.BadRequest) {
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
    }

    private final inline fun <reified ResponseType : Any> getListWithEmptyBody(uri: String): Array<ResponseType> {
        assert(!uri.startsWith('/'))
        return webClient
            .get()
            .uri(uri)
            .accept(MediaType.APPLICATION_JSON)
            .headers { headers: HttpHeaders -> headers.setBearerAuth(cachedAccessToken) }
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<Array<ResponseType>>() {})
            .log()
            .block()!!
    }

    private final inline fun <reified SendType : Any, reified ResponseType : Any> postWithJsonBody(sendObject: SendType, uri: String): ResponseType {
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

    private final inline fun <reified ResponseType : Any> deleteWithEmptyBody(uri: String): ResponseType {
        assert(!uri.startsWith('/'))
        return webClient
            .delete()
            .uri(uri)
            .headers { headers: HttpHeaders -> headers.setBearerAuth(cachedAccessToken) }
            .retrieve()
            .bodyToMono(ResponseType::class.java)
            .block()!!
    }

    private final inline fun <reified UpdateDtoType : Any, reified ResponseType : Any> patchWithJsonBody(newDtoObject: UpdateDtoType, uri: String): ResponseType {
        assert(!uri.startsWith('/'))
        return webClient
            .post()
            .uri(uri)
            .headers { headers: HttpHeaders -> headers.setBearerAuth(cachedAccessToken) }
            .retrieve()
            .bodyToMono(ResponseType::class.java)
            .block()!!
    }

    private final fun isServerNotRunningOrUnreachable(ex: WebClientRequestException): Boolean {
        val message = ex.message
        return message != null && message.startsWith("Connection refused")
    }

    private final inline fun makeRequestHandleTokensUpdate(defaultErrorMessage: String, requestBlock: () -> String): String {
        var updatedTokens = false
        while (true) {
            try {
                return requestBlock()
            } catch (fbEx: WebClientResponseException.Forbidden) {
                if (!updatedTokens) {
                    updatedTokens = true
                    return updateTokens() ?: continue
                }
                return "Access to the resource denied"
            } catch (fbEx: WebClientResponseException.BadRequest) {
                return fbEx.responseBodyAsString
            } catch (reqEx: WebClientRequestException) {
                if (isServerNotRunningOrUnreachable(reqEx))
                    return "Server is not active or unreachable"
                print("BackendRequestService::makeRequestHandleTokensUpdate(String,() -> String)")
                println(reqEx)
            } catch (ex: Throwable) {
                print("BackendRequestService::makeRequestHandleTokensUpdate(String,() -> String)")
                println(ex)
            }
            return defaultErrorMessage
        }
    }

    private final inline fun <reified ReturnType : Any> makeRequestHandleTokensUpdate(requestBlock: () -> ReturnType): Pair<ReturnType?, String> {
        var updatedTokens = false
        while (true) {
            val message: String = try {
                return requestBlock() to ""
            } catch (fbEx: WebClientResponseException.Forbidden) {
                if (!updatedTokens) {
                    updatedTokens = true
                    updateTokens() ?: continue
                } else {
                    "Access to the resource denied"
                }
            } catch (fbEx: WebClientResponseException.BadRequest) {
                fbEx.responseBodyAsString
            } catch (reqEx: WebClientRequestException) {
                print("BackendRequestService::makeRequestHandleTokensUpdate(() -> ReturnType)")
                println(reqEx)
                if (isServerNotRunningOrUnreachable(reqEx))
                    "Server is not active or unreachable"
                else
                    "Unknown error"
            } catch (ex: Throwable) {
                print("BackendRequestService::makeRequestHandleTokensUpdate(() -> ReturnType)")
                println(ex)
                "Unknown error"
            }
            return null to message
        }
    }
}
