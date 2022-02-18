/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web.common.timeattr;

import de.micromata.genome.db.jpa.tabattr.api.AttrDescription;
import de.micromata.genome.db.jpa.tabattr.api.AttrGroup;
import de.micromata.genome.db.jpa.tabattr.api.EntityWithAttributes;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.projectforge.framework.i18n.I18nHelper;
import org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel;
import org.projectforge.web.wicket.flowlayout.DropDownChoicePanel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A DropDown input field.
 *
 * @author Florian Blumenstein
 */
public class DropDownAttrWicketComponentFactory implements AttrWicketComponentFactory
{

  private List<String> i18nKeyList;

  @Override
  public ComponentWrapperPanel createComponents(final String id, final AttrGroup group, final AttrDescription desc,
      final EntityWithAttributes entity)
  {
    Map<String, String> keyValueMap = new HashMap<>();
    for (String i18nKey : i18nKeyList) {
      String i18nValue = I18nHelper.getLocalizedMessage(i18nKey);
      keyValueMap.put(i18nKey, i18nValue);
    }

    DropDownChoice<String> dropdownChoice = new DropDownChoice<>("dropDownChoice",
        new AttrModel<>(entity, desc.getPropertyName(), String.class), i18nKeyList, new IChoiceRenderer<String>()
    {
      private static final long serialVersionUID = 8866606967292296621L;

      @Override
      public Object getDisplayValue(String object)
      {
        return keyValueMap.get(object);
      }

      @Override
      public String getIdValue(String object, int index)
      {
        return String.valueOf(index) + "_" + object;
      }

      @Override
      public String getObject(final String indexId, final IModel<? extends List<? extends String>> iModel)
      {
        if (indexId == null) {
          return null;
        }

        String key = null;
        String[] keyArray = indexId.split("\\d+_");
        if (keyArray == null || keyArray.length < 2 || keyArray.length > 2) {
          return null;
        } else {
          key = keyArray[1];
        }

        for (String instance : iModel.getObject()) {
          if (instance.equals(key)) {
            return instance;
          }
        }

        return null;
      }
    }
    );
    setAndOutputMarkupId(dropdownChoice, group, desc);
    dropdownChoice.setRequired(desc.isRequired());

    return new DropDownChoicePanel<>(id, dropdownChoice);
  }

  public void setI18nKeyList(List<String> i18nKeyList)
  {
    this.i18nKeyList = i18nKeyList;
  }

}
