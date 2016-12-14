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

package org.projectforge.web.mobile;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.IFormSubmitter;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.PropertyModel;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.web.wicket.CsrfTokenHandler;

public abstract class AbstractMobileListForm<F extends BaseSearchFilter, P extends AbstractMobileListPage<?, ?, ?>> extends
    AbstractMobileForm<AbstractMobileListForm<?, ?>, AbstractMobileListPage<?, ?, ?>>
{
  private static final long serialVersionUID = -2521426347126048630L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AbstractMobileListForm.class);

  protected F filter;

  /**
   * Cross site request forgery token.
   */
  private final CsrfTokenHandler csrfTokenHandler;

  @SuppressWarnings("unchecked")
  public AbstractMobileListForm(final AbstractMobileListPage<?, ?, ?> parentPage)
  {
    super(parentPage);
    final String userPrefFilterKey = this.getClass().getSimpleName() + ".filter";
    try {
      filter = (F) parentPage.getUserPrefEntry(userPrefFilterKey);
    } catch (final ClassCastException ex) {
      log.info("Could not restore filter from user prefs (OK, probably new software release): " + userPrefFilterKey);
    }
    if (filter == null) {
      filter = newFilter();
      parentPage.putUserPrefEntry(userPrefFilterKey, filter, true);
    }
    csrfTokenHandler = new CsrfTokenHandler(this);
  }

  /**
   * Check the CSRF token right before the onSubmit methods are called, otherwise it may be too late.
   */
  @Override
  protected void delegateSubmit(IFormSubmitter submittingComponent)
  {
    csrfTokenHandler.onSubmit();
    super.delegateSubmit(submittingComponent);
  }

  protected void init()
  {
    add(new TextField<String>("searchField", new PropertyModel<String>(filter, "searchString")).add(AttributeModifier.replace(
        "placeholder", getString("search"))));
  }

  protected abstract F newFilter();
}
