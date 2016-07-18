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

import java.util.List;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.form.select.IOptionRenderer;
import org.apache.wicket.extensions.markup.html.form.select.Select;
import org.apache.wicket.extensions.markup.html.form.select.SelectOptions;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.projectforge.web.wicket.WicketUtils;

/**
 * Panel containing only one select box. <br/>
 * This component calls setRenderBodyOnly(true). If the outer html element is needed, please call
 * setRenderBodyOnly(false).
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@SuppressWarnings("serial")
public class SelectPanel<T> extends Panel implements ComponentWrapperPanel
{
  public static final String WICKET_ID = "select";

  public static final String OPTIONS_WICKET_ID = "options";

  private Select<T> select;

  /**
   * 
   * @param id
   * @param label see {@link FormComponent#setLabel(IModel)}
   * @param model
   * @param values
   * @param renderer
   */
  public SelectPanel(final String id, final IModel<T> model)
  {
    this(id, new Select<T>(WICKET_ID, model));
  }

  /**
   * @param id
   * @param label see {@link FormComponent#setLabel(IModel)}
   * @param select
   */
  public SelectPanel(final String id, final Select<T> select)
  {
    super(id);
    this.select = select;
    add(select);
    setRenderBodyOnly(true);
  }

  public SelectPanel<T> setRequired(final boolean required)
  {
    select.setRequired(required);
    return this;
  }

  public Select<T> getSelect()
  {
    return select;
  }

  /**
   * @param list
   * @param renderer
   * @return this for chaining.
   */
  public SelectPanel<T> addOrReplaceOptions(final List<T> list, final IOptionRenderer<T> renderer)
  {
    final SelectOptions<T> options = new SelectOptions<T>(OPTIONS_WICKET_ID, list, renderer);
    select.addOrReplace(options);
    return this;
  }

  /**
   * Adds attribute onchange="javascript:submit();"
   * 
   * @return This for chaining.
   */
  public SelectPanel<T> setAutoSubmit()
  {
    this.select.add(new AjaxEventBehavior("onchange")
    {
      @Override
      protected void onEvent(final AjaxRequestTarget target)
      {
        onChange(target);
      }
    });
    return this;
  }

  /**
   * Is called onchange event, but only if {@link #setAutoSubmit()} is called first.
   * 
   * @param target
   */
  protected void onChange(final AjaxRequestTarget target)
  {
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel#getComponentOutputId()
   */
  @Override
  public String getComponentOutputId()
  {
    select.setOutputMarkupId(true);
    return select.getMarkupId();
  }

  /**
   * Sets tool-tip for the label.
   * 
   * @param tooltip
   * @return this for chaining.
   */
  public SelectPanel<T> setTooltip(final String tooltip)
  {
    WicketUtils.addTooltip(select, tooltip);
    return this;
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel#getFormComponent()
   */
  @Override
  public FormComponent<?> getFormComponent()
  {
    return select;
  }
}
