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

package org.projectforge.web.rest.converter;

import org.hibernate.Hibernate;
import org.projectforge.business.converter.DOConverter;
import org.projectforge.business.fibu.KundeDO;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.fibu.kost.Kost2Dao;
import org.projectforge.model.rest.Cost2Object;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * For conversion of Kost2DO to cost2 object.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
public class Kost2DOConverter
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(Kost2DOConverter.class);

  @Autowired
  private Kost2Dao kost2Dao;

  public Cost2Object getCost2Object(Kost2DO kost2DO)
  {
    if (kost2DO == null) {
      return null;
    }
    if (Hibernate.isInitialized(kost2DO) == false) {
      final Integer kost2Id = kost2DO.getId();
      kost2DO = kost2Dao.internalGetById(kost2Id);
      if (kost2DO == null) {
        log.error("Oups, kost2 with id '" + kost2Id + "' not found.");
        return null;
      }
    }
    final Cost2Object cost2 = new Cost2Object();
    DOConverter.copyFields(cost2, kost2DO);
    cost2.setNumber(kost2DO.getFormattedNumber());
    if (kost2DO.getKost2Art() != null) {
      cost2.setType(kost2DO.getKost2Art().getName());
    }
    final ProjektDO projektDO = kost2DO.getProjekt();
    KundeDO kundeDO = null;
    if (projektDO != null) {
      cost2.setProject(projektDO.getName());
      kundeDO = projektDO.getKunde();
      if (kundeDO != null) {
        cost2.setCustomer(kundeDO.getName());
      }
    }
    return cost2;
  }
}
