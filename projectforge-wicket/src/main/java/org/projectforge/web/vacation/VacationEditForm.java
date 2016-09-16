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

package org.projectforge.web.vacation;

import java.math.BigDecimal;
import java.util.Date;
import java.util.function.Function;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.validator.PatternValidator;
import org.projectforge.business.fibu.Gender;
import org.projectforge.business.vacation.model.VacationDO;
import org.projectforge.business.vacation.service.VacationService;
import org.projectforge.framework.persistence.attr.impl.GuiAttrSchemaService;
import org.projectforge.web.common.BicValidator;
import org.projectforge.web.common.IbanValidator;
import org.projectforge.web.fibu.Kost1FormComponent;
import org.projectforge.web.user.UserSelectPanel;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.components.DatePanelSettings;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.components.MaxLengthTextField;
import org.projectforge.web.wicket.components.MaxLengthTextFieldWithRequiredSupplier;
import org.projectforge.web.wicket.components.MinMaxNumberField;
import org.projectforge.web.wicket.components.TabPanel;
import org.projectforge.web.wicket.flowlayout.AbstractFieldsetPanel;
import org.projectforge.web.wicket.flowlayout.FieldProperties;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.HtmlCommentPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;
import org.projectforge.web.wicket.flowlayout.TextAreaPanel;

import de.micromata.genome.db.jpa.tabattr.api.AttrGroup;

public class VacationEditForm extends AbstractEditForm<VacationDO, VacationEditPage>
{
  private static final long serialVersionUID = 8746545908106124484L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(VacationEditForm.class);

  private static final BigDecimal NUMBER_OF_WEEK_HOURS = new BigDecimal(168);

  @SpringBean
  private VacationService VacationService;

  @SpringBean
  private GuiAttrSchemaService attrSchemaService;

  public VacationEditForm(final VacationEditPage parentPage, final VacationDO data)
  {
    super(parentPage, data);
  }

  public static void generateCountryStateFields(GridBuilder gridBuilder, VacationDO data)
  {
    {
      // country
      final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "country");
      final MaxLengthTextField textField = new MaxLengthTextField(InputPanel.WICKET_ID,
          new PropertyModel<>(data, "country"));
      textField.setMarkupId("country").setOutputMarkupId(true);
      fs.add(textField);
    }

