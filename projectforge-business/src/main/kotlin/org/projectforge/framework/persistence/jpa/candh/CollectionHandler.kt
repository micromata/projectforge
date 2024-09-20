/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.framework.persistence.jpa.candh

import jakarta.persistence.JoinColumn
import mu.KotlinLogging
import org.hibernate.collection.spi.PersistentSet
import org.projectforge.common.AnnotationsUtils
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.PFPersistancyBehavior
import org.projectforge.framework.persistence.history.NoHistory
import org.projectforge.framework.persistence.jpa.candh.CandHMaster.copyValues
import org.projectforge.framework.persistence.jpa.candh.CandHMaster.setModificationStatusOnChange
import java.io.Serializable
import java.lang.reflect.Field
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * Used for mutable collections. TreeSet, ArrayList, HashSet and PersistentSet are supported.
 */
open class CollectionHandler : CandHIHandler {
    override fun accept(field: Field): Boolean {
        return Collection::class.java.isAssignableFrom(field.type)
    }

    override fun <IdType : Serializable> process(
        srcClazz: Class<*>,
        src: BaseDO<IdType>,
        dest: BaseDO<IdType>,
        field: Field,
        fieldName: String,
        srcFieldValue: Any?,
        destFieldValue: Any?,
        context: CandHContext,
    ): Boolean {
        val srcFieldCollection = srcFieldValue as? MutableCollection<Any?>
        var destFieldCollection = destFieldValue as? MutableCollection<Any?>
        if (srcFieldCollection.isNullOrEmpty() && destFieldCollection.isNullOrEmpty()) {
            // Both collections are null or empty, so nothing to do.
            return true
        }
        if (srcFieldCollection.isNullOrEmpty()) {
            field[dest] = null
            context.debugContext?.add(
                "$srcClazz.$fieldName",
                srcVal = srcFieldValue,
                destVal = "<not empty collection>"
            )
            if (collectionManagedBySrcClazz(srcClazz, fieldName)) {
                context.addHistoryEntry(fieldName, "", "collection removed")
            } else {
                context.addHistoryEntry(fieldName, "", "collection removed")
            }
            setModificationStatusOnChange(context, src, fieldName)
            return true
        }
        val toRemove = mutableListOf<Any>()
        if (destFieldCollection == null) {
            if (srcFieldValue is TreeSet<*>) {
                destFieldCollection = TreeSet()
                context.debugContext?.add(
                    "$srcClazz.$fieldName",
                    srcVal = srcFieldValue,
                    msg = "Creating TreeSet as destFieldValue.",
                )
            } else if (srcFieldValue is HashSet<*>) {
                destFieldCollection = HashSet()
                context.debugContext?.add(
                    "$srcClazz.$fieldName",
                    srcVal = srcFieldValue,
                    msg = "Creating HashSet as destFieldValue.",
                )
            } else if (srcFieldValue is List<*>) {
                destFieldCollection = ArrayList()
                context.debugContext?.add(
                    "$srcClazz.$fieldName",
                    srcVal = srcFieldValue,
                    msg = "Creating ArrayList as destFieldValue.",
                )
            } else if (srcFieldValue is PersistentSet<*>) {
                destFieldCollection = HashSet()
                context.debugContext?.add(
                    "$srcClazz.$fieldName",
                    srcVal = srcFieldValue,
                    msg = "Creating HashSet as destFieldValue. srcFieldValue is PersistentSet.",
                )
            } else {
                log.error("Unsupported collection type: " + srcFieldValue.javaClass.name)
                return true
            }
            field[dest] = destFieldCollection
        }
        destFieldCollection.filterNotNull().forEach { destColEntry ->
            if (srcFieldValue.none { it == destColEntry }) {
                toRemove.add(destColEntry)
            }
        }
        toRemove.forEach { removeEntry ->
            log.debug { "Removing collection entry: $removeEntry" }
            destFieldCollection.remove(removeEntry)
            context.debugContext?.add(
                "$srcClazz.$fieldName",
                msg = "Removing entry $removeEntry from destFieldValue.",
            )
            setModificationStatusOnChange(context, src, fieldName)
        }
        srcFieldValue.forEach { srcCollEntry ->
            if (!destFieldCollection.contains(srcCollEntry)) {
                log.debug { "Adding new collection entry: $srcCollEntry" }
                destFieldCollection.add(srcCollEntry)
                context.debugContext?.add(
                    "$srcClazz.$fieldName",
                    msg = "Adding entry $srcCollEntry to destFieldValue.",
                )
                setModificationStatusOnChange(context, src, fieldName)
            } else if (srcCollEntry is BaseDO<*>) {
                val behavior = field.getAnnotation(PFPersistancyBehavior::class.java)
                context.debugContext?.add(
                    "$srcClazz.$fieldName",
                    msg = "srcEntry of src-collection is BaseDO. autoUpdateCollectionEntres = ${behavior?.autoUpdateCollectionEntries == true}"
                )
                if (behavior != null && behavior.autoUpdateCollectionEntries) {
                    var destEntry: BaseDO<*>? = null
                    for (entry in destFieldCollection) {
                        if (entry == srcCollEntry) {
                            destEntry = entry as BaseDO<*>
                            break
                        }
                    }
                    requireNotNull(destEntry)
                    copyValues(
                        srcCollEntry as BaseDO<Serializable>,
                        destEntry as BaseDO<Serializable>,
                        context
                    )
                }
            }
        }
        return true
    }

    /**
     * If collection is declared as OneToMany and not marked as @NoHistory, the collection is managed by the source class.
     */
    private fun collectionManagedBySrcClazz(srcClazz: Class<*>, fieldName: String): Boolean {
        val annotations = AnnotationsUtils.getAnnotations(srcClazz, fieldName)
        if (annotations.any { it.annotationClass == JoinColumn::class } &&
            annotations.none { it.annotationClass == NoHistory::class }) {
            return true
        }
        return false
    }
}
