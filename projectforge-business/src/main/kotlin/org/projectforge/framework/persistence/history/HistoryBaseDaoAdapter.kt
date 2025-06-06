/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.persistence.history

import org.projectforge.common.AnnotationsUtils
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.IdObject
import org.projectforge.framework.persistence.candh.CandHHistoryEntryICustomizer
import org.projectforge.framework.persistence.entities.AbstractHistorizableBaseDO
import org.projectforge.framework.persistence.jpa.PfPersistenceContext
import java.util.*

/**
 * Utility to provide functionalities for BaseDao.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 * @author Kai Reinhard
 */
object HistoryBaseDaoAdapter {
    fun isHistorizable(bean: Any?): Boolean {
        if (bean == null) {
            return false
        }
        return isHistorizable(bean.javaClass)
    }

    fun isHistorizable(entityClass: Class<*>): Boolean {
        if (AbstractHistorizableBaseDO::class.java.isAssignableFrom(entityClass)) {
            return true
        }
        return AnnotationsUtils.hasClassAnnotation(entityClass, WithHistory::class.java)
    }

    fun createHistoryEntry(entity: IdObject<Long>, opType: EntityOpType): HistoryEntryDO {
        return HistoryEntryDO.create(entity, opType)
    }

    /**
     * Convention: If you want to create a history entry of collections, the oldValue should contain all elements that are removed and the newValue should contain all elements that are added.
     * @param oldValue Supports all types supported by [HistoryValueHandlerRegistry]. Also, collections of objects are supported and will be serialized to a csv string.
     * @param newValue Supports all types supported by [HistoryValueHandlerRegistry]. Also, collections of objects are supported and will be serialized to a csv string.
     */
    fun createHistoryUpdateEntryWithSingleAttribute(
        entity: IdObject<Long>,
        propertyName: String?,
        propertyTypeClass: Class<*>,
        oldValue: Any?,
        newValue: Any?,
    ): HistoryEntryDO {
        val entry = HistoryEntryDO.create(entity, EntityOpType.Update)
        HistoryEntryAttrDO.create(
            propertyTypeClass = propertyTypeClass,
            opType = PropertyOpType.Update,
            oldValue = oldValue,
            newValue = newValue,
            propertyName = propertyName,
            historyEntry = entry,
        )

        return entry
    }

    private fun insertHistoryEntry(
        entity: Any,
        historyEntry: HistoryEntryDO,
        context: PfPersistenceContext,
    ): HistoryEntryDO {
        if (entity is CandHHistoryEntryICustomizer) {
            entity.customize(historyEntry)
        }
        context.insert(historyEntry)
        return historyEntry
    }

    /**
     * Convention: If you want to create a history entry of collections, the oldValue should contain all elements that are removed and the newValue should contain all elements that are added.
     * @param oldValue Supports all types supported by [HistoryValueHandlerRegistry]. Also, collections of objects are supported and will be serialized to a csv string.
     * @param newValue Supports all types supported by [HistoryValueHandlerRegistry]. Also, collections of objects are supported and will be serialized to a csv string.
     */
    fun insertHistoryUpdateEntryWithSingleAttribute(
        entity: IdObject<Long>,
        propertyName: String?,
        propertyTypeClass: Class<*>,
        oldValue: Any?, newValue: Any?,
        context: PfPersistenceContext,
    ): HistoryEntryDO {
        val historyEntry = createHistoryUpdateEntryWithSingleAttribute(
            entity, propertyName, propertyTypeClass, oldValue, newValue,
        )
        return insertHistoryEntry(entity, historyEntry, context)
    }

    fun inserted(obj: BaseDO<Long>, context: PfPersistenceContext) {
        if (!isHistorizable(obj)) {
            // not historizable
            return
        }
        val historyEntry = createHistoryEntry(obj, EntityOpType.Insert)
        insertHistoryEntry(obj, historyEntry, context)
    }

