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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.markup.repeater.RepeatingView;

/**
 * List of components which are built in the order of the list. The RepeatingView is generated after creating this list (is used e. g. for
 * abstract pages or forms for buttons, therefore the derived class can insert buttons in the list of the super class.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class MyComponentsRepeater<T extends Component> implements Serializable
{
  private static final long serialVersionUID = -5889196867277720505L;

  private final List<T> components = new ArrayList<T>();

  // Needed for generating RepeatingView in onBeforeRender() if not already generated.
  private boolean rendered = false;

  private final RepeatingView repeatingView;

  public MyComponentsRepeater(final String repeatingViewId)
  {
    repeatingView = new RepeatingView(repeatingViewId);
  }

  public boolean hasEntries()
  {
    return CollectionUtils.isNotEmpty(this.components);
  }

  /**
   * @param visible
   * @return this for chaining.
   * @see Component#setVisible(boolean)
   */
  public MyComponentsRepeater<T> setVisibility(final boolean visible) {
    repeatingView.setVisible(visible);
    return this;
  }

  /**
   * @param component
   * @return this for chaining.
   */
  public MyComponentsRepeater<T> add(final T component)
  {
    components.add(component);
    return this;
  }

  /**
   * @return new child id of the repeating view.
   * @see RepeatingView#newChildId()
   */
  public String newChildId()
  {
    return repeatingView.newChildId();
  }

  /**
   * 
   * @param pos
   * @param component
   * @return
   * @see ArrayList#add(int, Object)
   */
  public MyComponentsRepeater<T> add(final int pos, final T component)
  {
    components.add(pos, component);
    return this;
  }

  /**
   * Number of contained components in the list.
   * @see ArrayList#size()
   */
  public int size()
  {
    return components.size();
  }

  /**
   * @return the list of components.
   */
  public List<T> getList()
  {
    return components;
  }

  /**
   * @return the repeatingView
   */
  public RepeatingView getRepeatingView()
  {
    return repeatingView;
  }

  /**
   * Add all the components to the repeating view (if not already added). Should be used e. g. in onBeforeRender().
   */
  public void render()
  {
    if (rendered == false) {
      if (hasEntries() == false) {
        setVisibility(false);
      } else {
        setVisibility(true);
        if (components.size() > 0) {
          for (final T component : this.components) {
            this.repeatingView.add(component);
          }
        }
      }
      rendered = true;
    }
  }
}
