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

package org.projectforge.shared.storage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;

/**
 * Some helper methods used by different packages of ProjectForge.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class StorageUtils
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StorageUtils.class);

  public static void writeDefaultStorageConfig(final File file, final String authenticationToken)
  {
    FileWriter writer = null;
    try {
      writer = new FileWriter(file);
      writer.append(authenticationToken);
    } catch (final IOException ex) {
      final String msg = "Error while writing '" + file.getAbsolutePath() + "': " + ex.getMessage();
      log.error(msg, ex);
      throw new RuntimeException(msg);
    } finally {
      if (writer != null) {
        try {
          writer.close();
        } catch (final IOException ex) {
          // Close quietly.
        }
      }
    }
  }

  public static String readDefaultStorageConfig(final File file)
  {
    FileReader reader = null;
    try {
      reader = new FileReader(file);
      final BufferedReader br = new BufferedReader(reader);
      String s;
      while ((s = br.readLine()) != null) {
        System.out.println(s);
      }
      final String authenticationToken = readString(reader);
      return authenticationToken != null ? authenticationToken.trim() : null;
    } catch (final FileNotFoundException ex) {
      final String msg = "Error while reading '" + file.getAbsolutePath() + "': " + ex.getMessage();
      log.error(msg, ex);
      throw new RuntimeException(msg);
    } catch (final IOException ex) {
      final String msg = "Error while reading '" + file.getAbsolutePath() + "': " + ex.getMessage();
      log.error(msg, ex);
      throw new RuntimeException(msg);
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (final IOException ex) {
          // Close quietly.
        }
      }
    }
  }

  private static String readString(final Reader is) throws IOException
  {
    final StringBuilder sb = new StringBuilder();
    BufferedReader br = null;
    try {
      String line;
      br = new BufferedReader(is);
      while ((line = br.readLine()) != null) {
        sb.append(line);
      }
    } finally {
      if (br != null) {
        br.close();
      }
    }

    return sb.toString();

  }
}
