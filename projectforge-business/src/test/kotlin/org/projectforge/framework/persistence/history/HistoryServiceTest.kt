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

import mu.KotlinLogging
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.AuftragDO
import org.projectforge.business.fibu.AuftragsPositionDO
import org.projectforge.business.fibu.RechnungDO
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import java.net.URI

private val log = KotlinLogging.logger {}

class HistoryServiceTest : AbstractTestBase() {
    @Autowired
    private lateinit var historyService: HistoryService

    /**
     * Key is the parsed history master entry id and value the database entry saved by this test case.
     */
    private val historyMasterMap = mutableMapOf<Long, PfHistoryMasterDO>()

    /**
     * Key is the parsed history master_fk id and value the generated PfHistoryAttrDO.
     */
    private val historyAttrMap = mutableMapOf<Long, MutableList<PfHistoryAttrDO>>()

    @Test
    fun testOldInvoiceHistory() {
        ensureSetup()
        val invoice = RechnungDO()
        invoice.id = 40770225
        /*historyService.loadHistory(invoice).let { historyEntries ->
            Assertions.assertEquals(2, historyEntries.size)
            var master = historyEntries[0]
            Assertions.assertEquals(12, master.attributes!!.size)
            val diffEntries = PFHistoryMasterUtils.createDiffEntries(master.attributes!!)
            Assertions.assertEquals(4, diffEntries.size)
            assert(diffEntries[0], "bemerkung", "PoBa", null, PropertyOpType.Insert)
            assert(diffEntries[1], "bezahlDatum", "2023-12-29", null, PropertyOpType.Insert)
            assert(diffEntries[2], "status", "BEZAHLT", "GESTELLT", PropertyOpType.Update)
            assert(diffEntries[3], "zahlBetrag", "4765.95", null, PropertyOpType.Insert)
            master = historyEntries[1]
            Assertions.assertEquals(34, master.attributes!!.size)
        }*/
        persistenceService.runInTransaction { context ->
            val em = context.em
            em.createNativeQuery("insert into t_fibu_rechnung (pk,deleted,datum) values (351958,false,'2023-12-29')").executeUpdate()
            em.createNativeQuery("insert into t_fibu_rechnung_position (pk,deleted,rechnung_fk,number) values (351959,false,351958,1)").executeUpdate()
            em.createNativeQuery("insert into t_fibu_kost_zuweisung (pk,deleted,rechnungs_pos_fk,index) values (382507,false,351959,1)").executeUpdate()
            em.createNativeQuery("insert into t_fibu_kost_zuweisung (pk,deleted,rechnungs_pos_fk,index) values (382508,false,351959,2)").executeUpdate()
            em.createNativeQuery("insert into t_fibu_kost_zuweisung (pk,deleted,rechnungs_pos_fk,index) values (382509,false,351959,3)").executeUpdate()
            em.createNativeQuery("insert into t_fibu_rechnung_position (pk,deleted,rechnung_fk,number) values (351960,false,351958,2)").executeUpdate()
            em.createNativeQuery("insert into t_fibu_kost_zuweisung (pk,deleted,rechnungs_pos_fk,index) values (382506,false,351960,1)").executeUpdate()
        }
        invoice.id = 351958
        historyService.loadHistory(invoice).let { historyEntries ->
            historyEntries.filter { it.entityId == 351958L }.let { entries ->
                Assertions.assertEquals(4, entries.size, "4 entries for Invoice 351958")
            }
            historyEntries.filter { it.entityId == 351959L }.let { entries ->
                Assertions.assertEquals(4, entries.size, "4 entries for Invoice position 351959")
            }
            historyEntries.filter { it.entityId == 351960L }.let { entries ->
                Assertions.assertEquals(4, entries.size, "4 entries for Invoice position 3519560")
            }
            historyEntries.filter { it.entityId == 382506L }.let { entries ->
                Assertions.assertEquals(2, entries.size, "2 entries for Kostzuweisung 382506")
            }
            historyEntries.filter { it.entityId == 382507L }.let { entries ->
                Assertions.assertEquals(2, entries.size, "2 entries for Kostzuweisung 382507")
            }
            historyEntries.filter { it.entityId == 382508L }.let { entries ->
                Assertions.assertEquals(2, entries.size, "2 entries for Kostzuweisung 382508")
            }
            historyEntries.filter { it.entityId == 382509L }.let { entries ->
                Assertions.assertEquals(2, entries.size, "2 entries for Kostzuweisung 382509")
            }
            Assertions.assertEquals(20, historyEntries.size)
        }
    }

