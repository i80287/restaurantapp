package restaurant.backend.controllers

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import restaurant.backend.dto.OrderDto
import restaurant.backend.services.OrderService

@RestController
@RequestMapping("orders")
class OrderController(private val orderService: OrderService) : ControllerHelper() {
    @GetMapping("/get")
    fun getOrders(): ResponseEntity<List<OrderDto>> {
        return ResponseEntity.ok(orderService.retrieveAllOrders())
    }

    @PostMapping("/add")
    fun addOrder(@RequestBody order: OrderDto): ResponseEntity<String> = responseFromAddedId(orderService.tryAddOrder(order))
}
