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

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.ProjektDao;
import org.projectforge.business.fibu.ProjektFavorite;
import org.projectforge.business.fibu.ProjektFormatter;
import org.projectforge.business.utils.HtmlHelper;
import org.projectforge.framework.persistence.user.api.UserPrefArea;
import org.projectforge.web.wicket.AbstractSelectPanel;
import org.projectforge.web.wicket.WebConstants;
import org.projectforge.web.wicket.components.FavoritesChoicePanel;
import org.projectforge.web.wicket.components.TooltipImage;
import org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel;

/**
 * This panel show the actual project and buttons for select/unselect projects.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class ProjektSelectPanel extends AbstractSelectPanel<ProjektDO> implements ComponentWrapperPanel
{
  private static final long serialVersionUID = 5452693296383142460L;

  @SpringBean
  private ProjektFormatter projektFormatter;

  @SpringBean
  private ProjektDao projektDao;

  private Label projektAsStringLabel;

  /**
   * @param id
   * @param label          Not yet in use.
   * @param model
   * @param caller
   * @param selectProperty
   */
  @SuppressWarnings("serial")
  public ProjektSelectPanel(final String id, final IModel<ProjektDO> model, final ISelectCallerPage caller, final String selectProperty)
  {
    super(id, model, caller, selectProperty);
    projektAsStringLabel = new Label("projectAsString", new Model<String>()
    {

      @Override
      public String getObject()
      {
        final ProjektDO projekt = ProjektSelectPanel.this.getModelObject();
        final String str = projektFormatter.format(projekt, false);
        if (str == null) {
          return projektFormatter.getNotVisibleString();
        }
        return HtmlHelper.escapeXml(str);
      }
    });
    projektAsStringLabel.setEscapeModelStrings(false);
    add(projektAsStringLabel);
  }

  @Override
  @SuppressWarnings("serial")
  public ProjektSelectPanel init()
  {
    super.init();
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
    selectButton.add(new TooltipImage("selectHelp", WebConstants.IMAGE_PROJEKT_SELECT, getString("fibu.tooltip.selectProjekt")));
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
        return hasSelectAccess == true && isRequired() == false && ProjektSelectPanel.this.getModelObject() != null;
      }
    };
    unselectButton.setDefaultFormProcessing(false);
    add(unselectButton);
    unselectButton.add(new TooltipImage("unselectHelp", WebConstants.IMAGE_PROJEKT_UNSELECT, getString("fibu.tooltip.unselectProjekt")));
    // DropDownChoice favorites
    final FavoritesChoicePanel<ProjektDO, ProjektFavorite> favoritesPanel = new FavoritesChoicePanel<ProjektDO, ProjektFavorite>(
        "favorites", UserPrefArea.PROJEKT_FAVORITE, tabIndex, "select half")
    {
      @Override
      protected void select(final ProjektFavorite favorite)
      {
        if (favorite.getProjekt() != null) {
          ProjektSelectPanel.this.selectProjekt(favorite.getProjekt());
        }
      }

      @Override
      protected ProjektDO getCurrentObject()
      {
        return ProjektSelectPanel.this.getModelObject();
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

  @Override
  public void convertInput()
  {
    setConvertedInput(getModelObject());
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel#getComponentOutputId()
   */
  @Override
  public String getComponentOutputId()
  {
    projektAsStringLabel.setOutputMarkupId(true);
    return projektAsStringLabel.getMarkupId();
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel#getFormComponent()
   */
  @Override
  public FormComponent<?> getFormComponent()
  {
    return null;
  }
}
