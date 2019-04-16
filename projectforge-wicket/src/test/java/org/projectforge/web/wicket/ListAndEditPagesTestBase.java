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
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.util.tester.FormTester;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Use-full super class for testing standard list pages (derived from AbstractListPage).
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public abstract class ListAndEditPagesTestBase extends WicketPageTestBase {
  protected final String KEY_EDITPAGE_BUTTON_CREATE = "create";

  protected final String KEY_EDITPAGE_BUTTON_MARK_AS_DELETED = "markAsDeleted";

  protected final String KEY_LISTPAGE_SEARCH_INPUT_FIELD = "searchFilter";

  protected final String PATH_CONTENT_MENU_REPEATER = "body:breadcrumb:menuBar:repeater";

  protected final String PATH_EDITPAGE_FORM = "body:form";

  protected final String PATH_LISTPAGE_FORM = "body:form";

  protected final String PATH_LISTPAGE_TABLE = "body:form:table";

  protected abstract Class<? extends AbstractListPage<?, ?, ?>> getListPageClass();

  protected abstract Class<? extends AbstractEditPage<?, ?, ?>> getEditPageClass();

  /**
   * Creates a new edit page and pre-fills all fields of the data model which are at least required to create a new
   * data-base entry.<br>
   * Example:<br>
   *
   * <pre>
   * AddressEditPage editPage = new AddressEditPage(new PageParameters());
   * final AddressDO data = editPage.getForm().getData();
   * data.setName(&quot;Reinhard&quot;).setFirstName(&quot;Kai&quot;))....;
   * return editPage.
   * </pre>
   *
   * @return
   */
  protected abstract AbstractEditPage<?, ?, ?> getEditPageWithPrefilledData();

  /**
   * Optional checks of the fields of the edit page after an object was inserted and clicked in the list page.<br>
   * Example:<br>
   *
   * <pre>
   * AddressEditPage editPage = (AddressEditPage) tester.getLastRenderedPage();
   * AddressDO address = editPage.getForm().getData();
   * assertEquals(&quot;Kai&quot;, address.getFirstName());
   * </pre>
   */
  protected void checkEditPage() {

  }

  /**
   * Override this method if any test object entries do exist in the list (default is 0).
   *
   * @return number of existing elements expected in the list view or null if not to-be checked.
   */
  protected Integer getNumberOfExistingListElements() {
    return 0;
  }

  /**
   * Starts list page with reseted filter.
   */
  protected void startListPage() {
    startListPage(null);
  }

  /**
   * Starts list page with reseted filter and given search string as filter string.
   */
  protected void startListPage(final String searchString) {
    tester.startPage(getListPageClass());
    tester.assertRenderedPage(getListPageClass());
    final FormTester form = tester.newFormTester(PATH_LISTPAGE_FORM);
    final Component comp = findComponentByLabel(form, KEY_LISTPAGE_SEARCH_INPUT_FIELD);
    form.setValue(comp, searchString != null ? searchString : "");
    form.submit();
  }

  public void baseTests() {
    loginTestAdmin();
    startListPage();
    if (getNumberOfExistingListElements() != null) {
      final DataTable<?, String> table = (DataTable<?, String>) tester
              .getComponentFromLastRenderedPage(PATH_LISTPAGE_TABLE);
      assertEquals(getNumberOfExistingListElements().intValue(), table.getRowCount());
    }
    // Now, add a new element:
    tester.clickLink(findComponentByAccessKey(tester, PATH_CONTENT_MENU_REPEATER, 'n'));
    tester.assertRenderedPage(getEditPageClass());

    // Need new page to initialize model:
    final AbstractEditPage<?, ?, ?> editPage = getEditPageWithPrefilledData();
    tester.startPage(editPage);
    FormTester form = tester.newFormTester(PATH_EDITPAGE_FORM);
    form.submit(findComponentByLabel(form, KEY_EDITPAGE_BUTTON_CREATE));
    final Integer id = (Integer) editPage.getData().getId();

    // Now check list page
    tester.assertRenderedPage(getListPageClass());
    if (getNumberOfExistingListElements() != null) {
      final DataTable<?, String> table = (DataTable<?, String>) tester
              .getComponentFromLastRenderedPage(PATH_LISTPAGE_TABLE);
      assertEquals(getNumberOfExistingListElements() + 1, table.getRowCount());
    }
    startListPage("id:" + id);
    // Now re-enter edit page and mark object as deleted
    final Component link = findComponentByLabel(tester, PATH_LISTPAGE_TABLE, "select");
    tester.clickLink(link); // Edit page
    tester.assertRenderedPage(getEditPageClass());
    checkEditPage();
    form = tester.newFormTester(PATH_EDITPAGE_FORM);
    form.submit(findComponentByLabel(form, KEY_EDITPAGE_BUTTON_MARK_AS_DELETED));

    // Now check list page again after object was deleted:
    tester.assertRenderedPage(getListPageClass());
    if (getNumberOfExistingListElements() != null) {
      final DataTable<?, String> table = (DataTable<?, String>) tester
              .getComponentFromLastRenderedPage(PATH_LISTPAGE_TABLE);
      assertEquals(getNumberOfExistingListElements().intValue(), table.getRowCount());
    }
  }
}
