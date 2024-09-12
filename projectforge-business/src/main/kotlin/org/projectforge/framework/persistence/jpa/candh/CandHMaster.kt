package org.projectforge.framework.persistence.jpa.candh

import mu.KotlinLogging
import org.apache.commons.lang3.ClassUtils
import org.hibernate.Hibernate
import org.hibernate.proxy.HibernateProxy
import org.projectforge.framework.ToStringUtil
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.EntityCopyStatus
import org.projectforge.framework.persistence.api.HibernateUtils
import org.projectforge.framework.persistence.entities.AbstractHistorizableBaseDO
import org.projectforge.framework.persistence.history.HistoryService
import java.io.Serializable
import java.lang.reflect.AccessibleObject
import java.lang.reflect.Field
import java.lang.reflect.Modifier

private val log = KotlinLogging.logger {}

/**
 * Manages copy and history of database objects. Copy is used for merging objects and history is used for tracking changes.
 */
object CandHMaster {
    /**
     * List of all registeredAdapters. For every field the first matching
     */
    private val registeredHandlers = mutableListOf<CandHIHandler>()

    init {
        // Register all handlers here.
        registeredHandlers.add(BigDecimalHandler())
        registeredHandlers.add(SqlDateHandler())
        registeredHandlers.add(UtilDateHandler())
        registeredHandlers.add(CollectionHandler())
        registeredHandlers.add(DefaultHandler()) // Handles everything else.
    }

    fun <IdType : Serializable> copyValues(
        src: BaseDO<IdType>,
        dest: BaseDO<IdType>,
        vararg ignoreFields: String,
    ): CandHContext {
        val context = CandHContext()
        copyValues(src = src, dest = dest, context = context, ignoreFields = ignoreFields)
        return context
    }

    fun <T : Serializable> copyValues(
        src: BaseDO<T>,
        dest: BaseDO<T>,
        context: CandHContext,
        vararg ignoreFields: String
    ) {
        if (!ClassUtils.isAssignable(src.javaClass, dest.javaClass)) {
            throw RuntimeException(
                ("Try to copyValues from different BaseDO classes: this from type "
                        + dest.javaClass.name
                        + " and src from type"
                        + src.javaClass.name
                        + "!")
            )
        }
        Hibernate.initialize(src)
        var useSrc: BaseDO<out Serializable> = src
        if (src is HibernateProxy) {
            useSrc = (src as HibernateProxy).hibernateLazyInitializer
                .implementation as BaseDO<*>
        }
        copyDeclaredFields(
            useSrc.javaClass,
            src = src,
            dest = dest,
            context = context,
            ignoreFields = ignoreFields,
        )
    }

    fun <IdType : Serializable> copyDeclaredFields(
        srcClazz: Class<*>,
        src: BaseDO<IdType>,
        dest: BaseDO<IdType>,
        context: CandHContext,
        vararg ignoreFields: String
    ) {
        context.debugContext?.add(msg = "Processing class $srcClazz")
        val fields = srcClazz.declaredFields
        AccessibleObject.setAccessible(fields, true)
        for (field in fields) {
            val fieldName = field.name
            if (ignoreFields.contains(fieldName)) {
                context.debugContext?.add("$srcClazz.$fieldName", msg = "Ignoring field in list of ignoreFields.")
                continue
            }
            if (!accept(field)) {
                context.debugContext?.add("$srcClazz.$fieldName", msg = "Ignoring field, not accepted.")
                continue
            }
            try {
                val srcFieldValue = field[src]
                val destFieldValue = field[dest]
                var processed = false
                for (handler in registeredHandlers) {
                    if (handler.accept(field)) {
                        if (handler.process(
                                srcClazz = srcClazz,
                                src = src,
                                dest = dest,
                                field = field,
                                fieldName = fieldName,
                                srcFieldValue = srcFieldValue,
                                destFieldValue = destFieldValue,
                                context = context,
                            )
                        ) {
                            processed = true
                            break
                        }
                    }
                }
                if (processed) {
                    // Field was processed by a handler.
                    continue
                } else if (srcFieldValue is BaseDO<*>) {
                    context.debugContext?.add("$srcClazz.$fieldName", msg = "srcFieldValue is BaseDO.")
                    val srcFieldValueId = HibernateUtils.getIdentifier(srcFieldValue)
                    if (srcFieldValueId != null) {
                        if (destFieldValue == null
                            || srcFieldValueId != (destFieldValue as BaseDO<*>).id
                        ) {
                            context.debugContext?.add(
                                "$srcClazz.$fieldName",
                                srcVal = srcFieldValue,
                                destVal = destFieldValue
                            )
                            field[dest] = srcFieldValue
                            setModificationStatusOnChange(context, src, fieldName)
                        }
                    } else {
                        log.error(
                            ("Can't get id though can't copy the BaseDO (see error message above about HHH-3502), or id not given for "
                                    + srcFieldValue.javaClass + ": " + ToStringUtil.toJsonString(srcFieldValue))
                        )
                    }
                } else if (destFieldValue != srcFieldValue) {
                    context.debugContext?.add("$srcClazz.$fieldName", srcVal = srcFieldValue, destVal = destFieldValue)
                    field[dest] = srcFieldValue
                    setModificationStatusOnChange(context, src, fieldName)
                }
            } catch (ex: IllegalAccessException) {
                throw InternalError("Unexpected IllegalAccessException: " + ex.message)
            }
        }
        val superClazz = srcClazz.superclass
        if (superClazz != null) {
            copyDeclaredFields(superClazz, src = src, dest = dest, context = context, ignoreFields = ignoreFields)
        }
    }

    /**
     * Field was modified, so set the modification status to MAJOR or, if not historizable to MINOR.
     */
    internal fun <IdType : Serializable> setModificationStatusOnChange(
        context: CandHContext,
        src: BaseDO<IdType>,
        modifiedField: String
    ) {
        if (HistoryService.get().isNoHistoryProperty(src.javaClass, modifiedField)) {
            // This field is not historized, so no major update:
            context.combine(EntityCopyStatus.MINOR)
            return
        }
        if (context.currentCopyStatus == EntityCopyStatus.MAJOR || src !is AbstractHistorizableBaseDO<*> || src !is HibernateProxy) {
            context.combine(EntityCopyStatus.MAJOR) // equals to context.currentCopyStatus=MAJOR.
        }
        context.combine(EntityCopyStatus.NONE)
    }

    /**
     * Returns whether to append the given `Field`.
     *
     *  * Ignore transient fields
     *  * Ignore static fields
     *  * Ignore inner class fields
     *
     *
     * @param field The Field to test.
     * @return Whether to consider the given `Field`.
     */
    internal fun accept(field: Field): Boolean {
        if (field.name.indexOf(ClassUtils.INNER_CLASS_SEPARATOR_CHAR) != -1) {
            // Reject field from inner class.
            return false
        }
        if (Modifier.isTransient(field.modifiers)) {
            // transients.
            return false
        }
        if (Modifier.isStatic(field.modifiers)) {
            // transients.
            return false
        }
        return true
    }
}
