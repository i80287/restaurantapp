package restaurant.backend.util

open class LoggingHelper <T: Any>(clazz: Class<T>) {
    protected val log: org.slf4j.Logger = org.slf4j.LoggerFactory.getLogger(clazz)

    protected inline fun <reified DtoType : Any, reified ExType: Throwable> logDebugOnIncorrectData(dtoObject: DtoType, methodName: String, ex: ExType) {
        if (log.isDebugEnabled) {
            log.debug("Incorrect data $dtoObject in the $methodName\nException: $ex\nStacktrace: ${ex.stackTraceToString()}")
        }
    }

    protected inline fun <reified T : Any> logDebug(lazyMessage: () -> T, methodName: String) {
        if (log.isDebugEnabled) {
            log.debug("${lazyMessage()} in the $methodName")
        }
    }

    protected fun logDebug(msg: String, methodName: String) {
        if (log.isDebugEnabled) {
            log.debug("$msg in the $methodName")
        }
    }

    protected fun logInfo(msg: String, methodName: String) {
        log.info("$msg in the $methodName")
    }

    protected inline fun <reified T : Any, reified ExType: Throwable> logDebug(lazyMessage: () -> T, methodName: String, ex: ExType) {
        if (log.isDebugEnabled) {
            log.debug("${lazyMessage()} in the $methodName\nException: $ex\nStacktrace: ${ex.stackTraceToString()}")
        }
    }

    protected inline fun <reified ExType: Throwable> logError(msg: String, methodName: String, ex: ExType) {
        log.error("$msg in the $methodName\nException: $ex\nStacktrace: ${ex.stackTraceToString()}")
    }

    protected inline fun <reified ExType: Throwable> logError(methodName: String, ex: ExType) {
        log.error("$methodName\nException: $ex\nStacktrace: ${ex.stackTraceToString()}")
    }

    protected fun logError(msg: String, methodName: String) {
        log.error("$msg in the $methodName")
    }
}
