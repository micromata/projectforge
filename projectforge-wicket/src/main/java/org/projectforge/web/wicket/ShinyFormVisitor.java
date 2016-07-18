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

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

/**
 * Visitor for highlighting form components with validation errors. A border around the error fields with error highlighting and validation
 * message will be shown.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class ShinyFormVisitor implements IVisitor<Component, Void>, Serializable
{
  private static final long serialVersionUID = -4544543580002871884L;

  Set<Component> visited = new HashSet<Component>();

  /**
   * Clear hash set with all visited components.
   */
  public void reset()
  {
    visited = new HashSet<Component>();
  }

  @Override
  public void component(final Component component, final IVisit<Void> visit)
  {
    if (component instanceof FieldsetPanel == true) {
      final FieldsetPanel fsPanel = (FieldsetPanel) component;
      if (fsPanel.isValid() == false) {
        if (visited.contains(component) == false) {
          visited.add(component);
          //fsPanel.getFieldset().add(new ValidationMsgBehavior());
          fsPanel.getFieldset().add(new ErrorHighlightBehavior());
        }
      }
      visit.dontGoDeeper();
      return;
    }
    if (component instanceof FormComponent< ? > == false) {
      return;
    }
    final FormComponent< ? > fc = (FormComponent< ? >) component;
    if (fc.isValid() == false && hasInvalidParent(fc.getParent()) == false) {
      if (visited.contains(component) == false) {
        visited.add(component);
        //component.add(new ValidationMsgBehavior());
        component.add(new ErrorHighlightBehavior());
      }
    }
    // if (fc.isValid() == false && hasInvalidParent(fc.getParent()) == false) {
    // component.setComponentBorder(new ValidationErrorBorder());
    // } else if (component.getComponentBorder() != null) {
    // // Clear component border.
    // component.setComponentBorder(null);
    // }
    visit.dontGoDeeper();
    return;
  }

  /** Avoid borders around components and child components. */
  private boolean hasInvalidParent(final Component component)
  {
    if (component == null) {
      return false;
    }
    if (component instanceof FieldsetPanel && ((FieldsetPanel) component).isValid() == false) {
      return true;
    }
    if (component instanceof FormComponent< ? > && ((FormComponent< ? >) component).isValid() == false) {
      return true;
    }
    return hasInvalidParent(component.getParent());
  }
}
