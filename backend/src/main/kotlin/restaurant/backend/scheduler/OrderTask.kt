package restaurant.backend.scheduler

import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import restaurant.backend.db.entities.DishEntity
import restaurant.backend.db.entities.OrderEntity
import restaurant.backend.db.entities.OrderDishEntity
import java.lang.UnsupportedOperationException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class OrderTask(order: OrderEntity, private val scheduler: OrderScheduler) {
    private val cookingDishes = ConcurrentHashMap<DishTask, Job>(order.dishes.size)
    private val readyDishesCount = AtomicInteger()
    private val totalDishesCount = AtomicInteger()
    private val thisOrderStartedCooking = AtomicBoolean()
    private val sendingReadySignal = AtomicBoolean()
    private val stateUpdatingMutex: Mutex = Mutex()

    val dishesTasks = ArrayList<DishTask>(order.dishes.size)

    init {
        for (dish: OrderDishEntity in order.dishes) {
            val dishId: Int = dish.dishId
            val cookTime: Long = dish.dish!!.cookTime
            repeat(dish.orderedCount) {
                dishesTasks.add(
                    DishTask(dishId = dishId,
                                         cookTime = cookTime,
                                         orderTask = this,
                                         priority = 5)
                )   
            }
        }
        totalDishesCount.getAndAdd(order.dishes.size)
    }

    val orderId: Int = order.orderId!!
    fun readyDishesCount(): Int = readyDishesCount.get()
    fun totalDishesCount(): Int = totalDishesCount.get()
    fun ready(): Boolean = readyDishesCount() == totalDishesCount()

    suspend fun addDishes(dish: DishEntity, addingCount: Int): ArrayList<DishTask> {
        if (sendingReadySignal.get()) {
            throw UnsupportedOperationException("Can not add dishes to the order that is being processed as ready")
        }

        assert(addingCount > 0)
        stateUpdatingMutex.withLock {
            // Prevent ready() from being true while adding new dishes
            totalDishesCount.getAndAdd(addingCount)
            val dishId: Int = dish.dishId!!
            val cookTime: Long = dish.cookTime
            val newDishesTasks = ArrayList<DishTask>(addingCount)
            repeat(addingCount) {
                newDishesTasks.add(DishTask(dishId = dishId, cookTime = cookTime, orderTask = this, priority = 5))
            }
            dishesTasks.addAll(newDishesTasks)
            return newDishesTasks
        }
    }

    fun onDishStartedCooking(dishTask: DishTask, cookingJob: Job) {
        val firstDishStartedCooking: Boolean = !thisOrderStartedCooking.getAndSet(true)
        cookingDishes[dishTask] = cookingJob
        if (firstDishStartedCooking) {
            scheduler.notifyFirstOrderDishStartedCooking(this, dishTask)
        }
    }

    suspend fun onDishReady(dishTask: DishTask) {
        assert(dishTask.isCooked)
        cookingDishes.remove(dishTask)!!
        readyDishesCount.getAndIncrement()
        checkReadiness()
    }

    suspend fun cancelDishes(dishId: Int, deletingCount: Int) {
        if (sendingReadySignal.get()) {
            throw UnsupportedOperationException("Can not remove dishes to the order that is being processed as ready")
        }
        
        assert(0 < deletingCount && deletingCount <= totalDishesCount())
        stateUpdatingMutex.withLock {
            // Prevent ready() from being true while adding new dishes
            totalDishesCount.getAndAdd(-deletingCount)

            val tasks: ArrayList<DishTask> = dishesTasks
            var i = 0
            var deletingLimit: Int = deletingCount
            while (deletingLimit > 0 && i < tasks.size) {
                if (tasks[i].dishId != dishId) {
                    i++
                    continue
                }
                deletingLimit--
                val dishTask: DishTask = tasks.removeAt(i)
                if (dishTask.isCooked) {
                    readyDishesCount.getAndDecrement()
                }
                if (dishTask.isCooking) {
                    cookingDishes.remove(dishTask)!!.cancel()
                }
            }
        }

        checkReadiness()
    }

    private suspend inline fun checkReadiness() {
        if (ready()) {
            stateUpdatingMutex.withLock {
                if (ready()) {
                    sendingReadySignal.set(true)
                    scheduler.onOrderReady(this)
                }
            }
        }
    }
}
