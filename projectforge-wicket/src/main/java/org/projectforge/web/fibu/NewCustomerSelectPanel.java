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

package org.projectforge.web.fibu;

import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.convert.IConverter;
import org.projectforge.business.fibu.KundeDO;
import org.projectforge.business.fibu.KundeDao;
import org.projectforge.business.fibu.KundeFavorite;
import org.projectforge.business.fibu.KundeFormatter;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.user.api.UserPrefArea;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.framework.utils.RecentQueue;
import org.projectforge.web.user.UserPreferencesHelper;
import org.projectforge.web.wicket.AbstractForm;
import org.projectforge.web.wicket.AbstractSelectPanel;
import org.projectforge.web.wicket.WebConstants;
import org.projectforge.web.wicket.autocompletion.PFAutoCompleteTextField;
import org.projectforge.web.wicket.components.FavoritesChoicePanel;
import org.projectforge.web.wicket.components.MaxLengthTextField;
import org.projectforge.web.wicket.components.TooltipImage;
import org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel;

/**
 * This panel shows the actual customer.
 * 
 * @author Werner Feder (werner.feder@t-online.de)
 * 
 */
public class NewCustomerSelectPanel extends AbstractSelectPanel<KundeDO> implements ComponentWrapperPanel
{
  private static final long serialVersionUID = -7114401036341110814L;

  private static final String USER_PREF_KEY_RECENT_CUSTOMERS = "CustomerSelectPanel:recentCustomers";

  @SuppressWarnings("unused")
  private boolean defaultFormProcessing = false;

  @SpringBean
  private KundeFormatter kundeFormatter;

  @SpringBean
  private KundeDao kundeDao;

  private RecentQueue<String> recentCustomers;

  private final PFAutoCompleteTextField<KundeDO> customerTextField;

  // Only used for detecting changes:
  private KundeDO currentCustomer;

  private final PropertyModel<String> kundeText;

  private TextField<String> kundeTextField;

  private FavoritesChoicePanel<KundeDO, KundeFavorite> favoritesPanel;

  /**
   * @param id
   * @param model
   * @param caller
   * @param selectProperty
   */
  @SuppressWarnings("serial")
  public NewCustomerSelectPanel(final String id, final IModel<KundeDO> model, final PropertyModel<String> kundeText,
      final ISelectCallerPage caller, final String selectProperty)
  {
    super(id, model, caller, selectProperty);
    this.kundeText = kundeText;
    customerTextField = new PFAutoCompleteTextField<KundeDO>("customerField", getModel())
    {
      @Override
      protected List<KundeDO> getChoices(final String input)
      {
        final BaseSearchFilter filter = new BaseSearchFilter();
        filter.setSearchFields("id", "name", "identifier", "division");
        filter.setSearchString(input);
        final List<KundeDO> list = kundeDao.getList(filter);
        return list;
      }

      @Override
      protected List<String> getRecentUserInputs()
      {
        return getRecentCustomers().getRecents();
      }

      @Override
      protected String formatLabel(final KundeDO customer)
      {
        if (customer == null) {
          return "";
        }
        return kundeFormatter.format(customer, false);
      }

      @Override
      protected String formatValue(final KundeDO customer)
      {
        if (customer == null) {
          return "";
        }
        return kundeFormatter.format(customer, false);
      }

      @Override
      protected void convertInput()
      {
        final KundeDO customer = getConverter(getType()).convertToObject(getInput(), getLocale());
        setConvertedInput(customer);
        if (customer != null && (currentCustomer == null || customer.getId() != currentCustomer.getId())) {
          getRecentCustomers().append(kundeFormatter.format(customer, false));
        }
        currentCustomer = customer;
      }

      /**
       * @see org.apache.wicket.Component#getConverter(java.lang.Class)
       */

      @SuppressWarnings({ "unchecked", "rawtypes" })
      @Override
      public <C> IConverter<C> getConverter(final Class<C> type)
      {
        return new IConverter()
        {
          @Override
          public Object convertToObject(final String value, final Locale locale)
          {
            if (StringUtils.isEmpty(value) == true) {
              getModel().setObject(null);
              return null;
            }
            final int ind = value.indexOf(" - ");
            final String idString = ind >= 0 ? value.substring(0, ind) : value;
            final Integer id = NumberHelper.parseInteger(idString);
            final KundeDO kunde = id != null ? kundeDao.getById(id) : null;
            if (kunde == null) {
              error(getString("panel.error.customernameNotFound"));
            }
            getModel().setObject(kunde);
            return kunde;
          }

          @Override
          public String convertToString(final Object value, final Locale locale)
          {
            if (value == null) {
              return "";
            }
            final KundeDO kunde = (KundeDO) value;
            return formatLabel(kunde);
          }

        };
      }
    };
    currentCustomer = getModelObject();
    customerTextField.enableTooltips().withLabelValue(true).withMatchContains(true).withMinChars(2)
        .withAutoSubmit(false); // .withWidth(400);
  }

