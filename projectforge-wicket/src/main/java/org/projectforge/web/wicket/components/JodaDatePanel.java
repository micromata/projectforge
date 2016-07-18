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

package org.projectforge.web.wicket.components;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.joda.time.DateMidnight;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.WicketRenderHeadUtils;
import org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel;

/**
 * Panel for date selection.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class JodaDatePanel extends Panel implements ComponentWrapperPanel
{
  private static final long serialVersionUID = 3785639935585959803L;

  protected JodaDateField dateField;

  protected boolean autosubmit;

  /**
   * @param id
   * @param label Only for displaying the field's name on validation messages.
   * @param model
   */
  public JodaDatePanel(final String id, final IModel<DateMidnight> model)
  {
    super(id);
    dateField = new JodaDateField("dateField", model);
    dateField.add(AttributeModifier.replace("size", "10"));
    dateField.setOutputMarkupId(true);
    add(dateField);
  }

  /**
   * @see org.apache.wicket.Component#renderHead(org.apache.wicket.markup.html.IHeaderResponse)
   */
  @Override
  public void renderHead(final IHeaderResponse response)
  {
    super.renderHead(response);
    WicketRenderHeadUtils.renderMainJavaScriptIncludes(response);
    DatePickerUtils.renderHead(response, getLocale(), dateField.getMarkupId(), autosubmit);
  }

  /**
   * @see org.apache.wicket.markup.html.form.FormComponent#setLabel(org.apache.wicket.model.IModel)
   */
  public JodaDatePanel setLabel(final IModel<String> labelModel)
  {
    dateField.setLabel(labelModel);
    return this;
  }

  public JodaDatePanel setFocus()
  {
    dateField.add(WicketUtils.setFocus());
    return this;
  }

  /**
   * @param autosubmit the autosubmit to set
   * @return this for chaining.
   */
  public JodaDatePanel setAutosubmit(final boolean autosubmit)
  {
    this.autosubmit = autosubmit;
    return this;
  }

  public JodaDateField getDateField()
  {
    return dateField;
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel#getComponentOutputId()
   */
  @Override
  public String getComponentOutputId()
  {
    return dateField.getMarkupId();
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel#getFormComponent()
   */
  @Override
  public FormComponent< ? > getFormComponent()
  {
    return dateField;
  }
}
