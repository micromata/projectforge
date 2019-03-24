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

package org.projectforge.address;

import static org.testng.AssertJUnit.*;

import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.AddressDao;
import org.projectforge.business.address.PersonalAddressDO;
import org.projectforge.business.address.PersonalAddressDao;
import org.projectforge.test.AbstractTestNGBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.testng.annotations.Test;

public class PersonalAddressTest extends AbstractTestNGBase
{
  @Autowired
  private PersonalAddressDao personalAddressDao;

  @Autowired
  private AddressDao addressDao;

  @Autowired
  private TransactionTemplate txTemplate;

  @Test
  public void testSaveAndUpdate()
  {
    logon(ADMIN);
    final Integer[] addressIds = new Integer[1];
    txTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    txTemplate.execute(new TransactionCallback()
    {
      @Override
      public Object doInTransaction(TransactionStatus status)
      {
        AddressDO address = new AddressDO();
        address.setFirstName("Kai");
        address.setName("Reinhard");
        address.setMobilePhone("+49 170 123 456");
        address.setFax("+49 561 316793-11");
        address.setBusinessPhone("+49 561 316793-0");
        address.setPrivatePhone("+49 561 12345678");
        addressIds[0] = (Integer) addressDao.save(address);

        PersonalAddressDO personalAddress = new PersonalAddressDO();
        AddressDO a = addressDao.getOrLoad(addressIds[0]);
        personalAddress.setAddress(a);
        personalAddress.setOwner(getUser(ADMIN));
        personalAddress.setFavoriteCard(true);
        personalAddress.setFavoriteBusinessPhone(true);
        personalAddress.setFavoriteMobilePhone(true);
        personalAddressDao.saveOrUpdate(personalAddress);
        return null;
      }
    });

    txTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    txTemplate.execute(new TransactionCallback()
    {
      @Override
      public Object doInTransaction(TransactionStatus status)
      {
        PersonalAddressDO personalAddress = personalAddressDao.getByAddressId(addressIds[0]);
        assertEquals(personalAddress.getAddressId(), addressIds[0]);
        assertEquals(personalAddress.getOwnerId(), getUser(ADMIN).getId());
        assertTrue(personalAddress.isFavoriteCard());
        assertTrue(personalAddress.isFavoriteBusinessPhone());
        assertTrue(personalAddress.isFavoriteMobilePhone());
        assertFalse(personalAddress.isFavoritePrivatePhone());
        assertFalse(personalAddress.isFavoriteFax());
        return null;
      }
    });

    /*
     * txTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW); txTemplate.execute(new
     * TransactionCallback() { public Object doInTransaction(TransactionStatus status) { PersonalAddressDO
     * personalAddress = personalAddressDao.getByAddressId(addressIds[0]); personalAddress.setCard(false);
     * personalAddress.setBusinessPhone(false); personalAddress.setMobilePhone(false); personalAddress.setFax(false);
     * personalAddress.setPrivatePhone(false); personalAddressDao.saveOrUpdate(personalAddress); return null; } });
     * txTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW); txTemplate.execute(new
     * TransactionCallback() { public Object doInTransaction(TransactionStatus status) { PersonalAddressDO
     * personalAddress = personalAddressDao.getByAddressId(addressIds[0]); assertNull(
     * "Entry should be deleted (because all values are false).", personalAddress); return null; } });
     */
  }
}
