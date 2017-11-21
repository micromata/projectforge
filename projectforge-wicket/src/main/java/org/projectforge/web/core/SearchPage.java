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

package org.projectforge.web.core;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.registry.WebRegistry;
import org.projectforge.web.registry.WebRegistryEntry;
import org.projectforge.web.wicket.AbstractStandardFormPage;

public class SearchPage extends AbstractStandardFormPage implements ISelectCallerPage
{
  private static final long serialVersionUID = -8416731462457080883L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(SearchPage.class);

  private final SearchForm form;

  private final RepeatingView areaRepeater;

  // Do not execute the search on the first call (due to performance issues):
  private boolean refreshed = true;

  public SearchPage(final PageParameters parameters)
  {
    this(parameters, null);
  }

  /**
   * @param parameters
   * @param searchString if given all areas will be searched.
   */
  public SearchPage(final PageParameters parameters, final String searchString)
  {
    super(parameters);
    form = new SearchForm(this, searchString);
    body.add(form);
    form.init();
    areaRepeater = new RepeatingView("areaRepeater");
    body.add(areaRepeater);
    if (StringUtils.isNotBlank(searchString) == true) {
      // User wants to search, so show results directly:
      refreshed = false;
    }
  }

  @Override
  public void cancelSelection(final String property)
  {
  }

  @Override
  public void select(final String property, final Object selectedValue)
  {
    if ("userId".equals(property) == true) {
      final PFUserDO user = getTenantRegistry().getUserGroupCache().getUser((Integer) selectedValue);
      form.filter.setModifiedByUser(user);
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }

  @Override
  public void unselect(final String property)
  {
    log.error("Property '" + property + "' not supported for unselection.");
  }

  @Override
  protected void onBeforeRender()
  {
    refresh();
    super.onBeforeRender();
  }

  @Override
  protected void onAfterRender()
  {
    refreshed = false;
    super.onAfterRender();
  }

  void refresh()
  {
    if (refreshed == true) {
      // Do nothing (called twice).
      return;
    }
    refreshed = true;
    areaRepeater.removeAll();
    if (form.filter.isEmpty() == true) {
      return;
    }
    if ("ALL".equals(form.filter.getArea()) == true) {
      for (final WebRegistryEntry registryEntry : WebRegistry.getInstance().getOrderedList()) {
        if (SearchForm.isSearchable(registryEntry.getRegistryEntry()) == true) {
          addArea(registryEntry);
        }
      }
    } else {
      final WebRegistryEntry registryEntry = WebRegistry.getInstance().getEntry(form.filter.getArea());
      if (registryEntry == null) {
        log.error("Can't search in area '" + form.filter.getArea()
            + "'. No such area registered in WebRegistry! No results.");
      } else {
        addArea(registryEntry);
      }
    }
  }

  private void addArea(final WebRegistryEntry webRegistryEntry)
  {
    @SuppressWarnings("serial")
    final Panel panel = new AjaxLazyLoadPanel(areaRepeater.newChildId())
    {
      @Override
      public final Component getLazyLoadComponent(final String id)
      {
        final SearchAreaPanel searchAreaPanel = new SearchAreaPanel(SearchPage.this, id, form.filter, webRegistryEntry);
        return searchAreaPanel;
      }
    };
    areaRepeater.add(panel);
  }

  @Override
  public void renderHead(final IHeaderResponse response)
  {
    super.renderHead(response);
    response.render(JavaScriptReferenceHeaderItem.forUrl("scripts/zoom.js"));
  }

  @Override
  protected String getTitle()
  {
    return getString("search.title");
  }
}
