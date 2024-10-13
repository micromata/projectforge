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

package org.projectforge.framework.persistence.jpa

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.test.AbstractTestBase

class PfPersistenceServiceTest : AbstractTestBase() {
    @Test
    fun contextTest() {
        persistenceService.runReadOnly { outerContext ->
            persistenceService.runReadOnly { innerContext ->
                Assertions.assertEquals(outerContext.contextId, innerContext.contextId)
                persistenceService.runReadOnly { mostInnerContext ->
                    Assertions.assertEquals(outerContext.contextId, innerContext.contextId)
                    Assertions.assertEquals(outerContext.contextId, mostInnerContext.contextId)
                }
            }
        }
        persistenceService.runReadOnly { outerContext ->
            persistenceService.runInIsolatedTransaction { transContext ->
                Assertions.assertNotEquals(
                    outerContext.contextId,
                    transContext.contextId,
                    "New transactional context expected."
                )
                persistenceService.runReadOnly { mostInnerContext ->
                    Assertions.assertEquals(transContext.contextId, mostInnerContext.contextId, "Not isolated context.")
                    Assertions.assertNotEquals(
                        outerContext.contextId,
                        mostInnerContext.contextId,
                        "Embedding transaction context expected."
                    )
                }
            }
        }
        persistenceService.runReadOnly { outerContext ->
            persistenceService.runIsolatedReadOnly { innerContext ->
                Assertions.assertNotEquals(outerContext.contextId, innerContext.contextId)
                persistenceService.runIsolatedReadOnly { mostInnerContext ->
                    Assertions.assertNotEquals(outerContext.contextId, innerContext.contextId, "Isolated context.")
                    Assertions.assertNotEquals(outerContext.contextId, mostInnerContext.contextId, "Isolated context.")
                }
                persistenceService.runReadOnly { mostInnerContext ->
                    Assertions.assertEquals(innerContext.contextId, mostInnerContext.contextId, "Not isolated context.")
                }
                persistenceService.runInTransaction { transContext ->
                    Assertions.assertNotEquals(
                        innerContext.contextId,
                        transContext.contextId,
                        "Readonly context isn't usable, new context expected."
                    )
                }
            }
        }
        persistenceService.runInTransaction { outerContext ->
            persistenceService.runInTransaction { innerContext ->
                Assertions.assertEquals(
                    innerContext.contextId,
                    outerContext.contextId,
                    "Re-used transactional context expected."
                )
            }
            persistenceService.runReadOnly { innerContext ->
                Assertions.assertEquals(
                    innerContext.contextId,
                    outerContext.contextId,
                    "Re-used transactional context expected. Readonly contexts re-uses transactional ones."
                )
            }
            persistenceService.runIsolatedReadOnly { innerContext ->
                Assertions.assertNotEquals(outerContext.contextId, innerContext.contextId, "Isolated context.")
            }
            persistenceService.runInIsolatedTransaction { innerContext ->
                Assertions.assertNotEquals(innerContext.contextId, outerContext.contextId, "Isolated context.")
            }
        }
        suppressErrorLogs {
            try {
                persistenceService.runInTransaction { outerContext ->
                    persistenceService.runInIsolatedTransaction { innerContext ->
                        throw IllegalArgumentException("Some exception for testing.")
                    }
                }
            } catch (e: IllegalArgumentException) {
                // Expected.
            }
            try {
                persistenceService.runReadOnly { outerContext ->
                    persistenceService.runIsolatedReadOnly { innerContext ->
                        throw IllegalArgumentException("Some exception for testing.")
                    }
                }
            } catch (e: IllegalArgumentException) {
                // Expected.
            }
        }
        Assertions.assertNull(
            PfPersistenceContextThreadLocal.getReadonly(), "All readonly contexts should be cleared."
        )
        Assertions.assertNull(
            PfPersistenceContextThreadLocal.getTransactional(), "All transactional contexts should be cleared."
        )
    }
}
