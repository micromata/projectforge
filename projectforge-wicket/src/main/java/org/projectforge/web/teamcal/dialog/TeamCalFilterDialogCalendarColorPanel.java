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

package org.projectforge.web.teamcal.dialog;

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.filter.TemplateCalendarProperties;
import org.projectforge.business.teamcal.filter.TemplateEntry;
import org.projectforge.web.common.ColorPickerPanel;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.flowlayout.CheckBoxPanel;

/**
 * Inner class to represent a single calendar color and visibility panel.
 */
public class TeamCalFilterDialogCalendarColorPanel extends Panel
{
  private static final long serialVersionUID = -4596590985776103813L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
      .getLogger(TeamCalFilterDialogCalendarColorPanel.class);

  private RepeatingView columnRepeater;

  final WebMarkupContainer main;

  /**
   * @param id
   */
  public TeamCalFilterDialogCalendarColorPanel(final String id)
  {
    super(id);
    main = new WebMarkupContainer("main");
    main.setOutputMarkupId(true);
    add(main);
  }

  /**
   * @see org.apache.wicket.Component#onInitialize()
   */
  @Override
  protected void onInitialize()
  {
    super.onInitialize();
    columnRepeater = new RepeatingView("columnRepeater");
    main.add(columnRepeater);
  }

  /**
   * @see org.apache.wicket.Component#onBeforeRender()
   */
  @SuppressWarnings("serial")
  protected void redraw(final TemplateEntry activeTemplateEntry, final List<TeamCalDO> selectedCalendars)
  {
    columnRepeater.removeAll();
    final int counter = selectedCalendars.size();
    int rowsPerColumn = counter / 3;
    if (counter % 3 > 0) {
      ++rowsPerColumn; // Need one more row.
    }
    int rowCounter = 0;
    WebMarkupContainer columnContainer;
    RepeatingView rowRepeater = null;
    for (final TeamCalDO calendar : selectedCalendars) {
      if (rowCounter++ % rowsPerColumn == 0) {
        // Start new column:
        columnContainer = new WebMarkupContainer(columnRepeater.newChildId());
        columnContainer.add(AttributeModifier.append("class", GridSize.COL33.getClassAttrValue()));
        columnRepeater.add(columnContainer);
        rowRepeater = new RepeatingView("rowRepeater");
        columnContainer.add(rowRepeater);
      }
      final WebMarkupContainer container = new WebMarkupContainer(rowRepeater.newChildId());
      rowRepeater.add(container);
      final IModel<Boolean> model = Model.of(activeTemplateEntry.isVisible(calendar.getId()) == true);
      final CheckBoxPanel checkBoxPanel = new CheckBoxPanel("isVisible", model, "");
      checkBoxPanel.getCheckBox().add(new AjaxFormComponentUpdatingBehavior("change")
      {
        private static final long serialVersionUID = 3523446385818267608L;

        @Override
        protected void onUpdate(final AjaxRequestTarget target)
        {
          final Boolean newSelection = checkBoxPanel.getCheckBox().getConvertedInput();
          final TemplateCalendarProperties properties = activeTemplateEntry.getCalendarProperties(calendar.getId());
          if (newSelection != properties.isVisible()) {
            properties.setVisible(newSelection);
            activeTemplateEntry.setDirty();
          }
        }
      });
      container.add(checkBoxPanel);
      WicketUtils.addTooltip(checkBoxPanel.getCheckBox(),
          getString("plugins.teamcal.filterDialog.calendarIsVisible.tooltip"));
      container.add(new Label("name", calendar.getTitle()));
      final ColorPickerPanel picker = new ColorPickerPanel("colorPicker",
          activeTemplateEntry.getColorCode(calendar.getId()))
      {
        @Override
        protected void onColorUpdate(final String selectedColor)
        {
          final TemplateCalendarProperties props = activeTemplateEntry.getCalendarProperties(calendar.getId());
          if (props != null) {
            props.setColorCode(selectedColor);
          } else {
            log.warn("TeamCalendarProperties not found: calendar.id='"
                + calendar.getId()
                + "' + for active template '"
                + activeTemplateEntry.getName()
                + "'.");
          }
        }
      };
      container.add(picker);
    }
  }
}
