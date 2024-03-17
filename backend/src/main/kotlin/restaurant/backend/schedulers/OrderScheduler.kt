package restaurant.backend.schedulers

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import restaurant.backend.db.entities.DishEntity
import restaurant.backend.db.entities.OrderEntity
import restaurant.backend.dto.OrderAddDishDto
import restaurant.backend.dto.OrderDeleteDishDto
import restaurant.backend.services.OrderService
import restaurant.backend.util.LoggingHelper
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

class OrderScheduler(private val orderService: OrderService) :
    LoggingHelper<OrderScheduler>(OrderScheduler::class.java) {

    private val cookingOrders = ConcurrentHashMap<Int, OrderTask>()
    private val dishTaskScheduler = DishTaskScheduler()

    private val cookedOrdersChannel: Channel<OrderTask> = Channel()
    private val cookedOrdersHandleThread: Thread = createCookedOrdersThread()

    fun scheduleOrdersOnAppInitialization(orderEntities: List<OrderEntity>): Unit = runBlocking {
        for (order: OrderEntity in orderEntities) {
            if (!order.isReady)
                addOrder(order)
        }
    }

    suspend fun addOrder(order: OrderEntity) {
        val orderTask: OrderTask = OrderTask.createOrderTask(order = order, scheduler = this)
        val orderId = orderTask.orderId
        logDebug(
            { "Adding ${orderTask.dishesTasks.size} dish cooking tasks | order id = $orderId" },
            "OrderScheduler::addOrder()"
        )
        dishTaskScheduler.addAll(orderTask.dishesTasks)
        cookingOrders[orderId] = orderTask
        logDebug({ "Added $orderId to the cookingOrders" }, "OrderScheduler::addOrder()")
    }

    suspend fun addDishesToOrder(dishEntity: DishEntity, orderAddDishDto: OrderAddDishDto) {
        val orderId = orderAddDishDto.orderId
        val orderTask: OrderTask? = cookingOrders[orderId]
        if (orderTask == null) {
            logError(
                "Order task with id $orderId not found",
                "OrderScheduler::addDishesToOrder(DishEntity,OrderAddDishDto)"
            )
            return
        }
        val newDishTasks: ArrayList<DishTask> = orderTask.addDishes(dishEntity, orderAddDishDto.addingCount)
        dishTaskScheduler.addAll(newDishTasks)
    }

    suspend fun cancelDishes(orderDeleteDishDto: OrderDeleteDishDto) {
        val orderId = orderDeleteDishDto.orderId
        val orderTask: OrderTask? = cookingOrders[orderId]
        if (orderTask == null) {
            logError("Order task with id $orderId not found", "OrderScheduler::cancelDishes(OrderDeleteDishDto)")
            return
        }
        orderTask.cancelDishes(orderDeleteDishDto.dishId, orderDeleteDishDto.deletingCount)
    }

    suspend fun deleteOrder(orderId: Int) {
        val orderTask: OrderTask? = cookingOrders.remove(orderId)
        if (orderTask == null) {
            logError("Order task with id $orderId not found", "OrderScheduler::deleteOrder(int)")
            return
        }
        dishTaskScheduler.removeAll(orderTask.dishesTasks)
        orderTask.cancelOrder()
        orderTask.unlock()
    }

    suspend fun onOrderReady(orderTask: OrderTask) {
        cookedOrdersChannel.send(orderTask)
    }

    suspend fun lockOrderIfCanBeChanged(orderId: Int): Boolean {
        val orderTask: OrderTask? = cookingOrders[orderId]
        return orderTask != null && orderTask.lockIfCanBeChanged()
    }

    fun unlockOrder(orderId: Int) {
        cookingOrders[orderId]?.unlock()
    }

    fun notifyFirstOrderDishStartedCooking(order: OrderTask, dish: DishTask) {
        assert(dish.orderTask.orderId == order.orderId)
        logDebug(
            "orderId=${order.orderId}, dishId=${dish.dishId}",
            "OrderScheduler::notifyFirstOrderDishStartedCooking()"
        )
        orderService.notifyFirstOrderDishStartedCooking(order.orderId)
    }

    private fun createCookedOrdersThread(): Thread = thread {
        runBlocking {
            while (this.isActive) {
                try {
                    val cookedOrderTask: OrderTask = cookedOrdersChannel.receive()
                    assert(cookedOrderTask.ready())
                    val orderId: Int = cookedOrderTask.orderId
                    logDebug({ "Removing ready task with id $orderId" }, "createCookedOrdersThread()::runBlocking")
                    if (cookingOrders.remove(orderId) == null) {
                        logError("Incorrect order task $orderId", "createCookedOrdersThread()::runBlocking")
                    }
                    orderService.onReadyOrder(orderId)
                } catch (ex: Throwable) {
                    logError("createCookedOrdersThread()::runBlocking", ex)
                }
            }
        }
    }

    fun destroy() {
        dishTaskScheduler.destroy()

        cookedOrdersChannel.close()
        if (cookedOrdersHandleThread.isAlive) {
            try {
                cookedOrdersHandleThread.interrupt()
            } catch (ex: Throwable) {
                logDebug(
                    { "Could not stop cooking thread ${cookedOrdersHandleThread.name}" },
                    "OrderScheduler::destroy()",
                    ex
                )
            }
        }

        cookingOrders.clear()
    }
}
