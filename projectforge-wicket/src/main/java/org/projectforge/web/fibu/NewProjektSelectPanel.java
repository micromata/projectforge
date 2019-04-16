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

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.convert.IConverter;
import org.projectforge.business.fibu.KundeDO;
import org.projectforge.business.fibu.KundeDao;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.ProjektDao;
import org.projectforge.business.fibu.ProjektFavorite;
import org.projectforge.business.fibu.ProjektFormatter;
import org.projectforge.business.user.service.UserPreferencesService;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.user.api.UserPrefArea;
import org.projectforge.framework.utils.RecentQueue;
import org.projectforge.web.wicket.AbstractSelectPanel;
import org.projectforge.web.wicket.WebConstants;
import org.projectforge.web.wicket.autocompletion.PFAutoCompleteTextField;
import org.projectforge.web.wicket.components.FavoritesChoicePanel;
import org.projectforge.web.wicket.components.TooltipImage;
import org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel;

/**
 * This panel shows the actual customer.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class NewProjektSelectPanel extends AbstractSelectPanel<ProjektDO> implements ComponentWrapperPanel
{
  private static final long serialVersionUID = -7461448790487855518L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NewProjektSelectPanel.class);

  private static final String USER_PREF_KEY_RECENT_PROJECTS = "ProjectSelectPanel:recentProjects";

  @SuppressWarnings("unused")
  private boolean defaultFormProcessing = false;

  @SpringBean
  private ProjektFormatter projektFormatter;

  @SpringBean
  private ProjektDao projektDao;

  @SpringBean
  private KundeDao kundeDao;

  @SpringBean
  private UserPreferencesService userPreferencesService;

  private RecentQueue<String> recentProjects;

  private final PFAutoCompleteTextField<ProjektDO> projectTextField;

  // Only used for detecting changes:
  private ProjektDO currentProject;

  /**
   * @param id
   * @param model
   * @param caller
   * @param selectProperty
   */
  public NewProjektSelectPanel(final String id, final IModel<ProjektDO> model, final ISelectCallerPage caller,
      final String selectProperty)
  {
    this(id, model, null, caller, selectProperty);
  }

  /**
   * @param id
   * @param model
   * @param caller
   * @param selectProperty
   */
  @SuppressWarnings("serial")
  public NewProjektSelectPanel(final String id, final IModel<ProjektDO> model, final String label,
      final ISelectCallerPage iCaller, final String selectProperty)
  {
    super(id, model, iCaller, selectProperty);
    projectTextField = new PFAutoCompleteTextField<ProjektDO>("projectField", getModel())
    {
      @Override
      protected List<ProjektDO> getChoices(final String input)
      {
        final BaseSearchFilter filter = new BaseSearchFilter();
        filter.setSearchFields("id", "name", "identifier", "nummer");
        filter.setSearchString(input);
        final List<ProjektDO> list = projektDao.getList(filter);
        return list;
      }

      @Override
      protected List<String> getRecentUserInputs()
      {
        return getRecentProjects().getRecents();
      }

      @Override
      protected String formatLabel(final ProjektDO project)
      {
        if (project == null) {
          return "";
        }
        return projektFormatter.format(project, false);
      }

      @Override
      protected String formatValue(final ProjektDO project)
      {
        if (project == null) {
          return "";
        }
        return projektFormatter.format(project, false);
      }

      @Override
      public void convertInput()
      {
        final ProjektDO project = getConverter(getType()).convertToObject(getInput(), getLocale());
        setConvertedInput(project);
        if (project != null && (currentProject == null || project.getId() != currentProject.getId())) {
          getRecentProjects().append(projektFormatter.format(project, false));
        }
        currentProject = project;
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

            final ProjektDO project = getProjekt(value);
            if (project == null) {
              error(getString("panel.error.projectNotFound"));
            }
            getModel().setObject(project);

            return project;
          }

          @Override
          public String convertToString(final Object value, final Locale locale)
          {
            if (value == null) {
              return "";
            }
            final ProjektDO project = (ProjektDO) value;
            return formatLabel(project);
          }

        };
      }
    };
    currentProject = getModelObject();
    projectTextField.enableTooltips().withLabelValue(true).withMatchContains(true).withMinChars(2)
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

  @Override
  @SuppressWarnings("serial")
  public NewProjektSelectPanel init()
  {
    super.init();
    add(projectTextField);
    final SubmitLink selectButton = new SubmitLink("select")
    {
      @Override
      public void onSubmit()
      {
        setResponsePage(new ProjektListPage(caller, selectProperty));
      }
    };

    selectButton.setDefaultFormProcessing(false);
    add(selectButton);
    final boolean hasSelectAccess = projektDao.hasLoggedInUserSelectAccess(false);
    if (hasSelectAccess == false) {
      selectButton.setVisible(false);
    }
    selectButton.add(
        new TooltipImage("selectHelp", WebConstants.IMAGE_PROJEKT_SELECT, getString("fibu.tooltip.selectProjekt")));
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
        return hasSelectAccess == true && isRequired() == false && NewProjektSelectPanel.this.getModelObject() != null;
      }
    };

    unselectButton.setDefaultFormProcessing(false);
    add(unselectButton);
    unselectButton.add(new TooltipImage("unselectHelp", WebConstants.IMAGE_PROJEKT_UNSELECT,
        getString("fibu.tooltip.unselectProjekt")));
    // DropDownChoice favorites
    final FavoritesChoicePanel<ProjektDO, ProjektFavorite> favoritesPanel = new FavoritesChoicePanel<ProjektDO, ProjektFavorite>(
        "favorites", UserPrefArea.PROJEKT_FAVORITE, tabIndex, "select half")
    {
      @Override
      protected void select(final ProjektFavorite favorite)
      {
        if (favorite.getProjekt() != null) {
          NewProjektSelectPanel.this.selectProjekt(favorite.getProjekt());
        }
      }

      @Override
      protected ProjektDO getCurrentObject()
      {
        return NewProjektSelectPanel.this.getModelObject();
      }

      @Override
      protected ProjektFavorite newFavoriteInstance(final ProjektDO currentObject)
      {
        final ProjektFavorite favorite = new ProjektFavorite();
        favorite.setProjekt(currentObject);
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

  /**
   * Will be called if the user has chosen an entry of the projekt favorites drop down choice.
   *
   * @param projekt
   */
  protected void selectProjekt(final ProjektDO projekt)
  {
    setModelObject(projekt);
    caller.select(selectProperty, projekt.getId());
  }

  public NewProjektSelectPanel withAutoSubmit(final boolean autoSubmit)
  {
    projectTextField.withAutoSubmit(autoSubmit);
    return this;
  }

  @Override
  public Component getWrappedComponent()
  {
    return projectTextField;
  }

  @Override
  public void convertInput()
  {
    setConvertedInput(getModelObject());
  }

  @SuppressWarnings("unchecked")
  private RecentQueue<String> getRecentProjects()
  {
    if (this.recentProjects == null) {
      this.recentProjects = (RecentQueue<String>) userPreferencesService.getEntry(USER_PREF_KEY_RECENT_PROJECTS);
    }
    if (this.recentProjects == null) {
      this.recentProjects = new RecentQueue<String>();
      userPreferencesService.putEntry(USER_PREF_KEY_RECENT_PROJECTS, this.recentProjects, true);
    }
    return this.recentProjects;
  }

  @SuppressWarnings("unused")
  private String formatCustomer(final ProjektDO customer)
  {
    if (customer == null) {
      return "";
    }
    return projektFormatter.format(customer, false);
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel#getComponentOutputId()
   */
  @Override
  public String getComponentOutputId()
  {
    projectTextField.setOutputMarkupId(true);
    return projectTextField.getMarkupId();
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel#getFormComponent()
   */
  @Override
  public FormComponent<?> getFormComponent()
  {
    return projectTextField;
  }

  private ProjektDO getProjekt(final String input)
  {
    try {
      int kundeId, kost2;
      String nummernkreis, kId, nummer;
      kundeId = kost2 = -1;
      nummernkreis = kId = nummer = "";
      final int ind1 = input.indexOf(".");
      if (ind1 < 0) {
        return null;
      }
      nummernkreis = input.substring(0, ind1);
      final int ind2 = input.indexOf(".", ind1 + 1);
      if (ind2 < 0) {
        return null;
      }
      kId = input.substring(ind1 + 1, ind2);
      final int ind3 = input.indexOf(" -", ind2 + 1);
      if (ind3 < 0) {
        return null;
      }
      nummer = input.substring(ind2 + 1, ind3);
      kundeId = Integer.parseInt(kId);
      kost2 = Integer.parseInt(nummer);
      if (kundeId < 0 || kost2 < 0) {
        return null;
      }
      if (nummernkreis.equals("4") == true) {
        return projektDao.getProjekt(kundeId, kost2);
      } else if (nummernkreis.equals("5") == true) {
        final KundeDO kunde = kundeDao.getById(kundeId);
        if (kunde == null) {
          return null;
        }
        return projektDao.getProjekt(kunde, kost2);
      }
    } catch (Exception e) {
      log.error("An exception accured while parsing customer id and kost2.", e);
    }
    return null;
  }

  /**
   * @return the projectTextField
   */
  public PFAutoCompleteTextField<ProjektDO> getTextField()
  {
    return projectTextField;
  }

}
