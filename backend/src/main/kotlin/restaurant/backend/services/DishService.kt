package restaurant.backend.services

import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service
import restaurant.backend.db.entities.DishEntity
import restaurant.backend.db.repositories.DishRepository
import restaurant.backend.dto.*
import restaurant.backend.util.LoggingHelper
import java.util.*

@Service
class DishService(private val dishRepository: DishRepository) : LoggingHelper<DishService>(DishService::class.java) {

    @PostConstruct
    fun addSampleDishesOnAppInitialization() {
        val sampleDishes = arrayOf(
            DishEntity(
                name = "apple",
                quantity = 100,
                cookTime = 10000L,
                price = 2
            ),
            DishEntity(
                name = "bread",
                quantity = 50,
                cookTime = 5000L,
                price = 2
            ),
            DishEntity(
                name = "guacamole",
                quantity = 10,
                cookTime = 20000L,
                price = 8
            ),
            DishEntity(
                name = "cake",
                quantity = 10,
                cookTime = 120000L,
                price = 10
            ),
            DishEntity(
                name = "water",
                quantity = 100,
                cookTime = 5000L,
                price = 2
            ),
            DishEntity(
                name = "chicken",
                quantity = 20,
                cookTime = 300000L,
                price = 8
            ),
            DishEntity(
                name = "fried potato",
                quantity = 30,
                cookTime = 60000L,
                price = 4
            ),
        )
        for (dishEntity in sampleDishes) {
            if (dishRepository.findByName(dishEntity.name) == null) {
                dishRepository.save(dishEntity)
            }
        }
    }

    fun addDish(dishDto: DishDto): Pair<Boolean, String> {
        val addingCount = dishDto.quantity
        if (addingCount <= 0)
            return false to "Can't add dish: non-positive quantity"

        try {
            val dishId: Int = dishRepository.save(dishDto.toDishWithoutId()).dishId!!
            return true to "Added $addingCount dishes with id $dishId and name ${dishDto.name}"
        } catch (ex: org.springframework.dao.DataIntegrityViolationException) {
            logDebugOnIncorrectData(dishDto, "DishService::tryAddDish(DishDto)", ex)
            val message = ex.localizedMessage
            if (message.contains("value violates unique constraint")) {
                return false to "Can't add dish: dish with name ${dishDto.name} already exists"
            }
        } catch (ex: Throwable) {
            logError(dishDto.toString(), "DishService::tryAddDish(DishDto)", ex)
        }
        return false to "Can't add dish: incorrect data provided"
    }

    fun retrieveAllDishes(): List<DishDto> =
        dishRepository.findAllByOrderByDishIdAsc().map { dish: DishEntity -> DishDto(dish) }

    fun retrieveDishById(dishId: Int): DishDto? {
        val dishEntity: Optional<DishEntity> = dishRepository.findById(dishId)
        return when {
            dishEntity.isPresent -> DishDto(dishEntity.get())
            else -> null
        }
    }

    fun retrieveDishByString(dishName: String): DishDto? =
        when (val dishEntity: DishEntity? = dishRepository.findByName(dishName)) {
            null -> null
            else -> DishDto(dishEntity)
        }

    fun deleteDishById(dishId: Int): Pair<Boolean, String> {
        if (dishRepository.findById(dishId).isEmpty)
            return false to "Dish with id $dishId not found"
        dishRepository.deleteById(dishId)
        return true to "Deleted dish with id $dishId"
    }

    fun deleteDishByName(dishName: String): Pair<Boolean, String> {
        val dishEntity: DishEntity = dishRepository.findByName(dishName) ?: return false to "Dish $dishName not found"
        dishRepository.deleteById(dishEntity.dishId!!)
        return true to "Deleted dish with name $dishName"
    }

    fun updateDishPriceByName(updateDishPriceDto: UpdateDishPriceDto): Pair<Boolean, String> {
        val dishName = updateDishPriceDto.dishName
        val dishEntity: DishEntity = dishRepository.findByName(dishName) ?: return false to "Dish $dishName not found"
        val newPrice = updateDishPriceDto.newPrice
        return try {
            dishRepository.updatePriceById(dishEntity.dishId!!, newPrice)
            return true to "Set price for the dish with name $dishName equal to $newPrice"
        } catch (ex: Throwable) {
            logDebugOnIncorrectData(updateDishPriceDto, "DishService::updateDishPriceByName(UpdateDishPriceDto)", ex)
            false to "Can't update dish: incorrect new price for the dish provided"
        }
    }

    fun updateDishQuantityByName(updateDishQuantityDto: UpdateDishQuantityDto): Pair<Boolean, String> {
        val dishName = updateDishQuantityDto.dishName
        val dishEntity: DishEntity = dishRepository.findByName(dishName) ?: return false to "Dish $dishName not found"
        val newQuantity = updateDishQuantityDto.newQuantity
        return try {
            val dishId = dishEntity.dishId!!
            if (newQuantity != 0) {
                dishRepository.updateQuantityById(dishId, newQuantity)
            } else {
                dishRepository.deleteById(dishId)
            }
            return true to "Set quantity of the dish with name $dishName equal to $newQuantity"
        } catch (ex: Throwable) {
            logDebugOnIncorrectData(
                updateDishQuantityDto,
                "DishService::updateDishQuantityByName(UpdateDishQuantityDto)",
                ex
            )
            false to "Can't update dish: incorrect new quantity for the dish provided"
        }
    }

    fun updateDishCookTimeByName(updateDishCookTimeDto: UpdateDishCookTimeDto): Pair<Boolean, String> {
        val dishName = updateDishCookTimeDto.dishName
        val dishEntity: DishEntity = dishRepository.findByName(dishName) ?: return false to "Dish $dishName not found"
        val newCookTime = updateDishCookTimeDto.newCookTime
        return try {
            dishRepository.updateCookTimeById(dishEntity.dishId!!, newCookTime)
            return true to "Set cook time of the dish with name $dishName equal to $newCookTime"
        } catch (ex: Throwable) {
            logDebugOnIncorrectData(
                updateDishCookTimeDto,
                "DishService::updateDishCookTimeByName(UpdateDishCookTimeDto)",
                ex
            )
            false to "Can't update dish: incorrect new cook time for the dish provided"
        }
    }

    fun updateDishNameByName(updateDishNameDto: UpdateDishNameDto): Pair<Boolean, String> {
        val dishName = updateDishNameDto.dishName
        val dishEntity: DishEntity = dishRepository.findByName(dishName) ?: return false to "Dish $dishName not found"
        val newName = updateDishNameDto.newDishName
        return try {
            dishRepository.updateNameById(dishEntity.dishId!!, newName)
            true to "Set name of the dish with old name $dishName equal to $newName"
        } catch (ex: Throwable) {
            logDebugOnIncorrectData(updateDishNameDto, "DishService::updateDishNameByName(UpdateDishNameDto)", ex)
            false to "Can't update dish: incorrect new name for the dish provided"
        }
    }
}
