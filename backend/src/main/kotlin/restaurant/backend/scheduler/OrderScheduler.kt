package restaurant.backend.scheduler

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import restaurant.backend.db.entities.DishEntity
import restaurant.backend.db.entities.OrderEntity
import restaurant.backend.db.entities.OrderDishEntity
import restaurant.backend.dto.OrderAddDishDto
import restaurant.backend.dto.OrderDeleteDishDto
import restaurant.backend.services.OrderService
import restaurant.backend.util.LoggingHelper
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.PriorityBlockingQueue
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

const val MAX_COOKING_DISHES_PER_ONE_TIME: Int = 4

class OrderScheduler(private val orderService: OrderService) :
    LoggingHelper<OrderScheduler>(OrderScheduler::class.java) {

    private val cookingThreads = ArrayList<Thread>(MAX_COOKING_DISHES_PER_ONE_TIME)
    private val dishTasks = PriorityBlockingQueue<DishTask>()
    private val cookedOrdersChannel: Channel<OrderTask> = Channel()
    private val cookedOrdersHandleThread: Thread = createCookedOrdersThread()
    private val cookingOrders = ConcurrentHashMap<Int, OrderTask>()

    init {
        repeat(MAX_COOKING_DISHES_PER_ONE_TIME) {
            cookingThreads.add(createDishCookingThread("cookingThread-$it"))
        }
    }

    fun addOrder(order: OrderEntity) {
        val orderTask = OrderTask(order = order, scheduler = this)
        debugLog(
            { "Adding ${orderTask.dishesTasks.size} dish cooking tasks | order id = ${order.orderId}" },
            "OrderScheduler::addOrder()"
        )
        for (dishTask: DishTask in orderTask.dishesTasks) {
            debugLog("Adding new dish task with dish id ${dishTask.dishId}", "OrderScheduler::addOrder()")
            dishTasks.offer(dishTask)
        }
        cookingOrders[order.orderId!!] = orderTask
        debugLog("Added ${order.orderId} to the cookingOrders", "OrderScheduler::addOrder()")
    }

    suspend fun addDishesToOrder(dishEntity: DishEntity, orderAddDishDto: OrderAddDishDto) {
        val orderTask: OrderTask = cookingOrders[orderAddDishDto.orderId]!!
        val newDishTasks: ArrayList<DishTask> = orderTask.addDishes(dishEntity, orderAddDishDto.addingCount)
        for (dishTask: DishTask in newDishTasks) {
            dishTasks.offer(dishTask)
        }
    }

    suspend fun cancelDishes(orderDeleteDishDto: OrderDeleteDishDto) {
        val orderTask: OrderTask = cookingOrders[orderDeleteDishDto.orderId]!!
        orderTask.cancelDishes(orderDeleteDishDto.dishId, orderDeleteDishDto.deletingCount)
    }

    suspend fun onOrderReady(orderTask: OrderTask) {
        cookedOrdersChannel.send(orderTask)
    }

    fun notifyFirstOrderDishStartedCooking(order: OrderTask, dish: DishTask) {
        assert(dish.orderTask.orderId == order.orderId)
        debugLog(
            "orderId=${order.orderId}, dishId=${dish.dishId}",
            "OrderScheduler::notifyFirstOrderDishStartedCooking()"
        )
        orderService.notifyFirstOrderDishStartedCooking(order.orderId)
    }

    private fun createDishCookingThread(threadName: String): Thread = thread(name = threadName) {
        runBlocking {
            while (this.isActive) {
                try {
                    val dishTask: DishTask = dishTasks.take()
                    infoLog(
                        "$threadName started (dishId=${dishTask.dishId}, orderid=${dishTask.orderTask.orderId})",
                        "OrderScheduler::createDishCookingThread()"
                    )
                    dishTask.startCooking(this)
                    infoLog(
                        "$threadName ended (dishId=${dishTask.dishId}, orderid=${dishTask.orderTask.orderId})",
                        "OrderScheduler::createDishCookingThread()"
                    )
                } catch (ex: Throwable) {
                    errorLog("Error cooking new dish", "OrderScheduler::createDishCookingThread()", ex)
                }
            }
            infoLog("$threadName exited", "OrderScheduler::createDishCookingThread()")
        }
    }

    private fun createCookedOrdersThread(): Thread = thread {
        runBlocking {
            while (this.isActive) {
                try {
                    val cookedOrderTask: OrderTask = cookedOrdersChannel.receive()
                    assert(cookedOrderTask.ready())
                    val orderId: Int = cookedOrderTask.orderId
                    debugLog({ "Removing ready task with id $orderId" }, "createCookedOrdersThread()::runBlocking")
                    if (cookingOrders.remove(orderId) == null) {
                        errorLog("Incorrect order task $orderId", "createCookedOrdersThread()::runBlocking")
                    }
                    orderService.onReadyOrder(orderId)
                } catch (ex: Throwable) {
                    errorLog("createCookedOrdersThread()::runBlocking", ex)
                }
            }
        }
    }

    fun destroy() {
        dishTasks.clear()

        for (cookingThread: Thread in cookingThreads) {
            if (cookingThread.isAlive) {
                try {
                    cookingThread.interrupt()
                } catch (ex: Throwable) {
                    debugLog(
                        { "Could not stop cooking thread ${cookingThread.name}" },
                        "OrderScheduler::destroy()",
                        ex
                    )
                }
            }
        }

        cookedOrdersChannel.close()
        if (cookedOrdersHandleThread.isAlive) {
            try {
                cookedOrdersHandleThread.interrupt()
            } catch (ex: Throwable) {
                debugLog(
                    { "Could not stop cooking thread ${cookedOrdersHandleThread.name}" },
                    "OrderScheduler::destroy()",
                    ex
                )
            }
        }

        cookingOrders.clear()
    }
}
