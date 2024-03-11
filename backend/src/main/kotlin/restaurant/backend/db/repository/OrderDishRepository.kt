package restaurant.backend.db.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import restaurant.backend.db.entities.OrderDishEntity

@Repository
interface OrderDishRepository : JpaRepository<OrderDishEntity, Long>