    @Test
    fun testOldUserHistory() {
        ensureSetup()
        val user = PFUserDO()
        user.id = 34961222
        historyService.loadHistory(user).let { historyEntries ->
            Assertions.assertEquals(27, historyEntries.size)
            var master = historyEntries.find { it.id == getNewMasterId(34961266) }!! // was 34961266
            Assertions.assertEquals(3, master.attributes!!.size)
            var diffEntries = PFHistoryMasterUtils.createDiffEntries(master.attributes!!)
            Assertions.assertEquals(1, diffEntries.size)
            assert(diffEntries[0], "assignedGroups", "1100452,1100063,1826459,33", null, PropertyOpType.Update)
            master = historyEntries.find { it.id == getNewMasterId(38057999) }!! // was 38057999
            Assertions.assertEquals(3, master.attributes!!.size)
            diffEntries = PFHistoryMasterUtils.createDiffEntries(master.attributes!!)
            Assertions.assertEquals(1, diffEntries.size)
            assert(
                diffEntries[0],
                "lastPasswordChange",
                "2023-02-10 13:34:25:184",
                "2022-10-04 09:55:19:329",
                PropertyOpType.Update
            )
            master = historyEntries.find { it.id == getNewMasterId(37229748) }!! // was 37229748
            Assertions.assertEquals(6, master.attributes!!.size)
            diffEntries = PFHistoryMasterUtils.createDiffEntries(master.attributes!!)
            Assertions.assertEquals(2, diffEntries.size)
            assert(diffEntries[0], "locale", "de_DE", "", PropertyOpType.Update)
            assert(diffEntries[1], "timeZoneString", "Europe/Berlin", null, PropertyOpType.Insert)
        }
    }

    @Test
    fun testOldAuftragHistory() {
        ensureSetup()
        val order = AuftragDO()
        order.id = 36901223
        historyService.loadHistory(order).let { historyEntries ->
            // 9 master entries, 36 attr entries in old format -> 18 entries in new format.
            historyEntries.filter { it.entityId == 36901223L }.let { entries ->
                Assertions.assertEquals(9, entries.size, "9 entries for Auftrag 36901229")
            }
            historyEntries.filter { it.entityId == 36901224L }.let { entries ->
                Assertions.assertEquals(3, entries.size, "3 for Auftragsposition 36901224")
            }
            historyEntries.filter { it.entityId == 36901225L }.let { entries ->
                Assertions.assertEquals(3, entries.size, "3 for Auftragsposition 36901225")
            }
            historyEntries.filter { it.entityId == 36901226L }.let { entries ->
                Assertions.assertEquals(3, entries.size, "3 for Auftragsposition 36901226")
            }
            historyEntries.filter { it.entityId == 36901227L }.let { entries ->
                Assertions.assertEquals(3, entries.size, "3 for Auftragsposition 36901227")
            }
            historyEntries.filter { it.entityId == 36901228L }.let { entries ->
                Assertions.assertEquals(5, entries.size, "5 for Auftragsposition 36901228")
            }
            Assertions.assertEquals(26, historyEntries.size, "26 entries in total")
            val master = historyEntries.find { it.id == getNewMasterId(36901229) }!! // was 36901229
            Assertions.assertEquals(36, master.attributes!!.size)
            val diffEntries = PFHistoryMasterUtils.createDiffEntries(master.attributes!!)
            Assertions.assertEquals(18, diffEntries.size)

            val orderPos = AuftragsPositionDO()
            orderPos.id = 36901228
        }
    }

