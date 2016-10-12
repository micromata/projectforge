/////////////////////////////////////////////////////////////////////////////
//
// Project   ProjectForge
//
// Copyright 2001-2016, Micromata GmbH, Kai Reinhard
//           All rights reserved.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.web.common.timeattr;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.projectforge.business.user.I18nHelper;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel;
import org.projectforge.web.wicket.flowlayout.DropDownChoicePanel;

import de.micromata.genome.db.jpa.tabattr.api.AttrDescription;
import de.micromata.genome.db.jpa.tabattr.api.AttrGroup;
import de.micromata.genome.db.jpa.tabattr.api.EntityWithAttributes;

/**
 * A DropDown input field.
 * 
 * @author Florian Blumenstein
 *
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
      String i18nValue = I18nHelper.getLocalizedString(ThreadLocalUserContext.getLocale(), i18nKey);
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
            return String.valueOf(index);
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
