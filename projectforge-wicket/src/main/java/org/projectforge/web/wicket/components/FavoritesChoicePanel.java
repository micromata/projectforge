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

package org.projectforge.web.wicket.components;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.user.UserPrefDao;
import org.projectforge.framework.persistence.user.api.UserPrefArea;
import org.projectforge.framework.persistence.user.entities.UserPrefDO;
import org.projectforge.web.user.UserPrefEditPage;
import org.projectforge.web.wicket.AbstractSecuredPage;

/**
 * Combo box for showing and selecting favorites quickly.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public abstract class FavoritesChoicePanel<T, F> extends FormComponentPanel<String>
{
  private static final long serialVersionUID = 4605128072052146129L;

  protected static final String ADD_NEW_ENTRY = "ADD_NEW_ENTRY";

  @SpringBean
  private UserPrefDao userPrefDao;

  protected String selected;

  private boolean refresh = false;

  private DropDownChoice<String> choice;

  private Integer tabIndex;

  private String cssClass;

  private UserPrefArea userPrefArea;

  private boolean clearSelectionAfterSelection = true;

  private String nullKey;

  @SuppressWarnings("unused")
  private String dummy;

  public FavoritesChoicePanel(final String componentId, final UserPrefArea userPrefArea)
  {
    this(componentId, userPrefArea, null, null);
  }

  public FavoritesChoicePanel(final String componentId, final UserPrefArea userPrefArea, final Integer tabIndex, final String cssClass)
  {
    super(componentId);
    this.userPrefArea = userPrefArea;
    this.tabIndex = tabIndex;
    this.cssClass = cssClass;
    setModel(new PropertyModel<String>(this, "dummy"));
  }

  @SuppressWarnings("serial")
  public DropDownChoice<String> init()
  {
    final LabelValueChoiceRenderer<String> renderer = createRenderer();
    choice = new DropDownChoice<String>("dropDownChoice", new PropertyModel<String>(this, "selected"), renderer.getValues(), renderer) {
      @Override
      protected boolean wantOnSelectionChangedNotifications()
      {
        return true;
      }

      @Override
      protected String getNullKey()
      {
        if (nullKey != null) {
          return nullKey;
        }
        return super.getNullKey();
      }

      @Override
      protected void onSelectionChanged(final String newSelection)
      {
        if (StringUtils.isNotEmpty(newSelection) == true) {
          if (ADD_NEW_ENTRY.equals(newSelection) == true) {
            final Object favorite = newFavoriteInstance(getCurrentObject());
            final UserPrefEditPage page = new UserPrefEditPage(userPrefArea, favorite);
            page.setReturnToPage((AbstractSecuredPage) this.getPage());
            refresh = true;
            setResponsePage(page);
            selected = "";
            return;
          }
          // Fill fields with selected template values:
          final F favorite = getCurrentFavorite();
          if (favorite != null) {
            select(favorite);
          }
          if (FavoritesChoicePanel.this.clearSelectionAfterSelection == true) {
            selected = "";
          }
        }
      }
    };
    choice.setNullValid(true);
    if (tabIndex != null) {
      choice.add(AttributeModifier.replace("tabindex", String.valueOf(tabIndex)));
    }

    if(cssClass != null){
      choice.add(AttributeModifier.append("class", cssClass));
    }
    add(choice);
    return choice;
  }

  private LabelValueChoiceRenderer<String> createRenderer()
  {
    final String[] entries = userPrefDao.getPrefNames(userPrefArea);
    final LabelValueChoiceRenderer<String> renderer = new LabelValueChoiceRenderer<String>();
    for (final String entry : entries) {
      renderer.addValue(entry, entry);
    }
    renderer.addValue(ADD_NEW_ENTRY, getString("userPref.favorite.create"));
    return renderer;
  }

  @Override
  protected void onBeforeRender()
  {
    super.onBeforeRender();
    if (refresh == true) {
      refresh = false;
      final LabelValueChoiceRenderer<String> renderer = createRenderer();
      choice.setChoiceRenderer(renderer);
      choice.setChoices(renderer.getValues());
    }
  }

  /**
   * If set to true (default) after selection of an item the selection is cleared.
   * @param clearSelectionAfterSelection
   * @return this.
   */
  public FavoritesChoicePanel<T, F> setClearSelectionAfterSelection(final boolean clearSelectionAfterSelection)
  {
    this.clearSelectionAfterSelection = clearSelectionAfterSelection;
    return this;
  }

  public FavoritesChoicePanel<T, F> setNullKey(final String nullKey)
  {
    this.nullKey = nullKey;
    return this;
  }

  /**
   * @return the selected
   */
  public String getSelected()
  {
    return selected;
  }

  public void setSelected(final String selected)
  {
    this.selected = selected;
  }

  /**
   * Tries to get the current selected favorite as object.
   */
  public F getCurrentFavorite()
  {
    if (StringUtils.isEmpty(selected) == true) {
      return null;
    }
    final UserPrefDO userPref = userPrefDao.getUserPref(userPrefArea, selected);
    if (userPref != null) {
      final F favorite = newFavoriteInstance(null);
      userPrefDao.fillFromUserPrefParameters(userPref, favorite);
      return favorite;
    }
    return null;
  }

  /**
   * @param currentObject Current selected object or null.
   * @return
   */
  protected abstract F newFavoriteInstance(final T currentObject);

  protected abstract void select(final F favorite);

  protected abstract T getCurrentObject();
}
