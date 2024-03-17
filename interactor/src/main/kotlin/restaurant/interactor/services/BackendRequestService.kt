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
import kotlin.properties.Delegates

@Service
class BackendRequestService(webClientBuilder: WebClient.Builder) {
    private final val webClient: WebClient = webClientBuilder
        .baseUrl("http://localhost:8081/")
        .build()
    
    private final lateinit var cachedAccessToken: String
    private final lateinit var cachedRefreshToken: String
    private final lateinit var cachedLogin: String
    private final lateinit var cachedPassword: String
    private final var cachedUserId by Delegates.notNull<Int>()

    final fun loginUser(login: String, password: String): LoginResponseStatus {
        return try {
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
            cachedUserId = response.userId!!
            cachedLogin = login
            cachedPassword = password
            LoginResponseStatus.OK
        } catch (badReqEx: WebClientResponseException.BadRequest) {
            LoginResponseStatus.INCORRECT_LOGIN_OR_PASSWORD
        } catch (forbEx: WebClientResponseException.Forbidden) {
            LoginResponseStatus.FORBIDDEN
        } catch (forbEx: WebClientResponseException.ServiceUnavailable) {
            LoginResponseStatus.SERVER_IS_NOT_RUNNING_OR_UNAVAILABLE
        } catch (reqEx: WebClientRequestException) {
            println("debug print in BackendRequestService::loginUser(): 1")
            println(reqEx)
            statusFromWebClientRequestException(reqEx)
        } catch (ex: Throwable) {
            println("debug print in BackendRequestService::loginUser(): 2")
            println(ex)
            LoginResponseStatus.UNKNOWN
        }
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
        updateDishComponent(UpdateDishCookTimeDto(dishName, newCookTime), "cooktime")

    final fun updateDishQuantity(dishName: String, newQuantity: Int): String =
        updateDishComponent(UpdateDishQuantityDto(dishName, newQuantity), "quantity")

    final fun updateDishName(dishName: String, newName: String): String =
        updateDishComponent(UpdateDishNameDto(dishName, newName), "name")

    final fun getAllUsers(): Pair<Array<UserDto>?, String> =
        makeRequestHandleTokensUpdate {
            getListWithEmptyBody<UserDto>("users/get/all")
        }

    final fun getUserById(userId: Int): Pair<UserDto?, String> =
        makeRequestHandleTokensUpdate {
            getWithEmptyBody("users/get/byid/$userId")
        }

    final fun getUserByLogin(userLogin: String): Pair<UserDto?, String> =
        makeRequestHandleTokensUpdate {
            getWithEmptyBody("users/get/bylogin/$userLogin")
        }

    final fun addUser(userLogin: String, userPassword: String, userRole: Role): String =
        makeRequestHandleTokensUpdate("Unknown error occured while adding the user") {
            postWithJsonBody(UserDto(userId = null, login = userLogin, password = userPassword, role = userRole), "users/add")
        }

    final fun deleteUserById(userId: Int): String =
        makeRequestHandleTokensUpdate("Unknown error occured while deleting the user") {
            deleteWithEmptyBody("users/delete/byid/$userId")
        }

    final fun deleteUserByLogin(userLogin: String): String =
        makeRequestHandleTokensUpdate("Unknown error occured while deleting the user") {
            deleteWithEmptyBody("users/delete/bylogin/$userLogin")
        }

    final fun forceDeleteOrderById(orderId: Int): String =
        makeRequestHandleTokensUpdate("Unknown error occured while deleting the order") {
            deleteWithEmptyBody("orders/admin/delete/byid/$orderId")
        }

    final fun getAllOrders(): Pair<Array<OrderDto>?, String> =
        makeRequestHandleTokensUpdate {
            getListWithEmptyBody<OrderDto>("orders/get/all")
        }

    final fun getOrderById(orderId: Int): Pair<OrderDto?, String> =
        makeRequestHandleTokensUpdate {
            getWithEmptyBody("orders/get/byid/$orderId")
        }

    final fun getAllDishes(): Pair<Array<DishDto>?, String> =
        makeRequestHandleTokensUpdate {
            getListWithEmptyBody<DishDto>("dishes/get/all")
        }

    final fun getLoggedInUserOrders(): Pair<Array<OrderDto>?, String> =
        makeRequestHandleTokensUpdate {
            getListWithEmptyBody<OrderDto>("orders/get/userorders/byid/${getThisUserId()}")
        }

    final fun getThisUserId(): Int = cachedUserId

    final fun addOrder(userId: Int, dishIdsWithCounts: ArrayList<OrderDishDto>): String =
        makeRequestHandleTokensUpdate("Unknown error occured while creating order") {
            postWithJsonBody(OrderDto(orderOwnerId = userId, orderDishes = dishIdsWithCounts),
                             "/orders/add")
        }

    final fun addDishToOrder(orderId: Int, dishId: Int, addingCount: Int): String =
        makeRequestHandleTokensUpdate("Unknown error occured while adding $addingCount dish[es] with id $dishId to the order with id $orderId") {
            postWithJsonBody(OrderAddDishDto(orderId, dishId, addingCount), "orders/add/dish")
        }


    final fun deleteDishFromOrder(orderId: Int, dishId: Int, deletingCount: Int): String =
        makeRequestHandleTokensUpdate("Unknown error occured while deleting $deletingCount dish[es] with id $dishId to the order with id $orderId") {
            postWithJsonBody(OrderDeleteDishDto(orderId, dishId, deletingCount), "orders/delete/dish")
        }

    final fun deleteLoggedInUserOrderById(orderId: Int): String =
        makeRequestHandleTokensUpdate("Unknown error occured while deleting the order with id $orderId") {
            deleteWithEmptyBody("orders/delete/byid/$orderId")
        }

    final fun payForTheOrder(orderId: Int): String =
        makeRequestHandleTokensUpdate("Unknown error occured while paying for the order with id $orderId") {
            postWithEmptyBody("orders/pay/byid/$orderId")
        }

    private final inline fun <reified UpdateDtoType : Any> updateDishComponent(newDtoObject: UpdateDtoType, componentName: String): String =
        makeRequestHandleTokensUpdate("Unknown error occured while updating dish's $componentName") {
            postWithJsonBody<UpdateDtoType, String>(newDtoObject, "dishes/update/$componentName")
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
            "Both access and refresh tokens are expired and could not be updated. Please, relogin"
        } catch (forbEx: WebClientResponseException.ServiceUnavailable) {
            "Server is not running or unavailable"
        } catch (reqEx: WebClientRequestException) {
            explainWebClientRequestException(reqEx) ?: "Unknown error occured while updating access token"
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
            .block()!!
    }

    private final inline fun <reified  ResponseType : Any> getWithEmptyBody(uri: String): ResponseType {
        assert(!uri.startsWith('/'))
        return webClient
            .get()
            .uri(uri)
            .accept(MediaType.APPLICATION_JSON)
            .headers { headers: HttpHeaders -> headers.setBearerAuth(cachedAccessToken) }
            .retrieve()
            .bodyToMono(ResponseType::class.java)
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

    private final inline fun <reified ResponseType : Any> postWithEmptyBody(uri: String): ResponseType {
        assert(!uri.startsWith('/'))
        return webClient
            .post()
            .uri(uri)
            .headers { headers: HttpHeaders -> headers.setBearerAuth(cachedAccessToken) }
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

    private final fun explainWebClientRequestException(ex: WebClientRequestException): String? =
        when (statusFromWebClientRequestException(ex)) {
            LoginResponseStatus.SERVER_IS_NOT_RUNNING_OR_UNAVAILABLE -> "Server is not active or unreachable"
            LoginResponseStatus.CONNECTION_RESET -> "Connection reset by the server"
            else -> null
        }

    private final fun statusFromWebClientRequestException(ex: WebClientRequestException): LoginResponseStatus {
        val message = ex.message ?: return LoginResponseStatus.UNKNOWN
        if (message.startsWith("Connection refused")) {
            return LoginResponseStatus.SERVER_IS_NOT_RUNNING_OR_UNAVAILABLE
        }
        if (message.startsWith("Connection reset")) {
            return LoginResponseStatus.CONNECTION_RESET
        }
        return LoginResponseStatus.UNKNOWN
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
                return "Bad request. Server answer:\n - ${fbEx.responseBodyAsString}"
            } catch (mnaEx: WebClientResponseException.MethodNotAllowed) {
                return "Implementation error, API mismatch: method not allowed. Server answer:\n - ${mnaEx.localizedMessage}"
            } catch (forbEx: WebClientResponseException.ServiceUnavailable) {
                return "Server is not running or unavailable"
            } catch (reqEx: WebClientRequestException) {
                println("debug print in BackendRequestService::makeRequestHandleTokensUpdate(String,() -> String) 1")
                println(reqEx)
                return explainWebClientRequestException(reqEx) ?: defaultErrorMessage
            } catch (ex: Throwable) {
                println("debug print in BackendRequestService::makeRequestHandleTokensUpdate(String,() -> String) 2")
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
            } catch (notFoundEx: WebClientResponseException.NotFound) {
                "Not found"
            } catch (fbEx: WebClientResponseException.BadRequest) {
                "Bad request. Server answer:\n - ${fbEx.responseBodyAsString}"
            } catch (reqEx: WebClientRequestException) {
                println("debug print in BackendRequestService::makeRequestHandleTokensUpdate(() -> ReturnType) 1")
                println(reqEx)
                explainWebClientRequestException(reqEx) ?: "Unknown error"
            } catch (forbEx: WebClientResponseException.ServiceUnavailable) {
                "Server is not running or unavailable"
            } catch (ex: Throwable) {
                println("debug print in BackendRequestService::makeRequestHandleTokensUpdate(() -> ReturnType) 2")
                println(ex)
                "Unknown error"
            }
            return null to message
        }
    }
}
