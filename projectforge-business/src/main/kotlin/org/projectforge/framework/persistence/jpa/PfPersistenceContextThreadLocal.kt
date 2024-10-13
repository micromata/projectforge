package org.projectforge.framework.persistence.jpa

/**
 * ThreadLocal for [PfPersistenceContext].
 * It is used to store context of current transaction or readonly operation.
 * If any method is using [PfPersistenceContext] and it is not passed as parameter, it will be get automatically by
 * [PfPersistenceService] for re-using contexts (readonly as well as transactional ones).
 * For readonly operations, any existing context (readonly or transactional) will be used.
 */
internal object PfPersistenceContextThreadLocal {
    private val threadLocalReadonly = ThreadLocal<PfPersistenceContext?>()
    private val threadLocalTransactional = ThreadLocal<PfPersistenceContext?>()

    /**
     * Gets context of ThreadLocal with transaction, if exists, or readonly context, if exists. Null, if no context exist.
     */
    fun get(): PfPersistenceContext? {
        return getTransactional() ?: getReadonly()
    }

    /**
     * Gets readonly context of ThreadLocal, if exists. Null, if no context exist.
     */
    fun getReadonly(): PfPersistenceContext? {
        return threadLocalReadonly.get()
    }

    /**
     * Gets transactional context of ThreadLoca, if exists. Null, if no context exist.
     */
    fun getTransactional(): PfPersistenceContext? {
        return threadLocalTransactional.get()
    }

    /**
     * Sets readonly context of ThreadLocal.
     */
    fun setReadonly(context: PfPersistenceContext) {
        require(context.type == PfPersistenceContext.ContextType.READONLY) { "Context must be of type READONLY." }
        threadLocalReadonly.set(context)
    }

    /**
     * Sets transactional context of ThreadLocal.
     */
    fun setTransactional(context: PfPersistenceContext) {
        require(context.type == PfPersistenceContext.ContextType.TRANSACTION) { "Context must be of type TRANSACTION." }
        threadLocalTransactional.set(context)
    }

    /**
     * Removes readonly context of ThreadLocal, if exists.
     */
    fun removeReadonly() {
        threadLocalReadonly.remove()
    }

    /**
     * Removes transactional context of ThreadLocal, if exists.
     */
    fun removeTransactional() {
        threadLocalTransactional.remove()
    }
}
