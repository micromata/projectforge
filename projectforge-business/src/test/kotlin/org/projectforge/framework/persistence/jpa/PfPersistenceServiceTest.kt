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
        resetContextCounters()
        persistenceService.runReadOnly { outerContext -> // Readonly +1
            persistenceService.runReadOnly { innerContext -> // Readonly +0
                Assertions.assertEquals(outerContext.contextId, innerContext.contextId)
                persistenceService.runReadOnly { mostInnerContext -> // Readonly +0
                    Assertions.assertEquals(outerContext.contextId, innerContext.contextId)
                    Assertions.assertEquals(outerContext.contextId, mostInnerContext.contextId)
                }
            }
        }
        assertContextCounterAndReset(0, 1)
        persistenceService.runReadOnly { outerContext -> // Readonly +1
            persistenceService.runInTransaction { transContext -> // Transactional +1
                Assertions.assertNotEquals(
                    outerContext.contextId,
                    transContext.contextId,
                    "New transactional context expected."
                )
                persistenceService.runReadOnly { mostInnerContext -> // Readonly +0
                    Assertions.assertEquals(transContext.contextId, mostInnerContext.contextId, "Not isolated context.")
                    Assertions.assertNotEquals(
                        outerContext.contextId,
                        mostInnerContext.contextId,
                        "Embedding transaction context expected."
                    )
                }
            }
        }
        assertContextCounterAndReset(1, 1)
        persistenceService.runReadOnly { outerContext -> // Readonly +1
            persistenceService.runIsolatedReadOnly { innerContext -> // Readonly +1
                Assertions.assertNotEquals(outerContext.contextId, innerContext.contextId)
                persistenceService.runIsolatedReadOnly { mostInnerContext -> // Readonly +1
                    Assertions.assertNotEquals(outerContext.contextId, innerContext.contextId, "Isolated context.")
                    Assertions.assertNotEquals(outerContext.contextId, mostInnerContext.contextId, "Isolated context.")
                }
                assertContextCounter(0, 3)
                persistenceService.runReadOnly { mostInnerContext -> // Readonly +0
                    Assertions.assertEquals(innerContext.contextId, mostInnerContext.contextId, "Not isolated context.")
                }
                assertContextCounter(0, 3)
                persistenceService.runInTransaction { transContext -> // Transactions +1
                    Assertions.assertNotEquals(
                        innerContext.contextId,
                        transContext.contextId,
                        "Readonly context isn't usable, new context expected."
                    )
                }
            }
        }
        assertContextCounterAndReset(1, 3)
        persistenceService.runInTransaction { outerContext -> // Transactional +1
            persistenceService.runInTransaction { innerContext -> // Transactional +0
                Assertions.assertEquals(
                    innerContext.contextId,
                    outerContext.contextId,
                    "Re-used transactional context expected."
                )
            }
            persistenceService.runReadOnly { innerContext -> // Readonly +0
                Assertions.assertEquals(
                    innerContext.contextId,
                    outerContext.contextId,
                    "Re-used transactional context expected. Readonly contexts re-uses transactional ones."
                )
            }
            persistenceService.runIsolatedReadOnly { innerContext ->  // Readonly +1
                Assertions.assertNotEquals(outerContext.contextId, innerContext.contextId, "Isolated context.")
            }
            persistenceService.runInNewTransaction { innerContext -> // Transactional +1
                Assertions.assertNotEquals(innerContext.contextId, outerContext.contextId, "Isolated context.")
            }
        }
        assertContextCounterAndReset(2, 1)
        suppressErrorLogs {
            try {
                persistenceService.runInTransaction { outerContext -> // Transactional +1
                    persistenceService.runInNewTransaction { innerContext -> // Transactional +1
                        throw IllegalArgumentException("Some exception for testing.")
                    }
                }
            } catch (e: IllegalArgumentException) {
                // Expected.
            }
            assertContextCounterAndReset(2, 0)
            try {
                persistenceService.runReadOnly { outerContext -> // Readonly +1
                    persistenceService.runIsolatedReadOnly { innerContext -> // Readonly +1
                        throw IllegalArgumentException("Some exception for testing.")
                    }
                }
            } catch (e: IllegalArgumentException) {
                // Expected.
            }
            assertContextCounterAndReset(0, 2)
        }
        Assertions.assertNull(
            PfPersistenceContextThreadLocal.getReadonly(), "All readonly contexts should be cleared."
        )
        Assertions.assertNull(
            PfPersistenceContextThreadLocal.getTransactional(), "All transactional contexts should be cleared."
        )
    }

    private fun assertContextCounter(numberOfExpectedTransactions: Long, numberOfEexpectedReadonlyContexts: Long) {
        Assertions.assertEquals(
            numberOfExpectedTransactions,
            PfPersistenceContext.transactionCounter - transactionCounterStart,
            "Number of transactional contexts expected.",
        )
        Assertions.assertEquals(
            numberOfEexpectedReadonlyContexts,
            PfPersistenceContext.readonlyContextCounter - readonlyCounterStart,
            "Number of readonly contexts expected.",
        )
    }

    private fun assertContextCounterAndReset(numberOfExpectedTransactions: Long, numberOfEexpectedReadonlyContexts: Long) {
        assertContextCounter(numberOfExpectedTransactions, numberOfEexpectedReadonlyContexts)
        resetContextCounters()
    }

    private fun resetContextCounters() {
        transactionCounterStart = PfPersistenceContext.transactionCounter
        readonlyCounterStart = PfPersistenceContext.readonlyContextCounter
    }

    companion object {
        private var transactionCounterStart = 0L
        private var readonlyCounterStart = 0L
    }
}
