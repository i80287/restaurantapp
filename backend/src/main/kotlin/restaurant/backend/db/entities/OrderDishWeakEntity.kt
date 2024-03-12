package restaurant.backend.db.entities

import jakarta.persistence.*
import lombok.NoArgsConstructor

@Entity
@Table(name = "order_dishes")
@NoArgsConstructor
class OrderDishWeakEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false, fetch = FetchType.EAGER)
    @Column(name = "relation_id", unique = true, updatable = false, insertable = false)
    var relationId: Long,

    @Basic(optional = false)
    @Column(name = "order_id", nullable = false, updatable = false)
    var orderId: Int,

    @Basic(optional = false)
    @Column(name = "dish_id", nullable = false, updatable = false)
    var dishId: Int,

    @Basic(optional = false)
    @Column(name = "ordered_count", nullable = false)
    var orderedCount: Int
)
