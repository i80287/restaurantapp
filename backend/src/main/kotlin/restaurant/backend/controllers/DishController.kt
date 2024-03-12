package restaurant.backend.controllers

import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import restaurant.backend.dto.DishDto
import restaurant.backend.services.DishService

@RestController
@RequestMapping("dishes")
class DishController(private val dishService: DishService) : ControllerHelper() {
    @GetMapping("/get/all")
    fun getDishes(): ResponseEntity<List<DishDto>> {
        return ResponseEntity.ok(dishService.retrieveAllDishes())
    }

    @GetMapping("/get/byid/{id}")
    fun getDishById(@PathVariable("id") dishId: Int): ResponseEntity<DishDto> = responseFromNullable(dishService.retrieveDishById(dishId))

    @GetMapping("/get/byname/{name}")
    fun getDishByName(@PathVariable("name") dishName: String): ResponseEntity<DishDto> = responseFromNullable(dishService.retrieveDishByString(dishName))

    @PostMapping("/add")
    fun addDish(@RequestBody dish: DishDto): ResponseEntity<String> = responseFromAddedId(dishService.tryAddDish(dish))

    @DeleteMapping("/delete/{id}")
    fun deleteDish(@PathVariable("id") dishId: Int): ResponseEntity<DishDto> = responseFromNullable(dishService.deleteDish(dishId))
}
