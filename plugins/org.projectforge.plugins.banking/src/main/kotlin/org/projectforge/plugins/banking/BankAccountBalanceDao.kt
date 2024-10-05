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

package org.projectforge.plugins.banking

import mu.KotlinLogging
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseDaoSupport
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class BankAccountBalanceDao : BaseDao<BankAccountBalanceDO>(BankAccountBalanceDO::class.java) {
    @Autowired
    private lateinit var bankAccountDao: BankAccountDao

    override fun hasAccess(
        user: PFUserDO,
        obj: BankAccountBalanceDO?,
        oldObj: BankAccountBalanceDO?,
        operationType: OperationType,
        throwException: Boolean
    ): Boolean {
        val bankAccount = obj?.bankAccount
        if (obj != null && bankAccount == null) {
            return BaseDaoSupport.returnFalseOrThrowException(
                throwException,
                user,
                operationType,
                msg = "Bank account not given.",
            )
        }
        if (!accessChecker.isLoggedInUserMemberOfGroup(ProjectForgeGroup.FINANCE_GROUP)) {
            // Double check, user isn't member of financial staff.
            return BaseDaoSupport.returnFalseOrThrowException(
                throwException,
                user,
                operationType,
                msg = "User not member of financial staff.",
            )
        }
        val oldBankAccount = oldObj?.bankAccount
        return bankAccountDao.hasAccess(user, bankAccount, oldBankAccount, operationType, throwException)
    }

    override fun newInstance(): BankAccountBalanceDO {
        return BankAccountBalanceDO()
    }

    fun getByTimePeriod(accountId: Int): List<BankAccountBalanceDO> {
        val account = bankAccountDao.getById(accountId)!! // For access checking
        log.info("Getting Balances of account '${account.name}', IBAN=${account.iban}")
        return persistenceService.executeNamedQuery(
            BankAccountBalanceDO.FIND_BY_BANK_ACCOUNT,
            BankAccountBalanceDO::class.java,
            Pair("bankAccountId", accountId),
        )
    }
}
