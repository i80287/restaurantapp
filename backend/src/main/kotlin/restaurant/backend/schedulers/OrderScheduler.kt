package restaurant.backend.schedulers

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import restaurant.backend.db.entities.DishEntity
import restaurant.backend.db.entities.OrderEntity
import restaurant.backend.dto.OrderAddDishDto
import restaurant.backend.dto.OrderDeleteDishDto
import restaurant.backend.schedulers.OrderTask
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

    fun scheduleOrdersOnAppInitialization(orderEntities: MutableList<OrderEntity>): Unit = runBlocking {
        for (order: OrderEntity in orderEntities) {
            if (!order.isReady)
                addOrder(order)
        }
    }

    suspend fun addOrder(order: OrderEntity) {
        val orderTask: OrderTask = OrderTask.createOrderTask(order = order, scheduler = this)
        debugLog(
            { "Adding ${orderTask.dishesTasks.size} dish cooking tasks | order id = ${order.orderId}" },
            "OrderScheduler::addOrder()"
        )
        for (dishTask: DishTask in orderTask.dishesTasks) {
            debugLog("Adding new dish task with dish id ${dishTask.dishId}", "OrderScheduler::addOrder()")
            dishTaskScheduler.offer(dishTask)
        }
        cookingOrders[order.orderId!!] = orderTask
        debugLog({ "Added ${order.orderId} to the cookingOrders" }, "OrderScheduler::addOrder()")
    }

    suspend fun addDishesToOrder(dishEntity: DishEntity, orderAddDishDto: OrderAddDishDto) {
        val orderTask: OrderTask = cookingOrders[orderAddDishDto.orderId]!!
        val newDishTasks: ArrayList<DishTask> = orderTask.addDishes(dishEntity, orderAddDishDto.addingCount)
        for (dishTask: DishTask in newDishTasks) {
            dishTaskScheduler.offer(dishTask)
        }
    }

    suspend fun cancelDishes(orderDeleteDishDto: OrderDeleteDishDto) {
        cookingOrders[orderDeleteDishDto.orderId]!!
            .cancelDishes(orderDeleteDishDto.dishId, orderDeleteDishDto.deletingCount)
    }

    suspend fun deleteOrder(orderId: Int) {
        val orderTask: OrderTask = cookingOrders.remove(orderId) ?: return
        dishTaskScheduler.removeAll(orderTask.dishesTasks)
        orderTask.cancelOrder()
    }

    suspend fun onOrderReady(orderTask: OrderTask) {
        cookedOrdersChannel.send(orderTask)
    }

    suspend fun lockOrderIfCanBeChanged(orderId: Int): Boolean {
        val orderTask: OrderTask? = cookingOrders[orderId]
        return orderTask != null && orderTask.lockIfCanBeChanged()
    }

    fun unlockOrder(orderId: Int): Unit = cookingOrders[orderId]!!.unlock()

    fun notifyFirstOrderDishStartedCooking(order: OrderTask, dish: DishTask) {
        assert(dish.orderTask.orderId == order.orderId)
        debugLog(
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
        dishTaskScheduler.destroy()
    }
}
