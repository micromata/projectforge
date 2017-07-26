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
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.projectforge.web.wicket.bootstrap.GridBuilder;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class ToggleContainerPanel extends Panel
{
  private static final long serialVersionUID = 6130552547273354134L;

  public static final String CONTENT_ID = "content";

  public static final String HEADING_TEXT_ID = "text";

  private static final String ICON_STATUS_OPENED = "glyphicon glyphicon-minus";

  private static final String ICON_OPENED = "glyphicon glyphicon-minus glyphicon-white";

  private static final String ICON_CLOSED = "glyphicon glyphicon-plus glyphicon-white";

  protected final WebMarkupContainer panel, toggleContainer, toggleHeading, iconContainer;

  private Component heading;

  private String headingText;

  private boolean headingChanged;

  private ToggleStatus toggleStatus = ToggleStatus.OPENED;

  /**
   * @param id
   */
  @SuppressWarnings("serial")
  public ToggleContainerPanel(final String id, final DivType... cssClasses)
  {
    super(id);
    panel = new WebMarkupContainer("panel");
    panel.setOutputMarkupId(true);
    super.add(panel);
    if (cssClasses != null) {
      for (final DivType cssClass : cssClasses) {
        panel.add(AttributeModifier.append("class", cssClass.getClassAttrValue()));
      }
    }
    panel.add(toggleContainer = new WebMarkupContainer("toggleContainer"));
    toggleContainer.setOutputMarkupId(true);
    panel.add(toggleHeading = new WebMarkupContainer("heading"));
    toggleHeading.add(iconContainer = new WebMarkupContainer("icon"));
    iconContainer.setOutputMarkupId(true);
    setOpen();

    if (wantsOnStatusChangedNotification()) {
      final AjaxEventBehavior behavior = new AjaxEventBehavior("click")
      {
        @Override
        protected void onEvent(final AjaxRequestTarget target)
        {
          if (toggleStatus == ToggleStatus.OPENED) {
            target.appendJavaScript("$('#" + toggleContainer.getMarkupId() + "').collapse('hide')");
            toggleStatus = ToggleStatus.CLOSED;
          } else {
            target.appendJavaScript("$('#" + toggleContainer.getMarkupId() + "').collapse('show')");
            toggleStatus = ToggleStatus.OPENED;
          }
          headingChanged = false;
          ToggleContainerPanel.this.onToggleStatusChanged(target, toggleStatus);
          if (headingChanged == true) {
            target.add(heading);
          }
          target.add(iconContainer);
          setIcon();
        }
      };
      toggleHeading.add(behavior);
    } else {
      toggleHeading.add(AttributeModifier.replace("onclick", "$('#"
          + toggleContainer.getMarkupId()
          + "').collapse('toggle'); toggleCollapseIcon($('#"
          + iconContainer.getMarkupId()
          + "'), '"
          + ICON_STATUS_OPENED
          + "','"
          + ICON_OPENED
          + "','"
          + ICON_CLOSED
          + "'); return false;"));
    }
  }

  /**
   * Appends class "highlight" to the heading class: "collapse-header highlight"
   *
   * @return
   */
  public ToggleContainerPanel setHighlightedHeader()
  {
    toggleHeading.add(AttributeModifier.append("class", "highlight"));
    return this;
  }

  private void setIcon()
  {
    if (toggleStatus == ToggleStatus.OPENED) {
      iconContainer.add(AttributeModifier.replace("class", ICON_OPENED));
    } else {
      iconContainer.add(AttributeModifier.replace("class", ICON_CLOSED));
    }
  }

  @SuppressWarnings("serial")
  public ToggleContainerPanel setHeading(final String heading)
  {
    if (this.heading == null) {
      toggleHeading.add(this.heading = new Label(HEADING_TEXT_ID, new Model<String>()
      {
        /**
         * @see org.apache.wicket.model.Model#getObject()
         */
        @Override
        public String getObject()
        {
          return ToggleContainerPanel.this.headingText;
        }
      }).setOutputMarkupId(true));
    }
    if (heading.equals(this.headingText) == false) {
      headingChanged = true;
    }
    this.headingText = heading;
    return this;
  }

  /**
   * @param heading Must have the component id {@link #HEADING_TEXT_ID}.
   * @return
   */
  public ToggleContainerPanel setHeading(final Component heading)
  {
    if (this.heading != null) {
      throw new IllegalArgumentException("Can't set heading component twice!");
    }
    this.heading = heading;
    toggleHeading.add(heading);
    return this;
  }

  @Override
  public ToggleContainerPanel setMarkupId(final String id)
  {
    toggleContainer.setMarkupId(id);
    return this;
  }

  public WebMarkupContainer getContainer()
  {
    return toggleContainer;
  }

  /**
   * Returns whether the subclass wants to be notified on toggle status change
   *
   * @return
   */
  protected boolean wantsOnStatusChangedNotification()
  {
    return false;
  }

  /**
   * Hook method when the toggle status of this {@link ToggleContainerPanel} was changed.
   *
   * @param target
   * @param toggleClosed this represents the <b>new</b> state of the toggle.
   */
  protected void onToggleStatusChanged(final AjaxRequestTarget target, final ToggleStatus toggleStatus)
  {
    this.toggleStatus = toggleStatus;
  }

  /**
   * @see org.apache.wicket.MarkupContainer#add(org.apache.wicket.Component[])
   */
  public MarkupContainer add(final DivPanel content)
  {
    return toggleContainer.add(content);
  }

  /**
   * Calls div.add(...);
   *
   * @see org.apache.wicket.Component#add(org.apache.wicket.behavior.Behavior[])
   */
  @Override
  public Component add(final Behavior... behaviors)
  {
    return toggleContainer.add(behaviors);
  }

  /**
   * Has only effect before rendering this component the first time. Must be called after heading was set.
   *
   * @return this for chaining.
   */
  public ToggleContainerPanel setOpen()
  {
    toggleStatus = ToggleStatus.OPENED;
    return this;
  }

  /**
   * Has only effect before rendering this component the first time. Must be called after heading was set.
   *
   * @return this for chaining.
   */
  public ToggleContainerPanel setClosed()
  {
    toggleStatus = ToggleStatus.CLOSED;
    return this;
  }

  /**
   * @see org.apache.wicket.Component#onBeforeRender()
   */
  @Override
  protected void onBeforeRender()
  {
    super.onBeforeRender();
    if (toggleStatus == ToggleStatus.CLOSED) {
      toggleContainer.add(AttributeModifier.replace("class", "collapse"));
    } else {
      toggleContainer.add(AttributeModifier.replace("class", "in collapse"));
    }
    setIcon();
  }

  /**
   * @return the toggleStatus
   */
  public ToggleStatus getToggleStatus()
  {
    return toggleStatus;
  }

  public GridBuilder createGridBuilder()
  {
    final DivPanel content = new DivPanel(ToggleContainerPanel.CONTENT_ID);
    this.add(content);
    final GridBuilder gridBuilder = new GridBuilder(content, content.newChildId());
    return gridBuilder;
  }

  public enum ToggleStatus
  {
    OPENED, CLOSED
  }
}
