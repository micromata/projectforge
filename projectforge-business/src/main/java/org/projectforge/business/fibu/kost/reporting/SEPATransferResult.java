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

package org.projectforge.business.fibu.kost.reporting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.projectforge.business.fibu.EingangsrechnungDO;

/**
 * This class contains the result of an SEPA transfer generation.
 *
 * @author Stefan Niemczyk (s.niemczyk@micromata.de)
 */
public class SEPATransferResult
{
  private byte[] xml;
  private Map<EingangsrechnungDO, List<SEPATransferGenerator.SEPATransferError>> errors;

  protected SEPATransferResult()
  {
    errors = new HashMap<>();
  }

  public boolean isSuccessful()
  {
    return xml != null;
  }

  public byte[] getXml()
  {
    return xml;
  }

  public void setXml(final byte[] xml)
  {
    this.xml = xml;
  }

  public Map<EingangsrechnungDO, List<SEPATransferGenerator.SEPATransferError>> getErrors()
  {
    return errors;
  }
}
