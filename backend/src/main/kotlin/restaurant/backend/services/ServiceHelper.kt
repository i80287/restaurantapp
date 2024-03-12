package restaurant.backend.services

open class ServiceHelper <T: Any>(clazz: Class<T>) {
    protected val logger: org.slf4j.Logger = org.slf4j.LoggerFactory.getLogger(clazz)

    protected inline fun <reified DtoType, reified ExType: Throwable> debugLogOnIncorrectData(dtoObject: DtoType, methodName: String, ex: ExType) {
        if (logger.isDebugEnabled)
            logger.debug("Incorrect input data $dtoObject in the $methodName\nException: $ex\nStacktrace: ${ex.stackTraceToString()}\n")
    }
}
