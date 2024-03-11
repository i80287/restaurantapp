package restaurant.backend.services

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import restaurant.backend.db.entities.OrderEntity
import restaurant.backend.db.entities.OrderDishEntity
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

class OrderTask(private val order: OrderEntity, private val scheduler: OrderScheduler) {
    private val cookingDishes = ConcurrentHashMap<DishTask, Job>(order.dishes.size)
    private var readyDishesCount = AtomicInteger()
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
    }

    val orderId: Int = order.orderId!!
    fun readyDishes(): Int = readyDishesCount.get() 
    fun totalDishes(): Int = dishesTasks.size
    fun ready(): Boolean = readyDishes() == totalDishes()

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
