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

package org.projectforge.test;

import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.*;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.ldap.LdapServer;
import org.junit.jupiter.api.Test;


//@RunWith(FrameworkRunner.class)
//@ExtendWith(FrameworkRunner.class)
@CreateDS(
    partitions = {
        @CreatePartition(
            name = "example",
            suffix = "dc=example dc=org",
            contextEntry = @ContextEntry(
                entryLdif = "dn: dc=example dc=org\n" +
                    "dc: example\n" +
                    "objectClass: top\n" +
                    "objectClass: domain\n\n"
            ),
            indexes = {
                @CreateIndex(attribute = "objectClass"),
                @CreateIndex(attribute = "dc"),
                @CreateIndex(attribute = "ou")
            })
    })
@ApplyLdifFiles("localTest.ldif")
@CreateLdapServer(
    transports = {
        @CreateTransport(protocol = "LDAP")
    })
public class JUnitLDAPTestWrapper extends AbstractLdapTestUnit
{

  public static LdapServer ldapServerWrap;

  @Test
  public void testInitLdap()
  {
    try {
      ldapServer.getDirectoryService().getSchemaManager().enable("nis");
    } catch (Exception e) {
      e.printStackTrace();
    }
    for (Partition partition : ldapServer.getDirectoryService().getPartitions()) {
      try {
        partition.initialize();
      } catch (Exception e) {
        e.printStackTrace();
      }

    }

    ldapServerWrap = ldapServer;
  }
}
