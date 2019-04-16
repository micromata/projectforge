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

import org.junit.jupiter.api.Test;
import org.projectforge.framework.time.DayHolder;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CSVParserTest
{

  //@Test
  public void importPostausgangsBuch() throws Exception
  {
    final FileReader reader = new FileReader("postausgang.csv");
    final FileWriter sqlWriter = new FileWriter("postausgang-import.sql");
    final CSVParser parser = new CSVParser(reader);
    parser.setCsvSeparatorChar(',');
    List<String> line;
    while ((line = parser.parseLine()) != null) {
      final int size = line.size();
      final String dateString = line.get(0);
      final DateFormat formatter = new java.text.SimpleDateFormat("dd.MM.yy");
      final Date parsedDate = (Date) formatter.parse(dateString);
      final DayHolder day = new DayHolder(parsedDate);
      final String empfaenger = escapeQuotationMark(line.get(1));
      final String person = escapeQuotationMark(line.get(2));
      final String inhalt = escapeQuotationMark(line.get(3));
      final String bemerkung = size >= 5 ? escapeQuotationMark(line.get(4)) : "";
      sqlWriter.append("insert into t_orga_postausgang values(nextval('hibernate_sequence'),'2009-10-31 18:05',false,'"
          + day.isoFormat()
          + " 00:00','"
          + day.isoFormat()
          + "','"
          + empfaenger
          + "','"
          + person
          + "','"
          + inhalt
          + "','"
          + bemerkung
          + "','BRIEF');\n");
    }
    reader.close();
    sqlWriter.close();
  }

  private String escapeQuotationMark(final String str)
  {
    return str.replace("'", "''");
  }

  @Test
  public void testWriteAndParseCSV() throws Exception
  {
    String[][] fields = { { "1.1", "1.2", "1.3" }, { "2.1", "2.2", "2.3" }, { "3.1", "3.2", "3.3" } };
    StringWriter writer = new StringWriter();
    CSVWriter csvWriter = new CSVWriter(writer);
    csvWriter.writeLine(fields[0]);
    csvWriter.writeLine(fields[1]);
    csvWriter.writeLine(fields[2]);
    StringReader in = new StringReader(writer.toString());
    CSVParser parser = new CSVParser(in);
    assertLine(parser.parseLine(), "1.1", "1.2", "1.3");
    assertLine(parser.parseLine(), "2.1", "2.2", "2.3");
    assertLine(parser.parseLine(), "3.1", "3.2", "3.3");
    assertNull(parser.parseLine());
  }

  @Test
  public void testParseCSV() throws Exception
  {
    StringReader in = new StringReader(
        "1.1, 1.2, 1.3\n2.1,2.2, 2.3\r\n3.1,3.2,3.3\n10.10.2009,\"Hurzel, Hurzel; \",,5");
    CSVParser parser = new CSVParser(in);
    parser.setCsvSeparatorChar(',');
    assertLine(parser.parseLine(), "1.1", "1.2", "1.3");
    assertLine(parser.parseLine(), "2.1", "2.2", "2.3");
    assertLine(parser.parseLine(), "3.1", "3.2", "3.3");
    assertLine(parser.parseLine(), "10.10.2009", "Hurzel, Hurzel; ", "", "5");
    assertNull(parser.parseLine());

    in = new StringReader(" 1.1; \"1.2\";\"1.3\"\n2.1;2.2; 2.3\r\n3.1;3.2;3.3\n   \t\n  \n ");
    parser = new CSVParser(in);
    assertLine(parser.parseLine(), "1.1", "1.2", "1.3");
    assertLine(parser.parseLine(), "2.1", "2.2", "2.3");
    assertLine(parser.parseLine(), "3.1", "3.2", "3.3");
    assertNull(parser.parseLine());

    in = new StringReader("\"Hello \"\"Kai\"\", how are you?\"; \"1.2\"\n   \t\n  \n ");
    parser = new CSVParser(in);
    assertLine(parser.parseLine(), "Hello \"Kai\", how are you?", "1.2");
    assertNull(parser.parseLine());

    in = new StringReader("\"\"\"Kai\"\", how are you?\" ; \"1.2\"\n   \t\n  \n ");
    parser = new CSVParser(in);
    assertLine(parser.parseLine(), "\"Kai\", how are you?", "1.2");
    assertNull(parser.parseLine());

    // Error checks:
    in = new StringReader("Kai\"\", how are you?\"; \"1.2\"");
    parser = new CSVParser(in);
    try {
      parser.parseLine();
    } catch (Exception ex) {
      assertEquals(CSVParser.createMessage(CSVParser.ERROR_UNEXPECTED_QUOTATIONMARK, "Kai", 1, 4), ex.getMessage());
    }
    in = new StringReader("\"1.2; \"1.2\"");
    parser = new CSVParser(in);
    try {
      parser.parseLine();
    } catch (Exception ex) {
      assertEquals(CSVParser.createMessage(CSVParser.ERROR_UNEXPECTED_CHARACTER_AFTER_QUOTATION_MARK, "", 1, 8),
          ex.getMessage());
      // assertEquals(CSVParser.createMessage(CSVParser.ERROR_DELIMITER_OR_NEW_LINE_EXPECTED_AFTER_QUOTATION_MARK, "", 1, 8),
      // ex.getMessage());
    }
    in = new StringReader("\"Hello Mr.\"\";1.2");
    parser = new CSVParser(in);
    try {
      parser.parseLine();
    } catch (Exception ex) {
      assertEquals(CSVParser.createMessage(CSVParser.ERROR_QUOTATIONMARK_MISSED_AT_END_OF_CELL, "", 1, 0),
          ex.getMessage());
    }
    in = new StringReader("\"Hello \"hurz\",1.2");
    parser = new CSVParser(in);
    try {
      parser.parseLine();
    } catch (Exception ex) {
      assertEquals(CSVParser.createMessage(CSVParser.ERROR_UNEXPECTED_CHARACTER_AFTER_QUOTATION_MARK, "", 1, 9),
          ex.getMessage());
    }
  }

  private void assertLine(List<String> line, String... values)
  {
    assertEquals(values.length, line.size());
    for (int i = 0; i < values.length; i++) {
      assertEquals(values[i], line.get(i));
    }
  }
}
