package restaurant.backend.schedulers

class PriorityUpdateEntry(var lastTimeUpdatedMillis: Long, val dishTask: DishTask)
    : Comparable<PriorityUpdateEntry> {
    override fun compareTo(other: PriorityUpdateEntry): Int {
        return lastTimeUpdatedMillis.compareTo(other.lastTimeUpdatedMillis)
    }

    override fun equals(other: Any?): Boolean = other is PriorityUpdateEntry && dishTask == other.dishTask

    override fun hashCode(): Int = dishTask.hashCode()
}
