package restaurant.backend.schedulers

import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import restaurant.backend.db.entities.DishEntity
import restaurant.backend.db.entities.OrderEntity
import restaurant.backend.db.entities.OrderDishEntity
import restaurant.backend.schedulers.DishTask
import restaurant.backend.schedulers.OrderScheduler
import java.lang.UnsupportedOperationException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class OrderTask private constructor(order: OrderEntity, private val scheduler: OrderScheduler) {
    companion object {
        fun createOrderTask(order: OrderEntity, scheduler: OrderScheduler): OrderTask {
            val orderTask = OrderTask(order, scheduler)
            orderTask.createDishTasks(order.dishes)
            return orderTask
        }
    }

    private val cookingDishes = ConcurrentHashMap<DishTask, Job>(order.dishes.size)
    private val readyDishesCount = AtomicInteger()
    private val totalDishesCount = AtomicInteger()
    private val thisOrderStartedCooking = AtomicBoolean()
    private val sendingReadySignal = AtomicBoolean()
    private val orderCancelled = AtomicBoolean()
    private val stateUpdatingMutex: Mutex = Mutex()
    private val dishTaskUniqueIdSeq = AtomicInteger()

    val dishesTasks = ArrayList<DishTask>(order.dishes.size)

    val orderId: Int = order.orderId!!
    fun ready(): Boolean = readyDishesCount() == totalDishesCount()
    private fun readyDishesCount(): Int = readyDishesCount.get()
    private fun totalDishesCount(): Int = totalDishesCount.get()

    suspend fun lockIfCanBeChanged(): Boolean {
        if (sendingReadySignal.get()) {
            return false
        }
        stateUpdatingMutex.lock()
        if (sendingReadySignal.get()) {
            stateUpdatingMutex.unlock()
            return false
        }
        return true
    }

    fun unlock() {
        stateUpdatingMutex.unlock()
    }

    fun addDishes(dish: DishEntity, addingCount: Int): ArrayList<DishTask> {
        throwIfCancelled()
        throwIfSendingReadySignal()
        throwIfNotLocked()
        assert(addingCount > 0)
        // Prevent ready() from being true while adding new dishes
        totalDishesCount.getAndAdd(addingCount)
        val dishId: Int = dish.dishId!!
        val cookTime: Long = dish.cookTime
        val newDishesTasks = ArrayList<DishTask>(addingCount)
        repeat(addingCount) {
            newDishesTasks.add(
                DishTask.createTask(
                    dishTaskOrderUniqueId = nextDishTaskUniqueId(),
                    dishId = dishId,
                    cookTime = cookTime,
                    orderTask = this
                )
            )
        }
        dishesTasks.addAll(newDishesTasks)
        assert(dishesTasks.size == totalDishesCount())
        return newDishesTasks
    }

    suspend fun onDishStartedCooking(dishTask: DishTask, cookingJob: Job) {
        throwIfCancelled()
        // Dish started cooking just now so we couldn't send ready signal
        assert(!sendingReadySignal.get())
        assert(dishTask.isCooking.get())
        assert(!dishTask.isCooked.get())
        if (dishTask.isDishTaskCancelled()) {
            return
        }
        stateUpdatingMutex.withLock {
            if (dishTask.isDishTaskCancelled()) {
                return
            }

            val firstDishStartedCooking: Boolean = !thisOrderStartedCooking.getAndSet(true)
            cookingDishes[dishTask] = cookingJob
            if (firstDishStartedCooking) {
                scheduler.notifyFirstOrderDishStartedCooking(this, dishTask)
            }
        }
    }

    suspend fun onDishReady(dishTask: DishTask) {
        // Dish cooked just now so we couldn't send ready signal
        assert(!sendingReadySignal.get())
        assert(!dishTask.isCooking.get())
        assert(dishTask.isCooked.get())
        if (dishTask.isDishTaskCancelled()) {
            return
        }
        stateUpdatingMutex.withLock {
            if (dishTask.isDishTaskCancelled()) {
                return
            }

            cookingDishes.remove(dishTask)
            readyDishesCount.getAndIncrement()
            checkReadiness()
        }
    }

    suspend fun cancelDishes(dishId: Int, deletingCount: Int) {
        throwIfCancelled()
        throwIfSendingReadySignal()
        throwIfNotLocked()
        assert(0 < deletingCount && deletingCount <= totalDishesCount())
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
            if (dishTask.isCooked.get()) {
                readyDishesCount.getAndDecrement()
            }
            if (dishTask.isCooking.get()) {
                cookingDishes.remove(dishTask)!!.cancel()
            }
            dishTask.cancelDishTask()
        }

        assert(dishesTasks.size == totalDishesCount())
        checkReadiness()
    }

    fun cancelOrder() {
        throwIfCancelled()
        throwIfSendingReadySignal()
        throwIfNotLocked()
        orderCancelled.set(true)
        cancelAllDishes()
    }

    private fun cancelAllDishes() {
        throwIfSendingReadySignal()
        throwIfNotLocked()
        for (dishTask: DishTask in dishesTasks) {
            dishTask.cancelDishTask()
        }
        for (dishCookingJob: Job in cookingDishes.values) {
            if (dishCookingJob.isActive && !dishCookingJob.isCancelled) {
                dishCookingJob.cancel()
            }
        }
        cookingDishes.clear()
        readyDishesCount.set(0)
        totalDishesCount.set(0)
        dishesTasks.clear()
    }

    private suspend inline fun checkReadiness() {
        throwIfSendingReadySignal()
        throwIfNotLocked()
        if (ready()) {
            sendingReadySignal.set(true)
            scheduler.onOrderReady(this)
        }
    }

    private fun throwIfSendingReadySignal() {
        if (sendingReadySignal.get()) {
            throw UnsupportedOperationException("Can not change order task state because order is being processed as ready")
        }
    }

    private fun throwIfNotLocked() {
        if (!stateUpdatingMutex.isLocked) {
            throw UnsupportedOperationException("Can not change order task state because order lock is not locked")
        }
    }

    private fun throwIfCancelled() {
        if (orderCancelled.get()) {
            throw UnsupportedOperationException("Can not change order task state because order is cancelled")
        }
    }

    private fun assert(expression: Boolean) {
        if (!expression) {
            throw InternalError("Implementation error")
        }
    }

    private fun nextDishTaskUniqueId(): Int = dishTaskUniqueIdSeq.getAndIncrement()

    override fun equals(other: Any?): Boolean =
        other is OrderTask &&
                other.orderId == orderId

    override fun hashCode(): Int = orderId

    private fun createDishTasks(dishes: MutableList<OrderDishEntity>) {
        var totalDishes = 0
        for (dish: OrderDishEntity in dishes) {
            val dishId: Int = dish.dishId
            val cookTime: Long = dish.dish!!.cookTime
            val orderedCount = dish.orderedCount
            assert(orderedCount > 0)
            totalDishes += orderedCount
            repeat(orderedCount) {
                dishesTasks.add(
                    DishTask.createTask(
                        dishTaskOrderUniqueId = nextDishTaskUniqueId(),
                        dishId = dishId,
                        cookTime = cookTime,
                        orderTask = this
                    )
                )
            }
        }
        totalDishesCount.getAndAdd(totalDishes)
        assert(dishesTasks.size == totalDishesCount())
    }
}