  /**
   * Should be called before init() method. If true, then the validation will be done after submitting.
   * 
   * @param defaultFormProcessing
   */
  public void setDefaultFormProcessing(final boolean defaultFormProcessing)
  {
    this.defaultFormProcessing = defaultFormProcessing;
  }

  @SuppressWarnings("serial")
  @Override
  public NewCustomerSelectPanel init()
  {
    super.init();
    if (kundeText != null) {
      kundeTextField = new MaxLengthTextField("kundeText", kundeText)
      {
        @Override
        public boolean isVisible()
        {
          return (NewCustomerSelectPanel.this.getModelObject() == null
              || NumberHelper.greaterZero(NewCustomerSelectPanel.this
                  .getModelObject().getId()) == false);
        }
      };
      add(kundeTextField);
    } else {
      add(AbstractForm.createInvisibleDummyComponent("kundeText"));
    }
    add(customerTextField);
    final SubmitLink selectButton = new SubmitLink("select")
    {
      @Override
      public void onSubmit()
      {
        setResponsePage(new CustomerListPage(caller, selectProperty));
      };
    };
    selectButton.setDefaultFormProcessing(false);
    add(selectButton);
    selectButton
        .add(new TooltipImage("selectHelp", WebConstants.IMAGE_KUNDE_SELECT, getString("fibu.tooltip.selectKunde")));
    final SubmitLink unselectButton = new SubmitLink("unselect")
    {
      @Override
      public void onSubmit()
      {
        caller.unselect(selectProperty);
      }

      @Override
      public boolean isVisible()
      {
        return NewCustomerSelectPanel.this.getModelObject() != null;
      }
    };
    unselectButton.setDefaultFormProcessing(false);
    add(unselectButton);
    unselectButton.add(
        new TooltipImage("unselectHelp", WebConstants.IMAGE_KUNDE_UNSELECT, getString("fibu.tooltip.unselectKunde")));
    // DropDownChoice favorites
    favoritesPanel = new FavoritesChoicePanel<KundeDO, KundeFavorite>("favorites", UserPrefArea.KUNDE_FAVORITE,
        tabIndex, "half select")
    {
      @Override
      protected void select(final KundeFavorite favorite)
      {
        if (favorite.getKunde() != null) {
          NewCustomerSelectPanel.this.selectKunde(favorite.getKunde());
        }
      }

      @Override
      protected KundeDO getCurrentObject()
      {
        return NewCustomerSelectPanel.this.getModelObject();
      }

      @Override
      protected KundeFavorite newFavoriteInstance(final KundeDO currentObject)
      {
        final KundeFavorite favorite = new KundeFavorite();
        favorite.setKunde(currentObject);
        return favorite;
      }
    };
    add(favoritesPanel);
    favoritesPanel.init();
    if (showFavorites == false) {
      favoritesPanel.setVisible(false);
    }
    return this;
  }

  public NewCustomerSelectPanel withAutoSubmit(final boolean autoSubmit)
  {
    customerTextField.withAutoSubmit(autoSubmit);
    return this;
  }

  @Override
  public Component getWrappedComponent()
  {
    return customerTextField;
  }

  @Override
  protected void convertInput()
  {
    setConvertedInput(getModelObject());
  }

  @SuppressWarnings("unchecked")
  private RecentQueue<String> getRecentCustomers()
  {
    if (this.recentCustomers == null) {
      this.recentCustomers = (RecentQueue<String>) UserPreferencesHelper.getEntry(USER_PREF_KEY_RECENT_CUSTOMERS);
    }
    if (this.recentCustomers == null) {
      this.recentCustomers = new RecentQueue<String>();
      UserPreferencesHelper.putEntry(USER_PREF_KEY_RECENT_CUSTOMERS, this.recentCustomers, true);
    }
    return this.recentCustomers;
  }

  @SuppressWarnings("unused")
  private String formatCustomer(final KundeDO customer)
  {
    if (customer == null) {
      return "";
    }
    return kundeFormatter.format(customer, false);
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel#getComponentOutputId()
   */
  @Override
  public String getComponentOutputId()
  {
    customerTextField.setOutputMarkupId(true);
    return customerTextField.getMarkupId();
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel#getFormComponent()
   */
  @Override
  public FormComponent<?> getFormComponent()
  {
    return customerTextField;
  }

  /**
   * @return The user's raw input of kunde text if given, otherwise null.
   */
  public String getKundeTextInput()
  {
    if (kundeTextField != null) {
      return kundeTextField.getRawInput();
    }
    return null;
  }

  /**
   * Will be called if the user has chosen an entry of the kunde favorites drop down choice.
   * 
   * @param kunde
   */
  protected void selectKunde(final KundeDO kunde)
  {
    setModelObject(kunde);
    caller.select(selectProperty, kunde.getId());
  }

  /**
   * @return the kundeTextField
   */
  public TextField<String> getKundeTextField()
  {
    return kundeTextField;
  }

  /**
   * @return the projectTextField
   */
  public PFAutoCompleteTextField<KundeDO> getTextField()
  {
    return customerTextField;
  }
}
