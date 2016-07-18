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

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.orga.ContractDao;
import org.projectforge.business.orga.ContractFilter;
import org.projectforge.business.orga.ContractStatus;
import org.projectforge.business.orga.ContractType;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.components.YearListCoiceRenderer;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

public class ContractListForm extends AbstractListForm<ContractFilter, ContractListPage>
{
  private static final long serialVersionUID = -2813402079364322428L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ContractListForm.class);

  @SpringBean
  private ConfigurationService configurationService;

  public ContractListForm(final ContractListPage parentPage)
  {
    super(parentPage);
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#onOptionsPanelCreate(org.projectforge.web.wicket.flowlayout.FieldsetPanel,
   *      org.projectforge.web.wicket.flowlayout.DivPanel)
   */
  @Override
  protected void onOptionsPanelCreate(final FieldsetPanel optionsFieldsetPanel, final DivPanel optionsCheckBoxesPanel)
  {
    // DropDownChoice years
    final ContractDao contractDao = getParentPage().getBaseDao();
    final YearListCoiceRenderer yearListChoiceRenderer = new YearListCoiceRenderer(contractDao.getYears(), true);
    final DropDownChoice<Integer> yearChoice = new DropDownChoice<Integer>(optionsFieldsetPanel.getDropDownChoiceId(),
        new PropertyModel<Integer>(this, "year"), yearListChoiceRenderer.getYears(), yearListChoiceRenderer);
    yearChoice.setNullValid(false);
    optionsFieldsetPanel.add(yearChoice, true);
    {
      // DropDownChoice status
      final LabelValueChoiceRenderer<ContractStatus> statusRenderer = new LabelValueChoiceRenderer<ContractStatus>();
      for (final ContractStatus status : ContractStatus.values()) {
        statusRenderer.addValue(status, getString(status.getI18nKey()));
      }
      final DropDownChoice<ContractStatus> statusChoice = new DropDownChoice<ContractStatus>(
          optionsFieldsetPanel.getDropDownChoiceId(),
          new PropertyModel<ContractStatus>(getSearchFilter(), "status"), statusRenderer.getValues(), statusRenderer);
      statusChoice.setNullValid(true);
      optionsFieldsetPanel.add(statusChoice, true).setTooltip(getString("status"));
    }
    final List<ContractType> contractTypes = configurationService.getContractTypes();
    if (CollectionUtils.isNotEmpty(contractTypes) == true) {
      // DropDownChoice type
      final LabelValueChoiceRenderer<ContractType> typeRenderer = new LabelValueChoiceRenderer<ContractType>();
      for (final ContractType type : contractTypes) {
        typeRenderer.addValue(type, type.getLabel());
      }
      final DropDownChoice<ContractType> typeChoice = new DropDownChoice<ContractType>(
          optionsFieldsetPanel.getDropDownChoiceId(),
          new PropertyModel<ContractType>(getSearchFilter(), "type"), typeRenderer.getValues(), typeRenderer);
      typeChoice.setNullValid(true);
      optionsFieldsetPanel.add(typeChoice, true).setTooltip(getString("legalAffaires.contract.type"));
    }
  }

  public Integer getYear()
  {
    return getSearchFilter().getYear();
  }

  public void setYear(final Integer year)
  {
    if (year == null) {
      getSearchFilter().setYear(-1);
    } else {
      getSearchFilter().setYear(year);
    }
  }

  @Override
  protected ContractFilter newSearchFilterInstance()
  {
    return new ContractFilter();
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
