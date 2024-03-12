package restaurant.backend.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import restaurant.backend.db.entities.DishEntity
import restaurant.backend.db.entities.OrderDishEntity
import restaurant.backend.db.entities.OrderEntity
import restaurant.backend.db.repository.OrderRepository
import restaurant.backend.dto.OrderAddDishDto
import restaurant.backend.dto.OrderDeleteDishDto
import restaurant.backend.dto.OrderDto
import restaurant.backend.scheduler.OrderScheduler
import restaurant.backend.util.LoggingHelper
import restaurant.backend.util.PaidOrderStatus
import java.util.*

@Service
class OrderService @Autowired constructor(
    private val orderRepository: OrderRepository
) : LoggingHelper<OrderService>(OrderService::class.java), DisposableBean {

    private final val orderScheduler = OrderScheduler(this)

    final fun retrieveAllOrders(): List<OrderDto> = orderRepository.findAll().map { order: OrderEntity -> OrderDto(order) }

    final fun retrieveOrderById(orderId: Int): OrderDto? {
        val orderEntity: Optional<OrderEntity> = orderRepository.findById(orderId)
        return when {
            orderEntity.isPresent -> OrderDto(orderEntity.get())
            else -> null
        }
    }

    final fun tryAddOrder(orderDto: OrderDto): Int? {
        val order: OrderEntity = try {
            orderRepository.addOrder(orderDto)
        } catch (ex: RuntimeException) {
            errorLog("OrderService::tryAddOrder(OrderDto)", ex)
            return null
        } catch (ex: Throwable) {
            // Incorrect data from the user
            debugLogOnIncorrectData(orderDto, "OrderService::tryAddOrder(OrderDto)", ex)
            return null
        }

        val orderId: Int = order.orderId!!
        try {
            orderScheduler.addOrder(order)
            return orderId
        } catch (ex: Throwable) {
            orderRepository.deleteById(orderId)
            logger.error("Internal error in the OrderScheduler::addOrder(OrderEntity)\nCould not add order $order\nException: $ex\n")
            return null
        }
    }

    final fun onReadyOrder(orderId: Int) {
        orderRepository.setOrderReady(orderId)
    }

    final suspend fun tryAddDishToOrder(orderAddDishDto: OrderAddDishDto): Boolean {
        val updatedOrderEntity: OrderEntity = try {
            withContext(Dispatchers.IO) {
                orderRepository.addDishToOrder(orderAddDishDto)
            } ?: return false
        } catch (ex: Throwable) {
            debugLogOnIncorrectData(orderAddDishDto, "OrderService::tryAddDishToOrder(OrderAddDishDto)", ex)
            return false
        }

        val dishEntity: DishEntity = updatedOrderEntity
            .dishes
            .find { orderDishEntity: OrderDishEntity -> orderDishEntity.dishId == orderAddDishDto.dishId }!!
            .dish!!
        return try {
            orderScheduler.addDishesToOrder(dishEntity, orderAddDishDto)
            true
        } catch (ex: UnsupportedOperationException) {
            false
        } catch (ex: Throwable) {
            errorLog("OrderService::tryAddDishToOrder(OrderAddDishDto)", ex)
            false
        }
    }

    final suspend fun deleteDishFromOrder(orderDeleteDishDto: OrderDeleteDishDto): Boolean {
        try {
            val deletedSuccessfully: Boolean = withContext(Dispatchers.IO) {
                orderRepository.deleteDishFromOrder(orderDeleteDishDto)
            }
            if (!deletedSuccessfully) {
                return false
            }
        } catch (ex: Throwable) {
            debugLogOnIncorrectData(orderDeleteDishDto, "OrderService::deleteDishFromOrder(OrderDeleteDishDto)", ex)
            return false
        }

        try {
            orderScheduler.cancelDishes(orderDeleteDishDto)
        } catch (ex: Throwable) {
            errorLog( "OrderService::deleteDishFromOrder(OrderDeleteDishDto)", ex)
        }
        return true
    }

    final fun onOrderPaid(orderId: Int): PaidOrderStatus {
        try {
            val order: OrderEntity = orderRepository.findByIdOrNull(orderId)
                ?: return PaidOrderStatus.ORDER_DOES_NOT_EXIST
            if (!order.isReady) {
                return PaidOrderStatus.ORDER_IS_NOT_READY
            }
            orderRepository.onReadyOrderPaid(order)
            return PaidOrderStatus.OK
        } catch (ex: Throwable) {
            debugLogOnIncorrectData("(id=$orderId)", "OrderService::onPaidOrder(PaidOrderDto)", ex)
            return PaidOrderStatus.OTHER_ERROR
        }
    }

    final fun notifyFirstOrderDishStartedCooking(orderId: Int) {
        orderRepository.setOrderStartedCooking(orderId)
    }

    override fun destroy() {
        orderScheduler.destroy()
    }
}
