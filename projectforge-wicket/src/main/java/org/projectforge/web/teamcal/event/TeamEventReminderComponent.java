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

package org.projectforge.web.teamcal.event;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.projectforge.business.teamcal.event.model.ReminderActionType;
import org.projectforge.business.teamcal.event.model.ReminderDurationUnit;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.common.i18n.I18nEnum;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.MinMaxNumberField;
import org.projectforge.web.wicket.flowlayout.DropDownChoicePanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

/**
 * Component renders DropDown to select reminder action, TextField to set duration and DropDown to select duration type.
 *
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 */
public class TeamEventReminderComponent extends Component
{
  private static final long serialVersionUID = -7384630904654370695L;

  private static final Integer DURATION_MAX = 1000;

  private final TeamEventDO data;

  @SuppressWarnings("unused")
  // used by wicked
  private List<ReminderActionType> reminderActionTypeList;

  @SuppressWarnings("unused")
  // used by wicked
  private List<ReminderDurationUnit> reminderDurationTypeList;

  private final FieldsetPanel reminderPanel;

  /**
   * @param id
   * @param model
   */
  public TeamEventReminderComponent(final String id, final IModel<TeamEventDO> model, final FieldsetPanel fieldSet)
  {
    super(id, model);
    data = model.getObject();
    this.reminderPanel = fieldSet;
  }

  /**
   * @see org.apache.wicket.Component#onInitialize()
   */
  @SuppressWarnings({ "unchecked", "serial" })
  @Override
  protected void onInitialize()
  {
    super.onInitialize();

    final boolean reminderOptionVisibility = data.getReminderActionType() != null;

    // ### unchecked
    reminderDurationTypeList = (List<ReminderDurationUnit>) getTypeList(ReminderDurationUnit.class);
    reminderActionTypeList = (List<ReminderActionType>) getTypeList(ReminderActionType.class);
    final IChoiceRenderer<ReminderDurationUnit> reminderDurationTypeRenderer = (IChoiceRenderer<ReminderDurationUnit>) getChoiceRenderer(
        ReminderDurationUnit.class); //
    final IChoiceRenderer<ReminderActionType> reminderActionTypeRenderer = (IChoiceRenderer<ReminderActionType>) getChoiceRenderer(
        ReminderActionType.class); //
    // ###

    final MinMaxNumberField<Integer> reminderDuration = new MinMaxNumberField<Integer>(reminderPanel.getTextFieldId(),
        new PropertyModel<Integer>(data, "reminderDuration"), 0, DURATION_MAX);
    WicketUtils.setSize(reminderDuration, 3);
    setComponentProperties(reminderDuration, reminderOptionVisibility, true);

    // reminder duration dropDown
    final IModel<List<ReminderDurationUnit>> reminderDurationChoicesModel = new PropertyModel<List<ReminderDurationUnit>>(
        this,
        "reminderDurationTypeList");
    final IModel<ReminderDurationUnit> reminderDurationActiveModel = new PropertyModel<ReminderDurationUnit>(data,
        "reminderDurationType");
    final DropDownChoicePanel<ReminderDurationUnit> reminderDurationTypeChoice = new DropDownChoicePanel<ReminderDurationUnit>(
        reminderPanel.newChildId(), reminderDurationActiveModel, reminderDurationChoicesModel,
        reminderDurationTypeRenderer, false);
    setComponentProperties(reminderDurationTypeChoice.getDropDownChoice(), reminderOptionVisibility, true);

    // reminder action dropDown
    final IModel<List<ReminderActionType>> reminderActionTypeChoiceModel = new PropertyModel<List<ReminderActionType>>(
        this,
        "reminderActionTypeList");
    final IModel<ReminderActionType> reminderActionActiveModel = new PropertyModel<ReminderActionType>(data,
        "reminderActionType");
    final DropDownChoicePanel<ReminderActionType> reminderActionTypeChoice = new DropDownChoicePanel<ReminderActionType>(
        reminderPanel.newChildId(),
        new DropDownChoice<ReminderActionType>(DropDownChoicePanel.WICKET_ID, reminderActionActiveModel,
            reminderActionTypeChoiceModel, reminderActionTypeRenderer)
        {
          /**
           * @see org.apache.wicket.markup.html.form.AbstractSingleSelectChoice#getNullKey()
           */
          @Override
          protected String getNullValidKey()
          {
            return "plugins.teamcal.event.reminder.NONE";
          }
        });
    reminderActionTypeChoice.setNullValid(true);
    reminderActionTypeChoice.getDropDownChoice().add(new AjaxFormComponentUpdatingBehavior("onchange")
    {

      @Override
      protected void onUpdate(final AjaxRequestTarget target)
      {
        final boolean isVisible = data.getReminderActionType() != null;
        if (isVisible == true) {
          // Pre-set default values if the user selects a reminder action:
          if (NumberHelper.greaterZero(data.getReminderDuration()) == false) {
            data.setReminderDuration(15);
          }
          if (data.getReminderDurationUnit() == null) {
            data.setReminderDurationUnit(ReminderDurationUnit.MINUTES);
          }
        }
        reminderDuration.setVisible(isVisible);
        reminderDurationTypeChoice.getDropDownChoice().setVisible(isVisible);
        reminderDurationTypeChoice.setRequired(isVisible);
        target.add(reminderDurationTypeChoice.getDropDownChoice(), reminderDuration);
      }

    });
    reminderPanel.add(reminderActionTypeChoice);
    reminderPanel.add(reminderDuration);
    reminderPanel.add(reminderDurationTypeChoice);
  }

  private void setComponentProperties(final FormComponent<?> comp, final boolean visible, final boolean markUp)
  {
    comp.setVisible(visible);
    comp.setRequired(visible);
    comp.setOutputMarkupId(markUp);
    comp.setOutputMarkupPlaceholderTag(markUp);
  }

  private List<? extends I18nEnum> getTypeList(final Class<? extends I18nEnum> obj)
  {
    final List<I18nEnum> list = new ArrayList<I18nEnum>();
    for (final I18nEnum type : obj.getEnumConstants()) {
      list.add(type);
    }
    return list;
  }

  private IChoiceRenderer<? extends I18nEnum> getChoiceRenderer(final Class<? extends I18nEnum> c)
  {
    final IChoiceRenderer<I18nEnum> reminderActionTypeRenderer = new IChoiceRenderer<I18nEnum>()
    {

      private static final long serialVersionUID = -4264875398872979820L;

      @Override
      public Object getDisplayValue(final I18nEnum object)
      {
        return getString(object.getI18nKey());
      }

      @Override
      public String getIdValue(final I18nEnum object, final int index)
      {
        return object.toString();
      }

      @Override
      public I18nEnum getObject(final String s, final IModel<? extends List<? extends I18nEnum>> iModel)
      {
        if (s == null) {
          return null;
        }

        for (I18nEnum instance : iModel.getObject()) {
          // TODO sn migration
          if (s.equals(instance.toString())) {
            return instance;
          }
        }

        return null;
      }
    };
    return reminderActionTypeRenderer;
  }

  /**
   * @see org.apache.wicket.Component#onRender()
   */
  @Override
  protected void onRender()
  {

  }
}
