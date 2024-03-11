package restaurant.backend.controllers

import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import restaurant.backend.dto.DishDto
import restaurant.backend.services.DishService

@RestController
@RequestMapping("dishes")
class DishController(private val dishService: DishService) : ControllerHelper() {
    @GetMapping("/get")
    fun getDishes(): ResponseEntity<List<DishDto>> {
        return ResponseEntity.ok(dishService.retrieveAllDishes())
    }
    
    @PostMapping("/add")
    @ResponseStatus(HttpStatus.CREATED)
    fun addDish(@RequestBody dish: DishDto): ResponseEntity<String> {
        return responseFromAddedId(dishService.tryAddDish(dish))
    }
}
