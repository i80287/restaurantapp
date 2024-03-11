package restaurant.backend.services

import kotlinx.coroutines.Job
import restaurant.backend.db.entities.DishEntity
import restaurant.backend.db.entities.OrderEntity
import restaurant.backend.db.entities.OrderDishEntity
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class OrderTask(private val order: OrderEntity, private val scheduler: OrderScheduler) {
    private val cookingDishes = ConcurrentHashMap<DishTask, Job>(order.dishes.size)
    private var readyDishesCount = AtomicInteger()
    private var totalDishesCount = AtomicInteger()
    val dishesTasks = ArrayList<DishTask>(order.dishes.size)

    init {
        for (dish: OrderDishEntity in order.dishes) {
            val dishId: Int = dish.dishId
            val cookTime: Long = dish.dish!!.cookTime
            repeat(dish.orderedCount) {
                dishesTasks.add(DishTask(dishId = dishId,
                                         cookTime = cookTime,
                                         orderTask = this,
                                         priority = 5))   
            }
        }
        totalDishesCount.getAndAdd(order.dishes.size)
    }

    val orderId: Int = order.orderId
    fun readyDishesCount(): Int = readyDishesCount.get()
    fun totalDishesCount(): Int = totalDishesCount.get()
    fun ready(): Boolean = readyDishesCount() == totalDishesCount()

    fun addDishes(dish: DishEntity, addingCount: Int): ArrayList<DishTask> {
        assert(addingCount > 0)
        // Prevent ready() from becoming true while adding new dishes
        totalDishesCount.getAndAdd(addingCount)
        val dishId: Int = dish.dishId
        val cookTime: Long = dish.cookTime
        val newDishesTasks = ArrayList<DishTask>(addingCount)
        repeat(addingCount) {
            newDishesTasks.add(DishTask(dishId = dishId, cookTime = cookTime, orderTask = this, priority = 5))
        }
        dishesTasks.addAll(newDishesTasks)
        return newDishesTasks
    }

    fun onDishStartedCooking(dishTask: DishTask, cookingJob: Job) {
        cookingDishes[dishTask] = cookingJob
    }

    suspend fun onDishReady(dishTask: DishTask) {
        cookingDishes.remove(dishTask)!!
        readyDishesCount.getAndIncrement()
        checkReadiness()
    }

    suspend fun cancelDishCooking(dishId: Int) {
        val dishTaskToCancel: DishTask = dishesTasks.removeAt(dishesTasks.indexOfFirst {
            dishTask: DishTask -> dishTask.dishId == dishId && dishTask.isCooking
        })

        cookingDishes.remove(dishTaskToCancel)!!.cancel()
        checkReadiness()
    }

    private suspend inline fun checkReadiness() {
        if (ready()) {
            scheduler.onOrderReady(this)
        }
    }
}
