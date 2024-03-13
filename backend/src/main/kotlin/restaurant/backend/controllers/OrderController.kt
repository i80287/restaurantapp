package restaurant.backend.controllers

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import restaurant.backend.dto.OrderAddDishDto
import restaurant.backend.dto.OrderDeleteDishDto
import restaurant.backend.dto.OrderDto
import restaurant.backend.services.OrderService
import restaurant.backend.util.PaidOrderStatus

@RestController
@RequestMapping("orders")
class OrderController(private val orderService: OrderService) : ControllerHelper() {
    @GetMapping("/get/all")
    fun getOrders(): ResponseEntity<List<OrderDto>> = ResponseEntity.ok(orderService.retrieveAllOrders())

    @GetMapping("/get/byid/{id}")
    fun getOrderById(@PathVariable("id") orderId: Int): ResponseEntity<OrderDto> = responseFromNullable(orderService.retrieveOrderById(orderId))

    @PostMapping("/add")
    suspend fun addOrder(@RequestBody order: OrderDto): ResponseEntity<String> = responseFromAddedId(orderService.addOrder(order))

    @PostMapping("/add/dish")
    suspend fun addDishToOrder(@RequestBody orderAddDishDto: OrderAddDishDto): ResponseEntity<String> =
        responseFromBoolStatus(orderService.addDishToOrder(orderAddDishDto), "incorrect data")

    @PostMapping("/delete/dish")
    suspend fun deleteDishFromOrder(@RequestBody orderDeleteDishDto: OrderDeleteDishDto): ResponseEntity<String> =
        responseFromBoolStatus(orderService.deleteDishFromOrder(orderDeleteDishDto), "incorrect data")

    @PostMapping("/paid/{id}")
    fun payOrder(@PathVariable("id") orderId: Int): ResponseEntity<String> =
        when (orderService.onOrderPaid(orderId)) {
            PaidOrderStatus.OK -> ResponseEntity.ok("success")
            PaidOrderStatus.ORDER_DOES_NOT_EXIST -> ResponseEntity.notFound().build()
            PaidOrderStatus.ORDER_IS_NOT_READY -> ResponseEntity.badRequest().body("order is not ready yet")
            PaidOrderStatus.OTHER_ERROR -> ResponseEntity.internalServerError().body("incorrect data")
        }

    @DeleteMapping("/delete/{id}")
    suspend fun deleteOrder(@PathVariable("id") orderId: Int): ResponseEntity<String> = responseFromBoolStatus(orderService.deleteOrder(orderId))
}
