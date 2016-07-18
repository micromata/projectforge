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

package org.projectforge.web.wicket.flowlayout;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.projectforge.web.wicket.WicketUtils;

/**
 * Panel containing only one check-box.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@SuppressWarnings("serial")
public class CheckBoxButton extends Panel implements ComponentWrapperPanel
{
  public static final String WICKET_ID = "checkBox";

  private CheckBox checkBox;

  private Label label;

  private WebMarkupContainer labelContainer;

  private boolean wantOnSelectionChangedNotifications;

  /**
   * @param id
   * @param model
   * @param labelString If null then a classic checkbox is used.
   */
  public CheckBoxButton(final String id, final IModel<Boolean> model, final String labelString)
  {
    this(id, model, labelString, false);
  }

  /**
   * @param id
   * @param model
   * @param labelString If null then a classic checkbox is used.
   * @param wantOnSelectionChangedNotifications if true then wantOnSelectionChangedNotifications method returns true.
   * @see CheckBox#wantOnSelectionChangedNotifications()
   */
  public CheckBoxButton(final String id, final IModel<Boolean> model, final String labelString,
      final boolean wantOnSelectionChangedNotifications)
  {
    super(id);
    this.wantOnSelectionChangedNotifications = wantOnSelectionChangedNotifications;
    checkBox = new CheckBox(WICKET_ID, model) {
      @Override
      public void onSelectionChanged(final Boolean newSelection)
      {
        CheckBoxButton.this.onSelectionChanged(newSelection);
      }

      @Override
      protected boolean wantOnSelectionChangedNotifications()
      {
        return CheckBoxButton.this.wantOnSelectionChangedNotifications();
      }
    };
    checkBox.setOutputMarkupId(true);
    init(labelString);
    labelContainer.add(checkBox);
  }

  public CheckBoxButton(final String id, final CheckBox checkBox, final String labelString)
  {
    super(id);
    this.checkBox = checkBox;
    init(labelString);
    labelContainer.add(checkBox);
  }

  private void init(final String labelString)
  {
    labelContainer = new WebMarkupContainer("labelTag");
    add(labelContainer);
    label = new Label("labelText", labelString);
    label.setRenderBodyOnly(true);
    labelContainer.add(label);
  }

  /**
   * Sets tool-tip for the label.
   * @param tooltip
   * @return this for chaining.
   */
  public CheckBoxButton setTooltip(final String tooltip)
  {
    WicketUtils.addTooltip(labelContainer, tooltip);
    return this;
  }

  /**
   * Sets tool-tip for the label.
   * @param tooltip
   * @return this for chaining.
   */
  public CheckBoxButton setTooltip(final String title, final String text)
  {
    WicketUtils.addTooltip(labelContainer, title, text);
    return this;
  }

  /**
   * If activated then the check box is colored (red). This is useful for checkboxes which have an important rule on something other, e. g.
   * "show only deleted" check-box in list view should be highlighted.
   * @return
   */
  public CheckBoxButton setWarning()
  {
    labelContainer.add(AttributeModifier.append("class", "warning"));
    return this;
  }

  /**
   * @see CheckBox#onSelectionChanged()
   */
  protected void onSelectionChanged(final Boolean newSelection)
  {
  }

  /**
   * @see CheckBox#wantOnSelectionChangedNotifications()
   */
  protected boolean wantOnSelectionChangedNotifications()
  {
    return wantOnSelectionChangedNotifications;
  }

  public CheckBox getCheckBox()
  {
    return checkBox;
  }

  public CheckBoxButton setSelected(final boolean selected)
  {
    checkBox.setDefaultModelObject(selected);
    return this;
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel#getComponentOutputId()
   */
  @Override
  public String getComponentOutputId()
  {
    checkBox.setOutputMarkupId(true);
    return checkBox.getMarkupId();
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel#getFormComponent()
   */
  @Override
  public FormComponent< ? > getFormComponent()
  {
    return checkBox;
  }
}
