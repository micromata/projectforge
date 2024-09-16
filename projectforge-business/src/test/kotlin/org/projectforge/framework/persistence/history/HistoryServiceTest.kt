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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.RechnungDO
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import java.net.URI

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
    fun testOldHistory() {
        ensureSetup()
        val invoice = RechnungDO()
        invoice.id = 40770225
        val historyEntries = historyService.loadHistory(invoice)
        Assertions.assertEquals(2, historyEntries.size)
    }

    fun ensureSetup() {
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
        }
        historyMasterMap.entries.forEach { entry ->
            val master = entry.value
            val masterId = entry.key
            val attrs = historyAttrMap[masterId]
            historyService.save(master, attrs)
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
        // select value,propertyname,type,property_type_class,old_value,optype,master_fk from t_pf_history_attr where master_fk=<PK>

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
