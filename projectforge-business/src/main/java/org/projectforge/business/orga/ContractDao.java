/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.orga;

import jakarta.persistence.Tuple;
import kotlin.Pair;
import org.apache.commons.lang3.ArrayUtils;
import org.projectforge.business.user.UserRightId;
import org.projectforge.common.i18n.MessageParam;
import org.projectforge.common.i18n.MessageParamType;
import org.projectforge.common.i18n.UserException;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.utils.SQLHelper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
public class ContractDao extends BaseDao<ContractDO> {
    public static final UserRightId USER_RIGHT_ID = UserRightId.ORGA_CONTRACTS;
    private final static int START_NUMBER = 1000;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ContractDao.class);
    private static final String[] ENABLED_AUTOCOMPLETION_PROPERTIES = {"title", "coContractorA", "coContractorB", "contractPersonA", "contractPersonB", "signerA", "signerB"};

    public ContractDao() {
        super(ContractDO.class);
        userRightId = USER_RIGHT_ID;
    }

    @Override
    public boolean isAutocompletionPropertyEnabled(String property) {
        // All users with select access for contracts have access to all contracts, therefore no special select checking for single entities is needed.
        return ArrayUtils.contains(ENABLED_AUTOCOMPLETION_PROPERTIES, property);
    }

    @Override
    public List<ContractDO> select(final BaseSearchFilter filter) throws AccessException {
        final ContractFilter myFilter;
        if (filter instanceof ContractFilter) {
            myFilter = (ContractFilter) filter;
        } else {
            myFilter = new ContractFilter(filter);
        }
        final QueryFilter queryFilter = new QueryFilter(myFilter);
        if (myFilter.getStatus() != null) {
            queryFilter.add(QueryFilter.eq("status", myFilter.getStatus().name()));
        }
        if (myFilter.getType() != null) {
            queryFilter.add(QueryFilter.eq("type", myFilter.getType().getValue()));
        }
        queryFilter.setYearAndMonth("date", myFilter.getYear(), -1);
        if (log.isDebugEnabled()) {
            log.debug(myFilter.toString());
        }
        return select(queryFilter);
    }

    /**
     * List of all years with contracts: select min(date), max(date) from t_contract.
     */
    public int[] getYears() {
        final Tuple minMaxDate = persistenceService.selectNamedSingleResult(
                ContractDO.SELECT_MIN_MAX_DATE,
                Tuple.class);
        return SQLHelper.getYearsByTupleOfLocalDate(minMaxDate);
    }

    /**
     * A given contract number must be consecutively numbered.
     */
    @Override
    public void onInsertOrModify(final ContractDO obj, final OperationType operationType) {
        if (obj.getId() == null) {
            // New contract
            final Integer next = getNextNumber(obj);
            obj.setNumber(next);
        } else {
            if (obj.getNumber() == null) {
                throw new UserException("validation.required.valueNotPresent", new MessageParam("legalAffaires.contract.number",
                        MessageParamType.I18N_KEY)).setCausedByField("number");
            }
            ContractDO other = persistenceService.selectNamedSingleResult(
                    ContractDO.FIND_OTHER_BY_NUMBER,
                    ContractDO.class,
                    new Pair<>("number", obj.getNumber()),
                    new Pair<>("id", obj.getId()));
            if (other != null) {
                throw new UserException("legalAffaires.contract.error.numberAlreadyExists").setCausedByField("number");
            }
        }
    }

    /**
     * Gets the highest contract number.
     *
     * @param contract is needed to check whether the contract does already exist or not. If already exist it will be
     *                 assured that this contract has an unchanged number.
     */
    @SuppressWarnings("unchecked")
    public Integer getNextNumber(final ContractDO contract) {
        if (contract.getId() != null) {
            final ContractDO orig = find(contract.getId(), false);
            if (orig.getNumber() != null) {
                contract.setNumber(orig.getNumber());
                return orig.getNumber();
            }
        }
        return persistenceService.getNextNumber("ContractDO", "number", START_NUMBER);
    }

    @Override
    public ContractDO newInstance() {
        return new ContractDO();
    }
}
