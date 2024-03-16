package restaurant.backend.controllers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import restaurant.backend.dto.OrderAddDishDto
import restaurant.backend.dto.OrderDeleteDishDto
import restaurant.backend.dto.OrderDto
import restaurant.backend.services.AuthService
import restaurant.backend.services.OrderService
import restaurant.backend.util.PaidOrderStatus

@RestController
@RequestMapping("/orders")
class OrderController @Autowired constructor(
    private val orderService: OrderService,
    private val authService: AuthService,
) : ControllerHelper() {
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/get/all")
    fun getOrders(): ResponseEntity<List<OrderDto>> =
        ResponseEntity.ok(orderService.retrieveAllOrders())

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/get/byid/{id}")
    fun getOrderById(@PathVariable("id") orderId: Int): ResponseEntity<OrderDto> =
        responseFromNullable(orderService.retrieveOrderById(orderId))

    @GetMapping("/get/userorders/byid/{id}")
    fun getUserOrders(@PathVariable("id") userId: Int): ResponseEntity<List<OrderDto>> =
        responseFromNullable(orderService.retrieveAllUserOrders(userId, authService.getAuthentication().principal))

    @PostMapping("/add")
    suspend fun addOrder(@RequestBody order: OrderDto): ResponseEntity<String> =
        responseFromBoolStatus(orderService.addOrder(order))

    @PostMapping("/add/dish")
    suspend fun addDishToOrder(@RequestBody orderAddDishDto: OrderAddDishDto): ResponseEntity<String> =
        responseFromBoolStatus(orderService.addDishToOrder(orderAddDishDto, authService.getAuthentication().principal))

    @PostMapping("/delete/dish")
    suspend fun deleteDishFromOrder(@RequestBody orderDeleteDishDto: OrderDeleteDishDto): ResponseEntity<String> =
        responseFromBoolStatus(
            orderService.deleteDishFromOrder(
                orderDeleteDishDto,
                authService.getAuthentication().principal
            )
        )

    @PostMapping("/pay/byid/{id}")
    fun payOrder(@PathVariable("id") orderId: Int): ResponseEntity<String> =
        when (orderService.onOrderPaid(orderId)) {
            PaidOrderStatus.OK -> ResponseEntity.ok("Success")
            PaidOrderStatus.ORDER_DOES_NOT_EXIST -> ResponseEntity.notFound().build()
            PaidOrderStatus.ORDER_IS_NOT_READY -> ResponseEntity.badRequest().body("Order is not ready yet")
            PaidOrderStatus.OTHER_ERROR -> ResponseEntity.internalServerError()
                .body("Sorry, internal server error occured")
        }

    @DeleteMapping("/delete/byid/{id}")
    suspend fun deleteOrder(@PathVariable("id") orderId: Int): ResponseEntity<String> =
        responseFromBoolStatus(orderService.deleteOrder(orderId, authService.getAuthentication().principal))

    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/admin/delete/byid/{id}")
    suspend fun forceDeleteOrder(@PathVariable("id") orderId: Int): ResponseEntity<String> =
        responseFromBoolStatus(orderService.forceDeleteOrder(orderId))
}
