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

package org.projectforge.web.common;

import org.projectforge.common.i18n.I18nEnum;
import org.projectforge.framework.i18n.I18nHelper;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

public class I18nEnumChoiceProvider<T extends Enum<T> & I18nEnum> extends ChoiceProvider<T>
{
  private final Class<T> clazz;

  public I18nEnumChoiceProvider(final Class<T> clazz)
  {
    this.clazz = clazz;
  }

  @Override
  public String getDisplayValue(final T choice)
  {
    return I18nHelper.getLocalizedMessage(choice.getI18nKey());
  }

  /**
   * Converts the given Enum value to the Enum name.
   */
  @Override
  public String getIdValue(final T choice)
  {
    return choice.name();
  }

  @Override
  public void query(final String term, final int page, final Response<T> response)
  {
    final String termLowerCase = term != null ? term.toLowerCase() : "";
    final List<T> matchingAuftragsPositionsArten = EnumSet.allOf(clazz).stream()
        .filter(art -> I18nHelper.getLocalizedMessage(art.getI18nKey()).toLowerCase().contains(termLowerCase))
        .collect(Collectors.toList());

    response.addAll(matchingAuftragsPositionsArten);
  }

  /**
   * Converts a Collection of Enum names to a Collection of the corresponding Enum values.
   */
  @Override
  public Collection<T> toChoices(final Collection<String> ids)
  {
    return ids.stream()
        .map(t -> T.valueOf(clazz, t))
        .collect(Collectors.toList());
  }
}
