/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web.wicket.converter;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;
import org.projectforge.framework.time.PFDay;
import org.projectforge.framework.time.PFDayUtils;

import java.time.LocalDate;
import java.util.Locale;

public class MyLocalDateConverter implements IConverter<LocalDate> {

  @Override
  public LocalDate convertToObject(String s, Locale locale) throws ConversionException {
    if (StringUtils.isBlank(s)) {
      return null;
    }
    return PFDayUtils.parseDate(s);
  }

  @Override
  public String convertToString(LocalDate localDate, Locale locale) {
    if (localDate == null) {
      return null;
    }
    return PFDay.from(localDate).format();//getIsoString();
  }
}
