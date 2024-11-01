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

package org.projectforge.framework.persistence.api.impl

import org.hibernate.search.engine.search.common.BooleanOperator
import org.hibernate.search.mapper.orm.Search
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.task.TaskDO
import org.projectforge.business.task.TaskDao
import org.projectforge.business.user.UserDao
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.Month

class DBQueryBuilderByFullTextTest : AbstractTestBase() {
    @Autowired
    protected lateinit var dbQuery: DBQuery

    @Autowired
    private lateinit var taskDao: TaskDao

    @Autowired
    private lateinit var userDao: UserDao

    @Test
    fun `test full text query`() {
        persistenceService.runInTransaction {
            val parentTask = initTestDB.addTask(this::class.simpleName!!, "root")
            val user1 = PFUserDO().also {
                it.username = "Horst Schlämmer"
                it.mobilePhone = "+49 1234 56789"
                userDao.insert(it, false)
            }
            val user2 = PFUserDO().also {
                it.username = "Berta Schlau-Meier"
                it.mobilePhone = "+31 9876 54321"
                userDao.insert(it, false)
            }
            val task1 = TaskDO().also {
                it.parentTask = parentTask
                it.title = "Task One"
                it.maxHours = 100
                it.protectTimesheetsUntil = LocalDate.of(2024, Month.OCTOBER, 31)
                it.description = "This is a task description"
                it.shortDescription = "Task one short description"
                it.responsibleUser = user1
                taskDao.insert(it, false)
            }
            val task2 = TaskDO().also {
                it.parentTask = parentTask
                it.title = "Task Two"
                it.protectTimesheetsUntil = LocalDate.of(2024, Month.JANUARY, 31)
                it.maxHours = 200
                it.description = "This is a task description"
                it.shortDescription = "Task one short description"
                it.responsibleUser = user2
                taskDao.insert(it, false)
            }
        }

        val queryFilter = QueryFilter().also {
            //it.add(QueryFilter.eq("deleted", false))
            //it.add(QueryFilter.eq("title", "Task One"))
            it.add(QueryFilter.gt("protectTimesheetsUntil", LocalDate.of(2024, Month.OCTOBER, 1)))
            it.addFullTextSearch("horst")
            it.fullTextSearchFields =
                arrayOf("title", "description", "shortDescription", "responsibleUser.username")
            //it.add(QueryFilter.gt("maxHours", 100))
        }
        // maxHours: Int, protectTimesheetsUntil: LocalDate, status: Status, responsibleUser: PFUserDO
        // fields: title, description, shortDescription,
        persistenceService.runReadOnly { context ->
            Search.session(context.em).massIndexer().startAndWait()
            val result = dbQuery.select(taskDao, queryFilter, null, false)
            Assertions.assertEquals(1, result.size)
        }
    }

    @Test
    fun `test bridges`() {
        // AddressDO, PFUserDO.mobilePhone, ...
    }

    /*
    // 1. Hibernate Search-Abfrage, um IDs der Treffer zu sammeln
val searchResult: SearchResult<Long> = Search.session(context.em).search(TaskDO::class.java)
    .select(f -> f.id())  // Nur die IDs abrufen
    .where { f ->
        f.bool { bool ->
            bool.must(
                f.simpleQueryString()
                    .fields("title", "description")
                    .matching("suchbegriff")
                    .defaultOperator(BooleanOperator.AND)
            )
        }
    }
    .fetchAll()  // Alle IDs abrufen

val taskIds = searchResult.hits()

// 2. Criteria-Abfrage für zusätzliche Filter auf nicht indizierte Felder
val criteriaBuilder: CriteriaBuilder = context.em.criteriaBuilder
val criteriaQuery: CriteriaQuery<TaskDO> = criteriaBuilder.createQuery(TaskDO::class.java)
val root: Root<TaskDO> = criteriaQuery.from(TaskDO::class.java)

criteriaQuery.select(root).where(
    criteriaBuilder.and(
        root.get<Long>("id").`in`(taskIds),  // IDs aus der Volltextsuche verwenden
        criteriaBuilder.equal(root.get<String>("status"), "offen")  // Zusätzliches Kriterium auf nicht indiziertes Feld
    )
)

// 3. Die kombinierten Ergebnisse abfragen
val results = context.em.createQuery(criteriaQuery).resultList
     */
}
