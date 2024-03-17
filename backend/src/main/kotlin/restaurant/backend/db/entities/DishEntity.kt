package restaurant.backend.db.entities

import jakarta.persistence.*
import lombok.NoArgsConstructor

@Entity
@Table(name = "dishes")
@NoArgsConstructor
data class DishEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false, fetch = FetchType.EAGER)
    @Column(name = "dish_id", unique = true, nullable = false, updatable = false)
    var dishId: Int? = null,

    @Basic(optional = false)
    @Column(name = "name", unique = true, nullable = false)
    val name: String,

    @Basic(optional = false)
    @Column(name = "quantity", nullable = false)
    val quantity: Int,

    @Basic(optional = false)
    @Column(name = "cook_time", nullable = false)
    val cookTime: Long,

    @Basic(optional = false)
    @Column(name = "price", nullable = false, updatable = true)
    val price: Int,
)
