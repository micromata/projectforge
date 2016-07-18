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

package org.projectforge.business.fibu.datev;

import org.apache.commons.lang.StringUtils;
import org.projectforge.framework.utils.NumberHelper;


public class KontenplanExcelRow
{
  Integer konto;

  String bezeichnung;

  public Integer getKonto()
  {
    return konto;
  }

  public void setKonto(Integer konto)
  {
    this.konto = konto;
  }

  public String getBezeichnung()
  {
    return bezeichnung;
  }
  
  public void setBezeichnung(String bezeichnung)
  {
    this.bezeichnung = NumberHelper.toPlainString(bezeichnung); // See BuchungssatzImportRow#setBeleg for explanation.
  }

  public String toString()
  {
    String txt = StringUtils.abbreviate(bezeichnung, 30);
    return StringUtils.leftPad(NumberHelper.getAsString(konto), 5) + " " + StringUtils.rightPad(txt, 30);
  }
}
