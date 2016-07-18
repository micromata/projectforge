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

package org.projectforge.common;

import org.projectforge.framework.utils.Crypt;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CryptTest
{
  @Test
  public void encryption()
  {
    encryption("hallo", "This is a text");
    encryption("hallo", "");
    encryption(
        "secret",
        "Another much longer text.\n dkfajsöflk djföldkjf öladksjf oaj0weajfü03ijvmü oerijvü093wjevm ü0qierjmv03üjw 19fjölfj asdölfjlökjaöojpiwejv03j w0vjreao");
  }

  private void encryption(final String password, final String data)
  {
    final String encryptedString = Crypt.encrypt(password, data);
    final String decrpytedString = Crypt.decrypt(password, encryptedString);
    Assert.assertEquals(data, decrpytedString);
  }
}