    private fun getNewMasterId(origMasterId: Long): Long {
        val newMaster = historyMasterMap[origMasterId]
        Assertions.assertNotNull(newMaster, "Master with id $origMasterId not found!")
        return newMaster!!.id!!
    }

    private fun ensureSetup() {
        if (historyMasterMap.isNotEmpty()) {
            return // Already done.
        }
        parseFile(getUri("/history/pf_history-testentries.csv")).forEach { map ->
            // pk, modifiedby, entity_id, entity_name, entity_optype
            val pk = map["pk"]!!.toLong()
            val historyMaster = PfHistoryMasterDO()
            historyMaster.modifiedBy = map["modifiedby"]
            historyMaster.entityId = map["entity_id"]!!.toLong()
            historyMaster.entityName = map["entity_name"]!!
            historyMaster.entityOpType = EntityOpType.valueOf(map["entity_optype"]!!)
            historyMasterMap[pk] = historyMaster
        }
        parseFile(getUri("/history/pf_history_attr-testentries.csv")).forEach { map ->
            try {
                // value, propertyname, type, property_type_class, old_value, optype, master_fk
                val attr = PfHistoryAttrDO()
                attr.value = map["value"]
                attr.oldValue = map["old_alue"]
                attr.propertyName = map["propertyname"]
                attr.propertyTypeClass = map["property_type_class"]
                map["optype"]?.let { optype ->
                    if (optype.isNotEmpty()) {
                        attr.optype = PropertyOpType.valueOf(optype)
                    }
                }
                val masterFk = map["master_fk"]!!.toLong()
                historyAttrMap.getOrPut(masterFk) { mutableListOf() }.add(attr)
            } catch (ex: Exception) {
                log.error {
                    "Error while parsing map ${
                        map.entries.joinToString(
                            prefix = "{",
                            postfix = "}"
                        ) { (key, value) -> "\"$key\": \"$value\"" }
                    }"
                }
                throw IllegalStateException("Error parsing map $map", ex)
            }
        }
        persistenceService.runInTransaction { context ->
            val em = context.em
            historyMasterMap.entries.forEach { entry ->
                val master = entry.value
                val masterId = entry.key
                val attrs = historyAttrMap[masterId]
                historyService.save(em, master, attrs)
            }
        }

