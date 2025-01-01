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

package org.projectforge.web.fibu;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.business.fibu.kost.BuchungssatzDO;
import org.projectforge.business.fibu.kost.BuchungssatzDao;
import org.projectforge.business.user.UserRightId;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.EditPage;
import org.slf4j.Logger;

@EditPage(defaultReturnPage = AccountingRecordListPage.class)
public class AccountingRecordEditPage
        extends AbstractEditPage<BuchungssatzDO, AccountingRecordEditForm, BuchungssatzDao> {
    private static final long serialVersionUID = -3899191243765232906L;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AccountingRecordEditPage.class);

    public AccountingRecordEditPage(final PageParameters parameters) {
        super(parameters, "fibu.buchungssatz");
        checkAccess();
        init();
    }

    private void checkAccess() {
        getAccessChecker().checkLoggedInUserRight(UserRightId.FIBU_DATEV_IMPORT, UserRightValue.TRUE);
        getAccessChecker().checkRestrictedOrDemoUser();
    }

    @Override
    protected void onPreEdit() {
        super.onPreEdit();
    }

    @Override
    protected BuchungssatzDao getBaseDao() {
        return WicketSupport.get(BuchungssatzDao.class);
    }

    @Override
    protected AccountingRecordEditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage, final BuchungssatzDO data) {
        return new AccountingRecordEditForm(this, data);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
}
