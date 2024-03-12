package restaurant.backend.util

open class LoggingHelper <T: Any>(clazz: Class<T>) {
    protected val logger: org.slf4j.Logger = org.slf4j.LoggerFactory.getLogger(clazz)

    protected inline fun <reified DtoType : Any, reified ExType: Throwable> debugLogOnIncorrectData(dtoObject: DtoType, methodName: String, ex: ExType) {
        if (logger.isDebugEnabled) {
            logger.debug("Incorrect data $dtoObject in the $methodName\nException: $ex\nStacktrace: ${ex.stackTraceToString()}")
        }
    }

    protected inline fun <reified T : Any> debugLog(loggingObjectFactory: () -> T, methodName: String) {
        if (logger.isDebugEnabled) {
            logger.debug("${loggingObjectFactory()} in the $methodName")
        }
    }

    protected fun debugLog(msg: String, methodName: String) {
        if (logger.isDebugEnabled) {
            logger.debug("$msg in the $methodName")
        }
    }

    protected fun infoLog(msg: String, methodName: String) {
        logger.info("$msg in the $methodName")
    }

    protected inline fun <reified T : Any, reified ExType: Throwable> debugLog(loggingObjectFactory: () -> T, methodName: String, ex: ExType) {
        if (logger.isDebugEnabled) {
            logger.debug("${loggingObjectFactory()} in the $methodName\nException: $ex\nStacktrace: ${ex.stackTraceToString()}")
        }
    }

    protected inline fun <reified ExType: Throwable> errorLog(msg: String, methodName: String, ex: ExType) {
        logger.error("$msg in the $methodName\nException: $ex\nStacktrace: ${ex.stackTraceToString()}")
    }

    protected inline fun <reified ExType: Throwable> errorLog(methodName: String, ex: ExType) {
        logger.error("$methodName\nException: $ex\nStacktrace: ${ex.stackTraceToString()}")
    }

    protected fun errorLog(msg: String, methodName: String) {
        logger.error("$msg in the $methodName")
    }
}
