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

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.model.IModel;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.fibu.ISelectCallerPage;

/**
 * Base class for selecting and unselecting items of type T.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public abstract class AbstractSelectPanel<T> extends FormComponentPanel<T>
{
  private static final long serialVersionUID = 1017917415565003328L;

  private boolean initialized = false;

  protected ISelectCallerPage caller;

  protected String selectProperty;

  protected boolean showFavorites = true;

  // Tab index for the favorites drop down choice.
  protected Integer tabIndex;

  public AbstractSelectPanel(final String id, final IModel<T> model, final ISelectCallerPage caller, final String selectProperty)
  {
    super(id, model);
    this.caller = caller;
    this.selectProperty = selectProperty;
  }

  /**
   * Throws UnsupportedOperationException if not implemented by the derived class.
   */
  public AbstractSelectPanel<T> setFocus()
  {
    throw new UnsupportedOperationException("Not yet implemented.");
  }

  /**
   * Override this method and don't forget to call super.init() for mark this panel as initialized.
   * @see #onBeforeRender()
   */
  public AbstractSelectPanel<T> init()
  {
    initialized = true;
    return this;
  }

  /**
   * If true (default) then the favorite tasks will be shown in a drop down choice for selection.
   * @param showFavorites
   */
  public AbstractSelectPanel<T> setShowFavorites(final boolean showFavorites)
  {
    this.showFavorites = showFavorites;
    return this;
  }

  /**
   * If given then the favorites drop down choice will get this html tab index.
   * @param tabIndex
   */
  public AbstractSelectPanel<T> setTabIndex(final Integer tabIndex)
  {
    this.tabIndex = tabIndex;
    return this;
  }

  /**
   * The length, break before and intend will be added as class attribute to this component.
   * @return null if not overridden.
   */
  public Component getClassModifierComponent()
  {
    return null;
  }

  /**
   * Used for setting label for (see
   * {@link WicketUtils#setLabel(org.apache.wicket.markup.html.form.FormComponent, org.apache.wicket.markup.html.basic.Label)}.
   * @return null if not overridden.
   */
  public Component getWrappedComponent()
  {
    return null;
  }

  /**
   * @see org.apache.wicket.Component#onBeforeRender()
   * @throws RuntimeException if component is not initialized (forgotten call of init()).
   */
  @Override
  protected void onBeforeRender()
  {
    if (initialized == false) {
      throw new RuntimeException("Select panel is not initialized. Maybe you have forgotten to call init() method for component: "
          + getId());
    }
    super.onBeforeRender();
  }
}