    fun updated(
        obj: BaseDO<Long>,
        historyEntries: List<HistoryEntryDO>?,
        context: PfPersistenceContext
    ) {
        if (!isHistorizable(obj) || historyEntries.isNullOrEmpty()) {
            return
        }
        historyEntries.forEach { entry ->
            insertHistoryEntry(obj, entry, context)
            /*entry.attributes?.forEach { attr ->
                entry.add(attr)
                attr.internalSerializeValueObjects()
                context.insert(attr)
            }*/
        }
    }

    /*
    public static ModificationStatus wrapHistoryUpdate(BaseDO<?> dbo, Supplier<ModificationStatus> callback) {
        final HistoryService historyService = HistoryServiceManager.get().getHistoryService();
        final List<WithHistory> whanots = historyService.internalFindWithHistoryEntity(dbo);
        if (whanots.isEmpty()) {
            return callback.get();
        }
        final List<BaseDO<?>> entitiesToHistoricize = getSubEntitiesToHistoricizeDeep(dbo);
        final PfEmgrFactory emf = ApplicationContextProvider.getApplicationContext().getBean(PfEmgrFactory.class);
        final ModificationStatus result = emf.runInTrans((emgr) -> {
            return wrapHistoryUpdate(emgr, historyService, whanots, dbo, callback);
        });

        //long end = System.currentTimeMillis();
        //log.info("HistoryBaseDaoAdapter.wrappHistoryUpdate took: " + (end - begin) + " ms.");
        return result;
    }

    public static ModificationStatus wrapHistoryUpdate(PfEmgr emgr, BaseDO<?> dbo, Supplier<ModificationStatus> callback) {
        final HistoryService historyService = HistoryServiceManager.get().getHistoryService();
        final List<WithHistory> whanots = historyService.internalFindWithHistoryEntity(dbo);
        if (whanots.isEmpty()) {
            return callback.get();
        }
        return wrapHistoryUpdate(emgr, historyService, whanots, dbo, callback);
    }

    private static ModificationStatus wrapHistoryUpdate(PfEmgr emgr, HistoryService historyService, List<WithHistory> whanots, BaseDO<?> dbo, Supplier<ModificationStatus> callback) {
        final List<BaseDO<?>> entitiesToHistoricize = getSubEntitiesToHistoricizeDeep(dbo);
        final Map<Serializable, HistoryProperties> props = new HashMap<>();

        // get the (old) history properties before the modification
        entitiesToHistoricize.forEach(
                entity -> {
                    final HistoryProperties p = getOrCreateHistoryProperties(props, entity);
                    p.oldProps = historyService.internalGetPropertiesForHistory(emgr, whanots, entity);
                }
        );

        // do the modification
        final ModificationStatus result = callback.get();

        // get the (new) history properties after the modification
        entitiesToHistoricize.forEach(
                entity -> {
                    final HistoryProperties p = getOrCreateHistoryProperties(props, entity);
                    p.newProps = historyService.internalGetPropertiesForHistory(emgr, whanots, entity);
                }
        );

        // create history entries with the diff resulting from the old and new history properties
        props.forEach(
                (pk, p) -> {
                    if (p.oldProps != null && p.newProps != null) {
                        try {
                            historyService.internalOnUpdate(emgr, p.entClassName, pk, p.oldProps, p.newProps);
                        } catch (Exception ex) {
                            log.error("Error while writing history entry (" + p.entClassName + ":" + pk + ", '" + p.oldProps + "'->'" + p.newProps + "': " + ex.getMessage(), ex);
                        }
                    }
                }
        );

        //long end = System.currentTimeMillis();
        //log.info("HistoryBaseDaoAdapter.wrappHistoryUpdate took: " + (end - begin) + " ms.");
        return result;
    }*/
    /**
     * Nested class just to hold some temporary history data.
     * /
     * private static final class HistoryProperties {
     * private String entClassName;
     * private Map<String></String>, HistProp> oldProps;
     * private Map<String></String>, HistProp> newProps;
     * }
     *
     * private static HistoryProperties getOrCreateHistoryProperties(final Map<Serializable></Serializable>, HistoryProperties> props, final DbRecord entity) {
     * final Serializable pk = entity.getPk();
     * if (props.containsKey(pk)) {
     * return props.get(pk);
     * } else {
     * final HistoryProperties hp = new HistoryProperties();
     * props.put(pk, hp);
     * hp.entClassName = entity.getClass().getName();
     * return hp;
     * }
     * }
     *
     * private static List<BaseDO></BaseDO>> getSubEntitiesToHistoricizeDeep(final BaseDO entity) {
     * final List<BaseDO></BaseDO>> result = new ArrayList<>();
     * final Queue<BaseDO></BaseDO>> queue = new LinkedList<>();
     * queue.add(entity);
     *
     * // do a breadth first search through the tree
     * while (!queue.isEmpty()) {
     * final BaseDO head = queue.poll();
     * result.add(head);
     * final List<BaseDO></BaseDO>> subEntries = getSubEntitiesToHistoricize(head);
     * queue.addAll(subEntries);
     * }
     *
     * return result;
     * }
     *
     * / **
     * Takes a DO and returns a list of DOs. This list contains all entries of the collections of the DOs where the class fields have this annotation:
     * "@PFPersistancyBehavior(autoUpdateCollectionEntries = true)".
     *
     * @param entity The DO.
     * @return The List of DOs.
     * /
     * private static List<BaseDO></BaseDO>> getSubEntitiesToHistoricize(final BaseDO entity) {
     * final Collection<Field> fields = ClassUtils.getAllFields(entity.getClass()).values();
     * AccessibleObject.setAccessible(fields.toArray(new Field[0]), true);
     *
     * return fields
     * .stream()
     * .filter(field -> {
     * final PFPersistancyBehavior behavior = field.getAnnotation(PFPersistancyBehavior.class);
     * return behavior != null && behavior.autoUpdateCollectionEntries();
     * })
     * .map(field -> {
     * try {
     * return (Collection<BaseDO></BaseDO>>) field.get(entity);
     * } catch (IllegalAccessException | ClassCastException e) {
     * return (Collection<BaseDO></BaseDO>>) Collections.EMPTY_LIST;
     * }
     * })
     * .flatMap(Collection::stream)
     * .collect(Collectors.toList());
     * }
     *
     * public static void updated(BaseDO oldo, BaseDO newo) {
     * //long begin = System.currentTimeMillis();
     * PfEmgrFactory emf = ApplicationContextProvider.getApplicationContext().getBean(PfEmgrFactory.class);
     * emf.runInTrans((emgr) -> {
     * EmgrUpdateCopyFilterEvent event = new EmgrUpdateCopyFilterEvent(emgr, oldo.getClass(), oldo.getClass(), oldo,
     * newo,
     * true);
     * new HistoryUpdateCopyFilterEventListener().onEvent(event);
     * return null;
     * });
     * //long end = System.currentTimeMillis();
     * //log.info("HistoryBaseDaoAdapter.updated took: " + (end - begin) + " ms.");
     * }
     *
     * public static void markedAsDeleted(ExtendedBaseDO oldo, ExtendedBaseDO newoj) {
     * //long begin = System.currentTimeMillis();
     * boolean prev = newoj.isDeleted;
     * newoj.isDeleted = true;
     * updated(oldo, newoj);
     * newoj.isDeleted = prev;
     * //long end = System.currentTimeMillis();
     * //log.info("HistoryBaseDaoAdapter.markedAsDeleted took: " + (end - begin) + " ms.");
     * }
     *
     * public static void markedAsUnDeleted(ExtendedBaseDO oldo, ExtendedBaseDO newoj) {
     * //long begin = System.currentTimeMillis();
     * boolean prev = newoj.isDeleted;
     * newoj.isDeleted = false;
     * updated(oldo, newoj);
     * newoj.isDeleted = prev;
     * //long end = System.currentTimeMillis();
     * //log.info("HistoryBaseDaoAdapter.markedAsUnDeleted took: " + (end - begin) + " ms.");
     * }
    </Field> */
}
