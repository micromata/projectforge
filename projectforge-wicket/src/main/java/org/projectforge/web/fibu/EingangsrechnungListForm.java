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

package org.projectforge.web.fibu;

import org.projectforge.business.fibu.EingangsrechnungDao;
import org.projectforge.business.fibu.EingangsrechnungListFilter;
import org.projectforge.business.fibu.EingangsrechnungsStatistik;
import org.projectforge.business.fibu.PaymentType;
import org.projectforge.web.common.I18nEnumChoiceProvider;
import org.projectforge.web.wicket.LambdaModel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.Select2MultiChoicePanel;
import org.slf4j.Logger;
import org.wicketstuff.select2.Select2MultiChoice;

public class EingangsrechnungListForm extends AbstractRechnungListForm<EingangsrechnungListFilter, EingangsrechnungListPage>
{
  private static final long serialVersionUID = 2678813484329104564L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EingangsrechnungListForm.class);

  @Override
  protected void init()
  {
    final EingangsrechnungDao eingangsrechnungDao = getParentPage().getBaseDao();
    this.years = eingangsrechnungDao.getYears();
    super.init();
  }

  @Override
  protected void onBeforeAddStatistics()
  {
    gridBuilder.newGridPanel();
    final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.payment.type")).suppressLabelForWarning();
    fs.add(createPaymentTypeMultiChoice());
  }

  @Override
  protected EingangsrechnungsStatistik getStats()
  {
    return parentPage.getEingangsrechnungsStatistik();
  }

  public EingangsrechnungListForm(final EingangsrechnungListPage parentPage)
  {
    super(parentPage);
  }

  @Override
  protected EingangsrechnungListFilter newSearchFilterInstance()
  {
    return new EingangsrechnungListFilter();
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

  private Select2MultiChoice<PaymentType> createPaymentTypeMultiChoice()
  {
    return new Select2MultiChoice<>(
        Select2MultiChoicePanel.WICKET_ID,
        LambdaModel.of(getSearchFilter()::getPaymentTypes, getSearchFilter()::setPaymentTypes),
        new I18nEnumChoiceProvider<>(PaymentType.class)
    );
  }

  @Override
  protected String getCancelButtonLabel()
  {
    return getString("back");
  }

  @Override
  protected String getNextButtonLabel()
  {
    return getString("export");
  }
}
