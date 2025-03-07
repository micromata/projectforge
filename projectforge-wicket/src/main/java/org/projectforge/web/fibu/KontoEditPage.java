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
import org.projectforge.business.fibu.KontoDO;
import org.projectforge.business.fibu.KontoDao;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.EditPage;
import org.slf4j.Logger;


@EditPage(defaultReturnPage = KontoListPage.class)
public class KontoEditPage extends AbstractEditPage<KontoDO, KontoEditForm, KontoDao> {
    private static final long serialVersionUID = 8763884579951937296L;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(KontoEditPage.class);

    public KontoEditPage(final PageParameters parameters) {
        super(parameters, "fibu.konto");
        init();
    }

    @Override
    protected KontoDao getBaseDao() {
        return WicketSupport.get(KontoDao.class);
    }

    @Override
    protected KontoEditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage, final KontoDO data) {
        return new KontoEditForm(this, data);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
}
