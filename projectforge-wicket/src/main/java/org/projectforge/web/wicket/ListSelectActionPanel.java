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

package org.projectforge.web.wicket;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.framework.utils.ReflectionHelper;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.components.PlainLabel;

/**
 * Panel for selecting list page entries for editing and selecting for callers.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@SuppressWarnings("serial")
public class ListSelectActionPanel extends Panel
{
  public static final String LABEL_ID = "label";

  public static final String LINK_ID = "select";

  /**
   * Constructor for list view in selection mode.
   * @param id component id
   * @param model model for contact
   * @param caller The calling page.
   * @param selectProperty The property (name) of the caller to select.
   * @param objectId The id of the object to select on click.
   * @param label The label string to show (additional to the row_pointer.png).
   */
  public ListSelectActionPanel(final String id, final IModel< ? > model, final ISelectCallerPage caller, final String selectProperty,
      final Integer objectId, final String label)
  {
    this(id, model, caller, selectProperty, objectId, new PlainLabel(LABEL_ID, label));
  }

  /**
   * Constructor for list view in selection mode.
   * @param id component id
   * @param model model for contact
   * @param caller The calling page.
   * @param selectProperty The property (name) of the caller to select.
   * @param objectId The id of the object to select on click.
   * @param label The label to show (additional to the row_pointer.png). The id of the label should be LABEL_ID.
   */
  public ListSelectActionPanel(final String id, final IModel< ? > model, final ISelectCallerPage caller, final String selectProperty,
      final Integer objectId, final Label label)
  {
    super(id, model);
    setRenderBodyOnly(true);
    final Link< ? > link = new Link<Void>("select") {
      @Override
      public void onClick()
      {
        WicketUtils.setResponsePage(this, caller);
        caller.select(selectProperty, objectId);
      };
    };
    add(link);
    add(label);
  }

  /**
   * Constructor for normal list view for selecting one entry to edit.
   * @param id component id
   * @param model model for contact
   * @param editClass The edit page to redirect to.
   * @param objectId The id of the object to edit in edit page.
   * @param label The label string to show (additional to the row_pointer.png).
   * @param params Pairs of params (key, value).
   * @see WicketUtils#getPageParameters(String[])
   */
  public ListSelectActionPanel(final String id, final IModel< ? > model, final Class< ? extends WebPage> editClass, final Integer objectId,
      final WebPage returnToPage, final String label, final String... params)
  {
    this(id, model, editClass, objectId, returnToPage, new PlainLabel(LABEL_ID, label), params);
  }

  /**
   * Constructor for normal list view for selecting one entry to edit.
   * @param id component id
   * @param model model for contact
   * @param editPageClass The edit page to redirect to.
   * @param objectId The id of the object to edit in edit page.
   * @param label The label to show (additional to the row_pointer.png). The id of the label should be LABEL_ID.
   * @param params Pairs of params (key, value).
   * @see WicketUtils#getPageParameters(String[])
   */
  public ListSelectActionPanel(final String id, final IModel< ? > model, final Class< ? extends WebPage> editPageClass,
      final Integer objectId, final WebPage returnToPage, final Label label, final String... params)
  {
    super(id, model);
    setRenderBodyOnly(true);
    final Link< ? > link = new Link<Void>(LINK_ID) {
      @Override
      public void onClick()
      {
        final PageParameters pageParams = WicketUtils.getPageParameters(params);
        if (objectId != null) {
          pageParams.add(AbstractEditPage.PARAMETER_KEY_ID, String.valueOf(objectId));
        }
        final AbstractSecuredPage editPage = (AbstractSecuredPage) ReflectionHelper.newInstance(editPageClass, PageParameters.class,
            pageParams);
        if (editPage instanceof AbstractEditPage) {
          ((AbstractEditPage< ? , ? , ? >) editPage).setReturnToPage(returnToPage);
        }
        setResponsePage(editPage);
      };
    };
    add(link);
    add(label);
  }

  public ListSelectActionPanel(final String id, final AbstractLink link, final Model<String> label)
  {
    this(id, link, new Label(LABEL_ID, label));
  }

  public ListSelectActionPanel(final String id, final AbstractLink link, final Label label)
  {
    super(id);
    add(link);
    add(label);
  }
}
