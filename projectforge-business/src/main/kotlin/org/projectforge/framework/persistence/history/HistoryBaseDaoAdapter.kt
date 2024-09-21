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

package org.projectforge.framework.persistence.history

import de.micromata.hibernate.history.delta.PropertyDelta
import de.micromata.hibernate.history.delta.SimplePropertyDelta
import mu.KotlinLogging
import org.projectforge.business.user.UserGroupCache
import org.projectforge.common.mgc.ClassUtils
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.entities.AbstractHistorizableBaseDO
import org.projectforge.framework.persistence.history.HistoryService.Companion.instance
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.stream.Collectors

private val log = KotlinLogging.logger {}

/**
 * Utility to provide compat with BaseDao.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
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
        val whl = ClassUtils.findClassAnnotations(
            entityClass,
            WithHistory::class.java
        )
        return whl.isEmpty() == false
    }


    private val HISTORY_ARR_TEMPL = arrayOf<HistoryEntry<*>>()

    /*
    fun getHistoryFor(obj: BaseDO<Long>): Array<HistoryEntry<*>> {
        //long begin = System.currentTimeMillis();
        val result = getHistoryEntries(obj).toArray<HistoryEntry<*>>(HISTORY_ARR_TEMPL)
        //long end = System.currentTimeMillis();
        //log.info("HistoryBaseDaoAdapter.getHistoryFor took: " + (end - begin) + " ms.");
        return result
    }*/

    fun getHistoryEntries(ob: BaseDO<Long>): List<HistoryEntry<*>> {
        //long begin = System.currentTimeMillis();xx
        val histservice = instance
        val ret: List<HistoryEntry<*>> = histservice.loadHistory(ob)
        val nret = ret.stream()
            .sorted { e1: HistoryEntry<*>, e2: HistoryEntry<*> -> e2.modifiedAt!!.compareTo(e1.modifiedAt) }
            .collect(Collectors.toList())
        //long end = System.currentTimeMillis();
        //log.info("HistoryBaseDaoAdapter.getHistoryEntries took: " + (end - begin) + " ms.");
        return nret
    }

    fun diffEntryToPropertyDelta(de: DiffEntry): PropertyDelta {
        //long begin = System.currentTimeMillis();
        val ret = SimplePropertyDelta(
            de.propertyName, String::class.java, de.oldValue,
            de.newValue
        )
        //long end = System.currentTimeMillis();
        //log.info("HistoryBaseDaoAdapter.diffEntryToPropertyDelta took: " + (end - begin) + " ms.");
        return ret
    }

    fun getSimpleHistoryEntries(ob: BaseDO<Long>, userGroupCache: UserGroupCache?): List<SimpleHistoryEntry> {
        //long begin = System.currentTimeMillis();
        val ret: MutableList<SimpleHistoryEntry> = ArrayList()
        val hel = getHistoryEntries(ob)

        for (he in hel) {
            val deltas = he.diffEntries
            if (deltas!!.isEmpty()) {
                val se = SimpleHistoryEntry(userGroupCache, he)
                ret.add(se)
            } else {
                for (de in deltas) {
                    val se = SimpleHistoryEntry(userGroupCache, he, diffEntryToPropertyDelta(de))
                    ret.add(se)
                }
            }
        }
        //long end = System.currentTimeMillis();
        //log.info("HistoryBaseDaoAdapter.getSimpleHistoryEntries took: " + (end - begin) + " ms.");
        return ret
    }

    private fun histCollectionValueToString(valueClass: Class<*>, value: Collection<*>): String {
        val sb = StringBuilder()
        for (ob in value) {
            if (sb.length > 0) {
                sb.append(",")
            }
            if (ob is BaseDO<*>) {
                sb.append(ob.id)
            } else {
                sb.append(ob)
            }
        }
        return sb.toString()
    }

    private fun histValueToString(valueClass: Class<*>, value: Any?): String? {
        if (value == null) {
            return null
        }
        if (value is Collection<*>) {
            return histCollectionValueToString(valueClass, value)
        }
        return Objects.toString(value)
    }

    fun createHistoryEntry(
        entity: Any?, id: Number?, user: String?, property: String?,
        valueClass: Class<*>, oldValue: Any?, newValue: Any?
    ) {
        createHistoryEntry(entity, id, EntityOpType.Update, user, property, valueClass, oldValue, newValue)
    }

    fun createHistoryEntry(
        entity: Any?, id: Number?, opType: EntityOpType?, user: String?, property: String?,
        valueClass: Class<*>, oldValue: Any?, newValue: Any?
    ) {
        //long begin = System.currentTimeMillis();
        val oldVals = histValueToString(valueClass, oldValue)
        val newVals = histValueToString(valueClass, newValue)
        throw IllegalArgumentException("Not yet implemented!")
        /*
        PfEmgrFactory emf = ApplicationContextProvider.getApplicationContext().getBean(PfEmgrFactory.class);
        emf.runInTrans((emgr) -> {
            HistoryServiceManager.get().getHistoryService().insertManualEntry(emgr, opType,
                    entity.getClass().getName(),
                    id, user, property, valueClass.getName(), oldVals, newVals);
            return null;
        });*/
        //long end = System.currentTimeMillis();
        //log.info("HistoryBaseDaoAdapter.createHistoryEntry took: " + (end - begin) + " ms.");
    } /*    public static void inserted(BaseDO<?> ob) {
        throw new IllegalArgumentException("Not yet implemented!");
        //long begin = System.currentTimeMillis();
        PfEmgrFactory emf = ApplicationContextProvider.getApplicationContext().getBean(PfEmgrFactory.class);
        emf.runInTrans((emgr) -> {
            inserted(emgr, ob);
            return null;
        });
        //long end = System.currentTimeMillis();
        //log.info("HistoryBaseDaoAdapter.inserted took: " + (end - begin) + " ms.");
    }

    public static void inserted(PfEmgr emgr, BaseDO<?> ob) {
        EmgrAfterInsertedEvent event = new EmgrAfterInsertedEvent(emgr, ob);
        new HistoryEmgrAfterInsertedEventHandler().onEvent(event);
    }

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
