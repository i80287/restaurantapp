package restaurant.backend.db.repository

import jakarta.persistence.EntityManager
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.Modifying
import org.springframework.transaction.annotation.Transactional
import restaurant.backend.db.entities.OrderDishEntity
import restaurant.backend.db.entities.OrderEntity
import restaurant.backend.dto.OrderDishDto
import restaurant.backend.dto.OrderDto

open class CustomOrderRepositoryImpl(@Autowired private val entityManager: EntityManager) : CustomOrderRepository {
    companion object {
        private val logger: Logger = org.slf4j.LoggerFactory.getLogger(CustomOrderRepositoryImpl::class.java)
    }

    @Modifying
    @Transactional(rollbackFor = [Throwable::class])
    override fun addOrder(orderDto: OrderDto): OrderEntity {
        val orderEntity: OrderEntity = entityManager
            .createNativeQuery("INSERT INTO orders(user_id) VALUES (${orderDto.orderOwnerId}) RETURNING *;",
                               OrderEntity::class.java)
            .singleResult as OrderEntity
        val orderId: Int = orderEntity.orderId!!
        logger.debug("CustomOrderRepositoryImpl::addOrder() created order with id $orderId\n")

        var count: Int = orderDto.orderDishes.size
        if (count > 0) {
            val valuesBuffer = StringBuilder(maxOf(count * "(0, 0, 0)".length, 16))
            for (orderDishDto: OrderDishDto in orderDto.orderDishes) {
                valuesBuffer.append("($orderId, ${orderDishDto.dishId}, ${orderDishDto.orderedCount})")
                if (--count > 0) {
                    valuesBuffer.append(',')
                }
            }

            val insertedRows: Int = entityManager
                .createNativeQuery("""
                DO $$
                DECLARE tmprow RECORD;
                BEGIN FOR tmprow IN
                INSERT INTO order_dishes(order_id, dish_id, ordered_count) VALUES $valuesBuffer RETURNING *
                LOOP
                UPDATE dishes SET quantity = quantity - tmprow.ordered_count WHERE dish_id = tmprow.dish_id;
                END LOOP;
                END; $$;
                DELETE FROM dishes WHERE quantity <= 0;
                """.trimIndent())
                .executeUpdate()
            logger.debug("Inserted $insertedRows order-dish relation rows for the $orderId")
        }

        return orderEntity
    }
}
