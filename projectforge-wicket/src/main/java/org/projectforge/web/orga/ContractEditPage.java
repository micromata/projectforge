/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.web.orga;

import org.apache.log4j.Logger;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.orga.ContractDO;
import org.projectforge.business.orga.ContractDao;
import org.projectforge.framework.time.DayHolder;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.AbstractSecuredBasePage;
import org.projectforge.web.wicket.EditPage;

@EditPage(defaultReturnPage = ContractListPage.class)
public class ContractEditPage extends AbstractEditPage<ContractDO, ContractEditForm, ContractDao>
{
  private static final long serialVersionUID = 4375220914096256551L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ContractEditPage.class);

  @SpringBean
  private ContractDao contractDao;

  public ContractEditPage(final PageParameters parameters)
  {
    super(parameters, "legalAffaires.contract");
    init();
    if (isNew() == true) {
      getData().setDate(new DayHolder().getSQLDate());
    }
  }

  @Override
  public AbstractSecuredBasePage onSaveOrUpdate()
  {
    if (isNew() == true && getData().getNumber() == null) {
      getData().setNumber(contractDao.getNextNumber(getData()));
    }
    return null;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditPage#cloneData()
   */
  @Override
  protected void cloneData()
  {
    super.cloneData();
    final ContractDO contract = getData();
    contract.setNumber(null);
    contract.setDate(new DayHolder().getSQLDate());
    form.numberField.modelChanged();
  }

  @Override
  protected ContractDao getBaseDao()
  {
    return contractDao;
  }

  @Override
  protected ContractEditForm newEditForm(final AbstractEditPage< ? , ? , ? > parentPage, final ContractDO data)
  {
    return new ContractEditForm(this, data);
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
