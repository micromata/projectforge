/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.fibu;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class GenderConverter implements AttributeConverter<Gender, Integer>
{
  private final Gender DEFAULT_GENDER = Gender.NOT_KNOWN;

  @Override
  public Integer convertToDatabaseColumn(Gender gender)
  {
    if (gender == null) {
      gender = DEFAULT_GENDER;
    }
    return gender.getIsoCode();
  }

  @Override
  public Gender convertToEntityAttribute(Integer isoCode)
  {
    // it may be null on an empty database/column
    if (isoCode == null) {
      return DEFAULT_GENDER;
    }

    int intIsoCode = isoCode;
    for (Gender gender : Gender.values()) {
      if (gender.getIsoCode() == intIsoCode) {
        return gender;
      }
    }

    return DEFAULT_GENDER;
  }
}
