/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.orga.ContractDO;
import org.projectforge.business.orga.ContractStatus;
import org.projectforge.business.orga.ContractType;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.autocompletion.PFAutoCompleteMaxLengthTextField;
import org.projectforge.web.wicket.autocompletion.PFAutoCompleteTextField;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.*;
import org.projectforge.web.wicket.flowlayout.*;
import org.slf4j.Logger;

import java.time.LocalDate;
import java.util.List;

public class ContractEditForm extends AbstractEditForm<ContractDO, ContractEditPage>
{
  private static final long serialVersionUID = -2138017238114715368L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ContractEditForm.class);

  protected LocalDatePanel datePanel, validFromDatePanel, validUntilDatePanel, dueDatePanel, resubmissionDatePanel,
      signingDatePanel;

  protected MinMaxNumberField<Integer> numberField;

  @SpringBean
  private ConfigurationService configurationService;

  public ContractEditForm(final ContractEditPage parentPage, final ContractDO data)
  {
    super(parentPage, data);
  }

  @SuppressWarnings("serial")
  private PFAutoCompleteTextField<String> createAutocompleteTextField(final String property)
  {
    final PFAutoCompleteTextField<String> textField = new PFAutoCompleteMaxLengthTextField(InputPanel.WICKET_ID,
        new PropertyModel<>(
            data, property))
    {
      @Override
      protected List<String> getChoices(final String input)
      {
        return parentPage.getBaseDao().getAutocompletion(property, input);
      }
    }.withMatchContains(true).withMinChars(2);
    return textField;
  }

  @Override
  protected void init()
  {
    super.init();
    // GRID 50% - BLOCK
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // Number
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("legalAffaires.contract.number"));
      fs.add(new DivTextPanel(fs.newChildId(), "C-"));
      numberField = new MinMaxNumberField<>(InputPanel.WICKET_ID, new PropertyModel<>(data, "number"), 0, 99999999);
      numberField.setMaxLength(8);
      WicketUtils.setSize(numberField, 6);
      fs.add(numberField);
      if (!NumberHelper.greaterZero(getData().getNumber())) {
        fs.addHelpIcon(getString("fibu.tooltip.nummerWirdAutomatischVergeben"));
      }
    }
    {
      // Date
      final FieldProperties<LocalDate> props = getDateProperties();
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("date"));
      datePanel = new LocalDatePanel(fs.newChildId(), new LocalDateModel(props.getModel()));
      fs.add(datePanel);
    }
    {
      // Title
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("title"));
      fs.add(createAutocompleteTextField("title")).getField().setRequired(true).add(WicketUtils.setFocus());
    }
    {
      // Contract type
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("legalAffaires.contract.type"));
      final List<ContractType> contractTypes = configurationService.getContractTypes();
      final LabelValueChoiceRenderer<ContractType> typeChoiceRenderer = new LabelValueChoiceRenderer<>(
          contractTypes);
      final DropDownChoice<ContractType> typeChoice = new DropDownChoice<>(fs.getDropDownChoiceId(),
          new PropertyModel<>(data, "type"), typeChoiceRenderer.getValues(), typeChoiceRenderer);
      typeChoice.setNullValid(false);
      fs.add(typeChoice);
    }
    {
      // Status
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("status"));
      // DropDownChoice for convenient selection of time periods.
      final LabelValueChoiceRenderer<ContractStatus> statusChoiceRenderer = new LabelValueChoiceRenderer<>();
      for (final ContractStatus status : ContractStatus.values()) {
        statusChoiceRenderer.addValue(status, getString(status.getI18nKey()));
      }
      final DropDownChoice<ContractStatus> statusChoice = new DropDownChoice<>(fs.getDropDownChoiceId(),
          new PropertyModel<>(data,
              "status"),
          statusChoiceRenderer.getValues(), statusChoiceRenderer);
      statusChoice.setNullValid(false);
      fs.add(statusChoice);

    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // Reference
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.common.reference"));
      fs.add(new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<>(data, "reference")));
    }
    {
      // Resubmission date
      final FieldProperties<LocalDate> props = getResubmissionOnDateProperties();
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("resubmissionOnDate"));
      resubmissionDatePanel = new LocalDatePanel(fs.newChildId(), new LocalDateModel(props.getModel()));
      fs.add(resubmissionDatePanel);
    }
    {
      // Due date
      final FieldProperties<LocalDate> props = getDueDateProperties();
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("dueDate"));
      dueDatePanel = new LocalDatePanel(fs.newChildId(), new LocalDateModel(props.getModel()));
      fs.add(dueDatePanel);
    }
    {
      // Signing date
      final FieldProperties<LocalDate> props = getSigningDateProperties();
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("legalAffaires.contract.signing"), getString("date"));
      signingDatePanel = new LocalDatePanel(fs.newChildId(), new LocalDateModel(props.getModel()));
      fs.add(signingDatePanel);
    }
    {
      // Validity
      FieldProperties<LocalDate> props = getValidFromProperties();
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("legalAffaires.contract.validity"));
      validFromDatePanel = new LocalDatePanel(fs.newChildId(), new LocalDateModel(props.getModel()));
      fs.add(validFromDatePanel);
      fs.add(new DivTextPanel(fs.newChildId(), "-"));
      props = getValidUntilProperties();
      validUntilDatePanel = new LocalDatePanel(fs.newChildId(), new LocalDateModel(props.getModel()));
      fs.add(validUntilDatePanel);
    }
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // CocontractorA
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("legalAffaires.contract.coContractorA"));
      fs.add(createAutocompleteTextField("coContractorA"));
    }
    {
      // CopersonA
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("legalAffaires.contract.contractPersonA"));
      fs.add(createAutocompleteTextField("contractPersonA"));
    }
    {
      // SignerA
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("legalAffaires.contract.signerA"));
      fs.add(createAutocompleteTextField("signerA"));
    }
    /* GRID8 */
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // CocontractorB
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("legalAffaires.contract.coContractorB"));
      fs.add(createAutocompleteTextField("coContractorB"));
    }
    {
      // CopersonB
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("legalAffaires.contract.contractPersonB"));
      fs.add(createAutocompleteTextField("contractPersonB"));
    }
    {
      // SignerB
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("legalAffaires.contract.signerB"));
      fs.add(createAutocompleteTextField("signerB"));
    }
    /* GRID16 */
    gridBuilder.newGridPanel();
    {
      // Text with JIRA support
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("text"));
      final IModel<String> model = new PropertyModel<>(data, "text");
      fs.add(new MaxLengthTextArea(TextAreaPanel.WICKET_ID, model));
      fs.addJIRAField(model);
    }
    {
      // Filing
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("filing"));
      fs.add(new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<>(data, "filing")));
    }
    addCloneButton();
  }



  private FieldProperties<LocalDate> getDateProperties() {
    return new FieldProperties<>("date", new PropertyModel<>(data, "date"));
  }

  private FieldProperties<LocalDate> getResubmissionOnDateProperties() {
    return new FieldProperties<>("resubmissionOnDate", new PropertyModel<>(data, "resubmissionOnDate"));
  }

  private FieldProperties<LocalDate> getDueDateProperties() {
    return new FieldProperties<>("dueDate", new PropertyModel<>(data, "dueDate"));
  }

  private FieldProperties<LocalDate> getSigningDateProperties() {
    return new FieldProperties<>("legalAffaires.contract.signing", new PropertyModel<>(data, "signingDate"));
  }

  private FieldProperties<LocalDate> getValidFromProperties() {
    return new FieldProperties<>("legalAffaires.contract.validity", new PropertyModel<>(data, "validFrom"));
  }

  private FieldProperties<LocalDate> getValidUntilProperties() {
    return new FieldProperties<>("legalAffaires.contract.validity", new PropertyModel<>(data, "validUntil"));
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
