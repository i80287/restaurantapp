package restaurant.backend.controllers

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import restaurant.backend.dto.*
import restaurant.backend.services.DishService

@RestController
@RequestMapping("/dishes")
class DishController(private val dishService: DishService) : ControllerHelper() {
    @GetMapping("/get/all")
    fun getDishes(): ResponseEntity<List<DishDto>> =
        ResponseEntity.ok(dishService.retrieveAllDishes())

    @GetMapping("/get/byid/{id}")
    fun getDishById(@PathVariable("id") dishId: Int): ResponseEntity<DishDto> =
        responseFromNullable(dishService.retrieveDishById(dishId))

    @GetMapping("/get/byname/{name}")
    fun getDishByName(@PathVariable("name") dishName: String): ResponseEntity<DishDto> =
        responseFromNullable(dishService.retrieveDishByString(dishName))

    @PostMapping("/add")
    fun addDish(@RequestBody dish: DishDto): ResponseEntity<String> =
        responseFromBoolStatus(dishService.addDish(dish))

    @DeleteMapping("/delete/byid/{id}")
    fun deleteDishById(@PathVariable("id") dishId: Int): ResponseEntity<String> =
        responseFromBoolStatus(dishService.deleteDishById(dishId))

    @DeleteMapping("/delete/byname/{name}")
    fun deleteDishByName(@PathVariable("name") dishName: String): ResponseEntity<String> =
        responseFromBoolStatus(dishService.deleteDishByName(dishName))

    @PatchMapping("/update/price")
    fun updateDishPrice(updateDishPriceDto: UpdateDishPriceDto): ResponseEntity<String> =
        responseFromBoolStatus(dishService.updateDishPriceByName(updateDishPriceDto))

    @PatchMapping("/update/quantity")
    fun updateDishQuantity(updateDishQuantityDto: UpdateDishQuantityDto): ResponseEntity<String> =
        responseFromBoolStatus(dishService.updateDishQuantityByName(updateDishQuantityDto))

    @PatchMapping("/update/cooktime")
    fun updateDishCookTime(updateDishCookTimeDto: UpdateDishCookTimeDto): ResponseEntity<String> =
        responseFromBoolStatus(dishService.updateDishCookTimeByName(updateDishCookTimeDto))

    @PatchMapping("/update/name")
    fun updateDishName(updateDishNameDto: UpdateDishNameDto): ResponseEntity<String> =
        responseFromBoolStatus(dishService.updateDishNameByName(updateDishNameDto))
}
