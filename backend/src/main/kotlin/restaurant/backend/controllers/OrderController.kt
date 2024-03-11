package restaurant.backend.controllers

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import restaurant.backend.dto.OrderAddDishDto
import restaurant.backend.dto.OrderDto
import restaurant.backend.services.OrderService

@RestController
@RequestMapping("orders")
class OrderController(private val orderService: OrderService) : ControllerHelper() {
    @GetMapping("/get/all")
    fun getOrders(): ResponseEntity<List<OrderDto>> {
        return ResponseEntity.ok(orderService.retrieveAllOrders())
    }

    @GetMapping("/get/byid/{id}")
    fun getOrderById(@PathVariable("id") orderId: Int): ResponseEntity<OrderDto> = responseFromNullable(orderService.retrieveOrderById(orderId))

    @PostMapping("/add")
    fun addOrder(@RequestBody order: OrderDto): ResponseEntity<String> = responseFromAddedId(orderService.tryAddOrder(order))

    @PostMapping("/add/dish")
    fun addDishToOrder(@RequestBody orderAddDishDto: OrderAddDishDto): ResponseEntity<String> = responseFromBoolStatus(orderService.tryAddDishToOrder(orderAddDishDto))
}
