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

package org.projectforge.reporting.impl;

import java.util.List;

import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.ProjektStatus;
import org.projectforge.business.utils.HtmlHelper;
import org.projectforge.reporting.Kost2Art;
import org.projectforge.reporting.Kunde;
import org.projectforge.reporting.Projekt;

/**
 * Proxy for ProjektDO;
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * @see ProjektDO
 */
public class ProjektImpl implements Projekt
{
  private ProjektDO projekt;

  private Kunde kunde;

  private List<Kost2Art> kost2Arts;

  public ProjektImpl(ProjektDO projekt)
  {
    this.projekt = projekt;
    if (this.projekt != null) {
      this.kunde = new KundeImpl(this.projekt.getKunde());
    } else {
      this.kunde = new KundeImpl(null);
    }
  }

  public void setKost2Arts(List<Kost2Art> kost2Arts)
  {
    this.kost2Arts = kost2Arts;
  }

  public Integer getId()
  {
    return projekt != null ? projekt.getId() : null;
  }

  public String getDescription()
  {
    return projekt != null ? projekt.getDescription() : "";
  }

  public Integer getInternKost2_4()
  {
    return projekt != null ? projekt.getInternKost2_4() : null;
  }

  public Kunde getKunde()
  {
    return kunde;
  }

  public String getName()
  {
    return projekt != null ? projekt.getName() : "";
  }

  public int getNummer()
  {
    return projekt != null ? projekt.getNummer() : 0;
  }

  public Integer getBereich()
  {
    return projekt != null ? projekt.getBereich() : null;
  }

  public Integer getTeilbereich()
  {
    return projekt != null ? projekt.getTeilbereich() : null;
  }

  public ProjektStatus getStatus()
  {
    return projekt != null ? projekt.getStatus() : null;
  }

  public String getKost()
  {
    return projekt != null ? projekt.getKost() : "";
  }

  public boolean isDeleted()
  {
    return projekt != null ? projekt.isDeleted() : false;
  }

  public String getKost2ArtsAsString()
  {
    if (kost2Arts == null) {
      return "";
    }
    StringBuffer buf = new StringBuffer();
    boolean first = false;
    for (Kost2Art value : kost2Arts) {
      if (first == true) {
        first = false;
      } else {
        buf.append(", ");
      }
      buf.append(value.getFormattedId());
    }
    return buf.toString();
  }

  public String getKost2ArtsAsHtml()
  {
    if (kost2Arts == null) {
      return "";
    }
    StringBuffer buf = new StringBuffer();
    boolean first = true;
    for (Kost2Art art : kost2Arts) {
      boolean suppress = true;
      if (art.isExistsAlready() == true) {
        if (first == false) {
          buf.append(", ");
        }
        if (art.isProjektStandard() == true) {
          buf.append("<span");
          HtmlHelper.attribute(buf, "style", "color: green;");
          buf.append(">").append(art.getFormattedId()).append("</span>");
        } else {
          buf.append(art.getFormattedId());
        }
        suppress = false;
      } else if (art.isProjektStandard() == true) {
        if (first == false) {
          buf.append(", ");
        }
        buf.append("<span");
        HtmlHelper.attribute(buf, "style", "text-decoration: line-through; color: gray;");
        buf.append(">").append(art.getFormattedId()).append("</span>");
        suppress = false;
      } else {
        // Suppress output;
      }
      if (suppress == false && first == true) {
        first = false;
      }
    }
    return buf.toString();
  }

  /**
   * Return the kost2Arts only if set previously via setKost2Arts.
   * 
   * @see org.projectforge.reporting.Projekt#getKost2Arts()
   * @see #setKost2Arts(int[])
   */
  public List<Kost2Art> getKost2Arts()
  {
    return kost2Arts;
  }
}