    {
      // state
      final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "state");
      final MaxLengthTextField textField = new MaxLengthTextField(InputPanel.WICKET_ID,
          new PropertyModel<>(data, "state"));
      textField.setMarkupId("state").setOutputMarkupId(true);
      fs.add(textField);
    }
  }

  public static void generateStreetZipCityFields(GridBuilder gridBuilder, VacationDO data)
  {
    {
      // street
      final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "street");
      final MaxLengthTextField textField = new MaxLengthTextField(InputPanel.WICKET_ID,
          new PropertyModel<>(data, "street"));
      textField.setMarkupId("street").setOutputMarkupId(true);
      fs.add(textField);
    }

    {
      // zip code
      final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "zipCode");
      final MaxLengthTextField textField = new MaxLengthTextField(InputPanel.WICKET_ID,
          new PropertyModel<>(data, "zipCode"));
      textField.setMarkupId("zipCode").setOutputMarkupId(true);
      fs.add(textField);
    }

    {
      // city
      final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "city");
      final MaxLengthTextField textField = new MaxLengthTextField(InputPanel.WICKET_ID,
          new PropertyModel<>(data, "city"));
      textField.setMarkupId("city").setOutputMarkupId(true);
      fs.add(textField);
    }
  }

  public static void createBirthdayPanel(final GridBuilder gridBuilder, VacationDO data)
  {
    // Birthday
    final FieldProperties<Date> props = new FieldProperties<>("fibu.Vacation.birthday",
        new PropertyModel<>(data, "birthday"));
    final AbstractFieldsetPanel<?> fs = gridBuilder.newFieldset(props);
    DatePanel datePanel = new DatePanel(
        fs.newChildId(),
        props.getModel(),
        DatePanelSettings.get().withTargetType(java.sql.Date.class));
    datePanel.getDateField().setMarkupId("birthday").setOutputMarkupId(true);
    fs.add(datePanel);
    fs.add(new HtmlCommentPanel(fs.newChildId(), new Model<String>()
    {
      @Override
      public String getObject()
      {
        return WicketUtils.getUTCDate("birthday", data.getBirthday());
      }
    }));
  }

  public static void createBankingDetails(final GridBuilder gridBuilder, VacationDO data)
  {
    // bank account: account holder
    final FieldsetPanel accountHolderFs = gridBuilder.newFieldset(VacationDO.class, "accountHolder");
    final MaxLengthTextFieldWithRequiredSupplier accountHolderTextField = new MaxLengthTextFieldWithRequiredSupplier(
        InputPanel.WICKET_ID,
        new PropertyModel<>(data, "accountHolder"));
    accountHolderTextField.setMarkupId("accountHolder").setOutputMarkupId(true);
    accountHolderFs.add(accountHolderTextField);

    // bank account: IBAN
    final FieldsetPanel ibanFs = gridBuilder.newFieldset(VacationDO.class, "iban");
    final MaxLengthTextFieldWithRequiredSupplier ibanTextField = new MaxLengthTextFieldWithRequiredSupplier(
        InputPanel.WICKET_ID, new PropertyModel<>(data, "iban"));
    ibanTextField.setMarkupId("iban").setOutputMarkupId(true);
    ibanTextField.add(new IbanValidator());
    ibanFs.add(ibanTextField);

    // validation: if one of account holder or IBAN is set, the other one has also to be set
    ibanTextField.setRequiredSupplier(() -> StringUtils.isNotBlank(accountHolderTextField.getValue()));
    accountHolderTextField.setRequiredSupplier(() -> StringUtils.isNotBlank(ibanTextField.getValue()));

    // bank account: BIC
    final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "bic");
    final MaxLengthTextField textField = new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<>(data, "bic"));
    textField.setMarkupId("bic").setOutputMarkupId(true);
    textField.add(new BicValidator());
    fs.add(textField);
  }

  @Override
  protected void init()
  {
    super.init();

    // replace the GridBuilder from superclass by our TabPanel
    final TabPanel tabPanel = new TabPanel("flowform");
    replace(tabPanel);
    gridBuilder = tabPanel.getOrCreateTab("fibu.Vacation.coredata", true); // create the default tab

    gridBuilder.newSplitPanel(GridSize.COL50, true).newSubSplitPanel(GridSize.COL100);
    {
      // User
      final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "user");
      final UserSelectPanel userSelectPanel = new UserSelectPanel(fs.newChildId(),
          new PropertyModel<>(data, "user"), parentPage,
          "userId");
      userSelectPanel.getWrappedComponent().setMarkupId("user").setOutputMarkupId(true);
      userSelectPanel.setShowSelectMeButton(false).setRequired(true);
      fs.add(userSelectPanel);
      userSelectPanel.init();
    }
    {
      // cost 1
      final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "kost1");
      Kost1FormComponent kost1 = new Kost1FormComponent(InputPanel.WICKET_ID, new PropertyModel<>(data, "kost1"), true);
      kost1.setMarkupId("kost1").setOutputMarkupId(true);
      fs.add(kost1);
    }
    {
      // DropDownChoice status
      final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "status");
      final LabelValueChoiceRenderer<VacationStatus> statusChoiceRenderer = new LabelValueChoiceRenderer<>(
          this,
          VacationStatus.values());
      final DropDownChoice<VacationStatus> statusChoice = new DropDownChoice<>(fs.getDropDownChoiceId(),
          new PropertyModel<>(data, "status"), statusChoiceRenderer.getValues(), statusChoiceRenderer);
      statusChoice.setNullValid(false).setRequired(true);
      statusChoice.setMarkupId("status").setOutputMarkupId(true);
      fs.add(statusChoice);
    }
    {
      // Division
      final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "abteilung");
      MaxLengthTextField abteilung = new MaxLengthTextField(InputPanel.WICKET_ID,
          new PropertyModel<>(data, "abteilung"));
      abteilung.setMarkupId("abteilung").setOutputMarkupId(true);
      fs.add(abteilung);
    }
    {
      // Position
      final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "position");
      MaxLengthTextField position = new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<>(data, "position"));
      position.setMarkupId("position").setOutputMarkupId(true);
      fs.add(position);
    }

    gridBuilder.newSplitPanel(GridSize.COL50, true).newSubSplitPanel(GridSize.COL100);
    {
      // Staff number
      final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "staffNumber");
      MaxLengthTextField position = new MaxLengthTextField(InputPanel.WICKET_ID,
          new PropertyModel<>(data, "staffNumber"));
      position.setMarkupId("staffNumber").setOutputMarkupId(true);
      position.add(new PatternValidator("[a-zA-Z0-9]*"));
      fs.add(position);
    }
    {
      // Weekly hours
      final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "weeklyWorkingHours");
      fs.add(new MinMaxNumberField<>(InputPanel.WICKET_ID,
          new PropertyModel<>(data, "weeklyWorkingHours"), BigDecimal.ZERO, NUMBER_OF_WEEK_HOURS));
    }
    {
      // Holidays
      final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "urlaubstage");
      fs.add(new MinMaxNumberField<>(InputPanel.WICKET_ID, new PropertyModel<>(data, "urlaubstage"), 0, 366));
    }
    {
      // Start date
      final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "eintrittsDatum");
      fs.add(new DatePanel(fs.newChildId(), new PropertyModel<>(data, "eintrittsDatum"), new DatePanelSettings()));
    }
    {
      // End date
      final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "austrittsDatum");
      fs.add(new DatePanel(fs.newChildId(), new PropertyModel<>(data, "austrittsDatum"), new DatePanelSettings()));
    }

    gridBuilder.newSplitPanel(GridSize.COL50, true);
    gridBuilder.newSubSplitPanel(GridSize.COL50);
    generateStreetZipCityFields(gridBuilder, data);

    gridBuilder.newSubSplitPanel(GridSize.COL50);
    generateCountryStateFields(gridBuilder, data);

    gridBuilder.newSplitPanel(GridSize.COL25, true).newSubSplitPanel(GridSize.COL100);
    createBirthdayPanel(gridBuilder, data);

    {
      // DropDownChoice gender
      final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "gender");
      final LabelValueChoiceRenderer<Gender> genderChoiceRenderer = new LabelValueChoiceRenderer<>(this,
          Gender.values());
      final DropDownChoice<Gender> statusChoice = new DropDownChoice<>(
          fs.getDropDownChoiceId(),
          new PropertyModel<>(data, "gender"),
          genderChoiceRenderer.getValues(),
          genderChoiceRenderer);
      statusChoice.setNullValid(false).setRequired(true);
      statusChoice.setMarkupId("gender").setOutputMarkupId(true);
      fs.add(statusChoice);
    }
    gridBuilder.newSplitPanel(GridSize.COL25, true).newSubSplitPanel(GridSize.COL100);
    createBankingDetails(gridBuilder, data);

    gridBuilder.newSplitPanel(GridSize.COL100, true); // set hasSubSplitPanel to true to remove borders from this split panel
    {
      // AttrPanels
      final Function<AttrGroup, VacationTimedDO> addNewEntryFunction = group -> VacationService
          .addNewTimeAttributeRow(data, group.getName());
      attrSchemaService.createAttrPanels(tabPanel, data, parentPage, addNewEntryFunction);
    }

    gridBuilder.newSplitPanel(GridSize.COL100, true).newSubSplitPanel(GridSize.COL100);
    {
      // Comment
      final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "comment");
      fs.add(new MaxLengthTextArea(TextAreaPanel.WICKET_ID, new PropertyModel<>(data, "comment")), true);
    }
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
