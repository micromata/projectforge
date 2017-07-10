package org.projectforge.test;

import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.ldap.LdapServer;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(FrameworkRunner.class)
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
