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
 * Panel containing only one check-box. <br/>
 * This component calls setRenderBodyOnly(true). If the outer html element is needed, please call setRenderBodyOnly(false).
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@SuppressWarnings("serial")
public class CheckBoxPanel extends Panel implements ComponentWrapperPanel
{
  public static final String WICKET_ID = "checkBox";

  private CheckBox checkBox;

  private Label label;

  private WebMarkupContainer parentContainer;

  private boolean wantOnSelectionChangedNotifications;

  /**
   * @param id
   * @param model
   * @param labelString If null then a classic checkbox is used.
   */
  public CheckBoxPanel(final String id, final IModel<Boolean> model, final String labelString)
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
  public CheckBoxPanel(final String id, final IModel<Boolean> model, final String labelString,
      final boolean wantOnSelectionChangedNotifications)
  {
    super(id);
    this.parentContainer = new WebMarkupContainer("parent");
    add(this.parentContainer);
    this.wantOnSelectionChangedNotifications = wantOnSelectionChangedNotifications;
    checkBox = new CheckBox(WICKET_ID, model) {
      @Override
      public void onSelectionChanged(final Boolean newSelection)
      {
        CheckBoxPanel.this.onSelectionChanged(newSelection);
      }

      @Override
      protected boolean wantOnSelectionChangedNotifications()
      {
        return CheckBoxPanel.this.wantOnSelectionChangedNotifications();
      }
    };
    checkBox.setOutputMarkupId(true);
    this.parentContainer.add(checkBox);
    init(labelString);
  }

  public CheckBoxPanel(final String id, final CheckBox checkBox, final String labelString)
  {
    super(id);
    this.checkBox = checkBox;
    add(checkBox);
    init(labelString);
  }

  private void init(final String labelString)
  {
    if (labelString != null) {
      label = new Label("label", labelString);
      label.add(AttributeModifier.replace("for", checkBox.getMarkupId()));
      label.setRenderBodyOnly(true);
    } else {
      label = new Label("label");
      label.setVisible(false);
    }
    this.parentContainer.add(label);
    setRenderBodyOnly(true);
  }

  /**
   * Sets tool-tip for the label.
   * @param tooltip
   * @return this for chaining.
   */
  public CheckBoxPanel setTooltip(final String tooltip)
  {
    WicketUtils.addTooltip(this.parentContainer, tooltip);
    return this;
  }

  /**
   * Sets tool-tip for the label.
   * @param tooltip
   * @return this for chaining.
   */
  public CheckBoxPanel setTooltip(final String title, final String text)
  {
    WicketUtils.addTooltip(this.parentContainer, title, text);
    return this;
  }

  /**
   * If activated then the check box is colored (red). This is useful for checkb-oxes which have an important rule on something other, e. g.
   * "show only deleted" check-box in list view should be highlighted.
   * @return
   */
  public CheckBoxPanel setWarning()
  {
    if (label.isVisible() == true) {
      label.add(AttributeModifier.append("class", "warning"));
    } else {
      checkBox.add(AttributeModifier.append("class", "warning"));
    }
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

  public CheckBoxPanel setSelected(final boolean selected)
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
