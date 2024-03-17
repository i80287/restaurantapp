package restaurant.backend.db.entities

import jakarta.persistence.*
import lombok.NoArgsConstructor

@Entity
@Table(name = "restaurant_info")
@NoArgsConstructor
data class RestaurantInfoEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false, fetch = FetchType.EAGER)
    @Column(name = "key", unique = true)
    val key: String,
    @Basic(optional = false)
    @Column(name = "value", nullable = false)
    val value: Long,
)
