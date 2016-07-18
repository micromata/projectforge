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
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.projectforge.web.wicket.WicketUtils;

/**
 * List of components should be updated after every Ajax call. They will be added to the AjaxTarget.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class MyAjaxComponentHolder implements Serializable
{
  private static final long serialVersionUID = 8802349202112158070L;

  private final List<Component> components = new ArrayList<Component>();

  /**
   * Adds the given component and calls {@link Component#setOutputMarkupId(boolean)}.
   * @param component
   */
  public void register(final Component component)
  {
    components.add(component.setOutputMarkupId(true));
  }

  /**
   * Adds all registered components to the given AjaxRequestTarget.
   * @param target
   */
  public void addTargetComponents(final AjaxRequestTarget target)
  {
    for (final Component component : components) {
      target.add(component);
    }
  }

  /**
   * Remove the registered component because it's removed from the DOM model, therefore it's not needed to update anymore inside the AJAX
   * target. Any descendant component of the given one will be removed to.
   * @param component
   */
  public void remove(final Component component)
  {
    final Iterator<Component> it = components.iterator();
    while (it.hasNext() == true) {
      final Component comp = it.next();
      if (comp == component || WicketUtils.isParent(component, comp)) {
        it.remove();
      }
    }
  }
}
