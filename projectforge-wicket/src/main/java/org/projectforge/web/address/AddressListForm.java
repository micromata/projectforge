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

package org.projectforge.web.address;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.form.FormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.convert.IConverter;
import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.AddressDao;
import org.projectforge.business.address.AddressFilter;
import org.projectforge.common.StringHelper;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.autocompletion.PFAutoCompleteTextField;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DivPanelVisibility;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;
import org.projectforge.web.wicket.flowlayout.RadioGroupPanel;

public class AddressListForm extends AbstractListForm<AddressListFilter, AddressListPage>
{
  private static final long serialVersionUID = 8124796579658957116L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AddressListForm.class);

  @SpringBean
  private AddressDao addressDao;

  /**
   * Used by AddressCampaignValueListForm.
   */
  @SuppressWarnings("serial")
  public static void onOptionsPanelCreate(final AbstractListPage<?, ?, ?> parentPage,
      final FieldsetPanel optionsFieldsetPanel,
      final AddressFilter searchFilter)
  {
    {
      final DivPanel radioGroupPanel = optionsFieldsetPanel.addNewRadioBoxButtonDiv();
      final RadioGroupPanel<String> radioGroup = new RadioGroupPanel<String>(radioGroupPanel.newChildId(), "listtype",
          new PropertyModel<String>(searchFilter, "listType"), new FormComponentUpdatingBehavior()
      {
        @Override
        public void onUpdate()
        {
          parentPage.refresh();
        }
      });
      radioGroupPanel.add(radioGroup);
      radioGroup.add(new Model<String>(AddressFilter.FILTER_FILTER), parentPage.getString("filter"));
      radioGroup.add(new Model<String>(AddressFilter.FILTER_NEWEST), parentPage.getString("filter.newest"));
      radioGroup.add(new Model<String>(AddressFilter.FILTER_MY_FAVORITES),
          parentPage.getString("address.filter.myFavorites"));
      radioGroup.add(new Model<String>(AddressFilter.FILTER_DOUBLETS), parentPage.getString("address.filter.doublets"));
    }

  }

  /**
   * Used by AddressCampaignValueListForm.
   */
  @SuppressWarnings("serial")
  public static void addFilter(final AbstractListPage<?, ?, ?> parentPage, final AbstractListForm<?, ?> form,
      final GridBuilder gridBuilder, final AddressFilter searchFilter)
  {
    {
      gridBuilder.newSplitPanel(GridSize.COL50);
      gridBuilder.getRowPanel().setVisibility(new DivPanelVisibility()
      {

        @Override
        public boolean isVisible()
        {
          return searchFilter.isFilter();
        }
      });
      final FieldsetPanel fieldset = gridBuilder.newFieldset(parentPage.getString("address.contactStatus"))
          .suppressLabelForWarning();
      final DivPanel checkBoxPanel = fieldset.addNewCheckBoxButtonDiv();
      checkBoxPanel.add(form.createAutoRefreshCheckBoxButton(checkBoxPanel.newChildId(),
          new PropertyModel<Boolean>(searchFilter, "active"),
          parentPage.getString("address.contactStatus.active")));
      checkBoxPanel
          .add(form.createAutoRefreshCheckBoxButton(checkBoxPanel.newChildId(), new PropertyModel<Boolean>(searchFilter,
              "nonActive"), parentPage.getString("address.contactStatus.nonActive")));
      checkBoxPanel
          .add(form.createAutoRefreshCheckBoxButton(checkBoxPanel.newChildId(), new PropertyModel<Boolean>(searchFilter,
              "uninteresting"), parentPage.getString("address.contactStatus.uninteresting")));
      checkBoxPanel
          .add(form.createAutoRefreshCheckBoxButton(checkBoxPanel.newChildId(), new PropertyModel<Boolean>(searchFilter,
              "personaIngrata"), parentPage.getString("address.contactStatus.personaIngrata")));
      checkBoxPanel.add(form.createAutoRefreshCheckBoxButton(checkBoxPanel.newChildId(),
          new PropertyModel<Boolean>(searchFilter, "departed"),
          parentPage.getString("address.contactStatus.departed")));
    }
    {
      gridBuilder.newSplitPanel(GridSize.COL50);
      final FieldsetPanel fieldset = gridBuilder.newFieldset(parentPage.getString("address.addressStatus"))
          .suppressLabelForWarning();
      final DivPanel checkBoxPanel = fieldset.addNewCheckBoxButtonDiv();
      checkBoxPanel.add(form.createAutoRefreshCheckBoxButton(checkBoxPanel.newChildId(),
          new PropertyModel<Boolean>(searchFilter, "uptodate"),
          parentPage.getString("address.addressStatus.uptodate")));
      checkBoxPanel.add(form.createAutoRefreshCheckBoxButton(checkBoxPanel.newChildId(),
          new PropertyModel<Boolean>(searchFilter, "outdated"),
          parentPage.getString("address.addressStatus.outdated")));
      checkBoxPanel.add(form.createAutoRefreshCheckBoxButton(checkBoxPanel.newChildId(),
          new PropertyModel<Boolean>(searchFilter, "leaved"),
          parentPage.getString("address.addressStatus.leaved")));
    }

  }

  @Override
  protected void init()
  {
    super.init();
    addFilter(parentPage, this, gridBuilder, getSearchFilter());
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#onOptionsPanelCreate(org.projectforge.web.wicket.flowlayout.FieldsetPanel,
   * org.projectforge.web.wicket.flowlayout.DivPanel)
   */
  @Override
  protected void onOptionsPanelCreate(final FieldsetPanel optionsFieldsetPanel, final DivPanel optionsCheckBoxesPanel)
  {
    onOptionsPanelCreate(parentPage, optionsFieldsetPanel, searchFilter);
  }

  public AddressListForm(final AddressListPage parentPage)
  {
    super(parentPage);
  }

  @SuppressWarnings("serial")
  @Override
  protected TextField<?> createSearchTextField()
  {
    @SuppressWarnings({ "unchecked", "rawtypes" })
    final PFAutoCompleteTextField<AddressDO> searchField = new PFAutoCompleteTextField<AddressDO>(InputPanel.WICKET_ID,
        new Model()
        {
          @Override
          public Serializable getObject()
          {
            // Pseudo object for storing search string (title field is used for this foreign purpose).
            return new AddressDO().setComment(searchFilter.getSearchString());
          }

          @Override
          public void setObject(final Serializable object)
          {
            if (object != null) {
              if (object instanceof String) {
                searchFilter.setSearchString((String) object);
              }
            } else {
              searchFilter.setSearchString("");
            }
          }
        })
    {
      @Override
      protected List<AddressDO> getChoices(final String input)
      {
        final AddressFilter filter = new AddressFilter();
        filter.setSearchString(input);
        filter.setSearchFields("name", "firstName", "organization");
        final List<AddressDO> list = addressDao.getList(filter);
        return list;
      }

      @Override
      protected List<String> getRecentUserInputs()
      {
        return parentPage.getRecentSearchTermsQueue().getRecents();
      }

      @Override
      protected String formatLabel(final AddressDO address)
      {
        return StringHelper.listToString("; ", address.getName(), address.getFirstName(), address.getOrganization());
      }

      @Override
      protected String formatValue(final AddressDO address)
      {
        return "id:" + address.getId();
      }

      /**
       * @see org.apache.wicket.Component#getConverter(java.lang.Class)
       */
      @Override
      public <C> IConverter<C> getConverter(final Class<C> type)
      {
        return new IConverter<C>()
        {
          @Override
          public C convertToObject(final String value, final Locale locale)
          {
            searchFilter.setSearchString(value);
            return null;
          }

          @Override
          public String convertToString(final Object value, final Locale locale)
          {
            return searchFilter.getSearchString();
          }
        };
      }
    };
    searchField.withLabelValue(true).withMatchContains(true).withMinChars(2).withFocus(true).withAutoSubmit(true);
    createSearchFieldTooltip(searchField);
    return searchField;
  }

  @Override
  protected AddressListFilter newSearchFilterInstance()
  {
    return new AddressListFilter();
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