        val user = getUser(TEST_USER)
        // One group assigned:
        addOldFormat(user, value = "1052256", propertyName = "assignedGroups", operationType = EntityOpType.Insert)
        // Assigned: 34,101478,33 unassigned: 17,16,11,31
        addOldFormat(
            user,
            value = "34,101478,33",
            oldValue = "17,16,11,31",
            propertyName = "assignedGroups",
            operationType = EntityOpType.Insert
        )
        // U
        addOldFormat(
            user,
            value = "Project manager",
            oldValue = "Project assistant",
            propertyName = "description",
            operationType = EntityOpType.Update,
        )
        // Test entries generated with:
        // \pset null 'NULL'
        // select pk,entity_id,entity_name,entity_optype from t_pf_history where entity_id = <ENTITY_ID>
        // select value,propertyname,type,property_type_class,old_value,optype,master_fk from t_pf_history_attr where master_fk in (PK1,PK2,PK3);

    }

    private fun assert(
        diffEntry: DiffEntry,
        propertyName: String,
        value: String?,
        oldValue: String?,
        operationType: PropertyOpType
    ) {
        Assertions.assertEquals(propertyName, diffEntry.propertyName)
        Assertions.assertEquals(value, diffEntry.newValue)
        Assertions.assertEquals(oldValue, diffEntry.oldValue)
        Assertions.assertEquals(operationType, diffEntry.propertyOpType)
    }

    /**
     * Create entries in new format:
     */
    private fun add(
        entity: BaseDO<Long>,
        value: String?,
        oldValue: String? = null,
        propertyName: String,
        operationType: EntityOpType
    ) {
        val master = HistoryCreateUtils.createMaster(entity, operationType)

        val attr1 = HistoryCreateUtils.createAttr(
            GroupDO::class,
            propertyName = propertyName,
            value = value,
            oldValue = oldValue,
        )
        val attrs = mutableListOf(attr1)

        val pk = historyService.save(master, attrs)!!

        Assertions.assertEquals("org.projectforge.framework.persistence.user.entities.PFUserDO", master.entityName)
        Assertions.assertEquals(entity.id, master.entityId)
        Assertions.assertEquals("anon", master.modifiedBy)
        val createdAt = master.modifiedAt!!.time
        Assertions.assertTrue(
            Math.abs(System.currentTimeMillis() - createdAt) < 10000,
            "createdAt should be near to now (10s)",
        )

        Assertions.assertEquals(master.id, attr1.master!!.id)
    }

    /**
     * Create entries in old mgc format:
     */
    private fun addOldFormat(
        entity: BaseDO<Long>,
        value: String?,
        oldValue: String? = null,
        propertyName: String,
        operationType: EntityOpType
    ) {
        val master = HistoryCreateUtils.createMaster(entity, operationType)

        val attr1 = HistoryCreateUtils.createAttr(GroupDO::class, propertyName = "$propertyName:nv", value = value)
        val attr2 = HistoryCreateUtils.createAttr(oldPropertyClass, "$propertyName:op", value = operationType.name)
        val attr3 = HistoryCreateUtils.createAttr(GroupDO::class, "$propertyName:ov", value = oldValue)
        val attrs = mutableListOf(attr1, attr2, attr3)

        val pk = historyService.save(master, attrs)!!

        Assertions.assertEquals("org.projectforge.framework.persistence.user.entities.PFUserDO", master.entityName)
        Assertions.assertEquals(entity.id, master.entityId)
        Assertions.assertEquals("anon", master.modifiedBy)
        val createdAt = master.modifiedAt!!.time
        Assertions.assertTrue(
            Math.abs(System.currentTimeMillis() - createdAt) < 10000,
            "createdAt should be near to now (10s)",
        )

        Assertions.assertEquals(master.id, attr1.master!!.id)
    }

    private fun parseFile(uri: URI): List<Map<String, String?>> {
        val result = mutableListOf<Map<String, String?>>()
        val columnNames = mutableListOf<String>()
        var lineNo = 0
        File(uri).readLines().forEach { line ->
            ++lineNo
            if (columnNames.isEmpty()) {
                // First row:
                columnNames.addAll(parseLine(line).map { it ?: "<unknown>" })
            } else if (!line.contains('|') || line.startsWith("--")) {
                // Ignore empty lines and second row.
            } else {
                val values = parseLine(line)
                val map = mutableMapOf<String, String?>()
                if (values.size != columnNames.size) {
                    throw IllegalStateException("Values size ${values.size} != columnNames size ${columnNames.size} in line $lineNo: $line")
                }
                columnNames.forEachIndexed { index, name ->
                    map[name] = values[index]
                }
                result.add(map)
            }
        }
        return result
    }

    private fun parseLine(line: String): List<String?> {
        val list = mutableListOf<String?>()
        line.split("|").forEach { entry ->
            val trimmed = entry.trim()
            if (trimmed == "NULL") {
                list.add(null)
            } else {
                list.add(trimmed)
            }
        }
        return list
    }

    private fun getUri(path: String): URI {
        return object {}.javaClass.getResource(path)!!.toURI()
    }

    companion object {
        internal const val SELECT_HISTORY_FOR_BASEDO = "PfHistoryAttrDO_SelectForBaseDO"

        private val oldPropertyClass = "de.micromata.genome.db.jpa.history.entities.PropertyOpType"
    }
}
