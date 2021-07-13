/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.ihk;

import de.micromata.merlin.excel.ExcelRow;
import de.micromata.merlin.excel.ExcelSheet;
import de.micromata.merlin.excel.ExcelWorkbook;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.framework.time.PFDateTime;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

import static java.time.temporal.ChronoUnit.*;
import static org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.getUser;

/**
 * Created by mnuhn on 05.12.2019
 * Updated by mweishaar, jhpeters and mopreusser on 27.07.2020 with updated IHK-Fields in XLSX file
 * Updated by mweishaar on 09.07.2021
 */
class IHKExporter
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IHKExporter.class);
  private static final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");

  private static final int FIRST_DATA_ROW_NUM = 2;

  private String teamname;
  private int ausbildungsjahr = -1;
  private LocalDate ausbildungsbeginn;
  private String docNr = "error";
  private TimeZone timeZone;

  // returns the filled excel as byteArray
  byte[] getExcel(final List<TimesheetDO> timesheets, LocalDate ausbildungsBeginn,
      String teamName, int ausbildungsJahr, TimeZone usersTimeZone)
  {
    if (timesheets.size() < 1) {
      log.error("in getExcel(...) List<TimesheetDO> is empty");
      return new byte[] {};
    }

    teamname = teamName;
    ausbildungsjahr = ausbildungsJahr;
    ausbildungsbeginn = ausbildungsBeginn;
    timeZone = usersTimeZone;

    ExcelSheet excelSheet = null;
    ExcelRow emptyRow = null;
    ClassPathResource classPathResource = new ClassPathResource("VorlageWochenbericht.xlsx");

    try (ExcelWorkbook workbook = new ExcelWorkbook(classPathResource.getInputStream(), classPathResource.getPath())) {
      excelSheet = workbook.getSheet(0);

      emptyRow = excelSheet.getRow(2);

      final int anzNewRows = timesheets.size(); // 1 already exists

      setFirstRow(timesheets, excelSheet);
      createNewRow(excelSheet, emptyRow, anzNewRows);

      double hourCounter = 0;

      for (int i = 0; i < anzNewRows; i++) {
        final TimesheetDO timesheet = timesheets.get(i);
        hourCounter = setNewRows(hourCounter, timesheet, excelSheet, i);
      }

      excelSheet.getRow(FIRST_DATA_ROW_NUM + anzNewRows).getCell(5).setCellValue(trimDouble(hourCounter));

      return returnByteFile(excelSheet);
    } catch (NullPointerException | IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  private void setFirstRow(final List<TimesheetDO> timesheets, ExcelSheet excelSheet)
  {
    PFDateTime mondayDate = PFDateTime.from(Objects.requireNonNull(timesheets.get(0).getStartTime()))
        .getBeginOfWeek().getEndOfDay();
    PFDateTime sundayDate = mondayDate.getEndOfWeek().getEndOfDay();
    sdf.setTimeZone(timeZone);

    // run exception
    if (excelSheet == null) {
      return;
    }

    String contentOfCell = excelSheet.getRow(0).getCell(0).getStringCellValue();

    if (contentOfCell == null) {
      log.error("in setFirstRow(...) contentOfCell is null");
      return;
    }
    contentOfCell = contentOfCell.replace("#idName", getCurrentAzubiName());
    // KR: contentOfCell = contentOfCell.replace("#idYear", getCurrentAzubiYear(sundayDate));
    contentOfCell = contentOfCell.replace("#idYear", getCurrentAzubiYear(sundayDate.getUtilDate()));
    contentOfCell = contentOfCell.replace("#idNr", getDocNrByDate(sundayDate));
    contentOfCell = contentOfCell.replace("#idFirstDate",
        mondayDate.getLocalDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
    contentOfCell = contentOfCell.replace("#idLastDate",
        sundayDate.getLocalDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
    contentOfCell = contentOfCell.replace("#idDepartment", getDepartment());

    excelSheet.getRow(0).getCell(0).setCellValue(contentOfCell);
  }

  private void createNewRow(ExcelSheet excelSheet, ExcelRow emptyRow, int anzNewRows)
  {
    if (excelSheet == null || emptyRow == null) {
      log.error("in createNewRow(...) excelSheet or emptyRow is null");
      return;
    }

    for (int i = 1; i < anzNewRows; i++) {
      Objects.requireNonNull(
          excelSheet.getRow(FIRST_DATA_ROW_NUM)).copyAndInsert(emptyRow.getSheet()
      );
    }
  }

  private double setNewRows(double hourCounter, final TimesheetDO timesheet, ExcelSheet excelSheet, int cell)
  {
    final double durationInHours = timesheet.getDuration() / (1000.0 * 60.0 * 60.0);
    hourCounter += durationInHours;

    String lernfeld = "";
    String description = "";

    if (!(timesheet.getDescription() == null)) {
      if (timesheet.getDescription().contains("|")) { // If no | in String then IndexOf will be -1
        lernfeld = StringUtils.substringBefore(timesheet.getDescription(), "|").trim();
        description = StringUtils.substringAfter(timesheet.getDescription(), "|").trim();
      } else {
        description = timesheet.getDescription();
      }
    }

    /*
        // if you wanna use merlin style:

        final CellStyle descriptionStyle = excelSheet.getExcelWorkbook().createOrGetCellStyle("description");
        descriptionStyle.setWrapText(true);

        CellStyle style = excelSheet.getExcelWorkbook().createOrGetCellStyle("id");
        style.setWrapText(true);
        excelRow.getCell(1).setCellStyle(style);
    */

    ExcelRow excelRow = excelSheet.getRow(FIRST_DATA_ROW_NUM + cell);

    excelRow.getCell(0).setCellValue(sdf.format(timesheet.getStartTime()));
    excelRow.getCell(1).setCellValue(description);
    excelRow.getCell(3).setCellValue(lernfeld);
    excelRow.getCell(4).setCellValue(trimDouble(durationInHours));
    excelRow.getCell(5).setCellValue(trimDouble(hourCounter));


    /*
      Calculate height of cell from the content lenght and the number of line breaks
    */

    String puffer = description;
    int counterOfBreaking = -1, counterOfOverlength = 0;

    String[] pufferSplit = puffer.split("\n");

    // check for line-breaks
    for (int i = 0; i < pufferSplit.length; i++) {
      counterOfBreaking++;
      counterOfOverlength += pufferSplit[i].length() / 70;
    }

    excelRow.setHeight(14 + counterOfOverlength * 14 + counterOfBreaking * 14);

    return hourCounter;
  }

  private String trimDouble(double value)
  {
    DecimalFormat df = new DecimalFormat("#.##");
    return df.format(value);
  }

  private byte[] returnByteFile(ExcelSheet excelSheet)
  {
    try (ExcelWorkbook workbook = excelSheet.getExcelWorkbook()) {
      ByteArrayOutputStream byteArrayOutputStream = workbook.getAsByteArrayOutputStream();

      return byteArrayOutputStream.toByteArray();
    }
  }

  private String getCurrentAzubiName()
  {
    return Objects.requireNonNull(getUser()).getFullname();
  }

  // KR: Hier als Parameter besser PFDateTime nehmen. Bei der Konvertierung in Util-Date landet man schon wieder bei UTC.
  private String getCurrentAzubiYear(Date date)
  {
    String azubiYear = "";

    // only needed if someone doesn't start in the first year otherwise it should be set on -1 in the address-book
    if (ausbildungsjahr > 0) {
      azubiYear = ausbildungsjahr + "";
      return azubiYear;
    }

    if (ausbildungsbeginn != null) {
      Period period = Period.between(ausbildungsbeginn, date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
      double diff = period.getYears();
      if (diff < 1.0)
        return "1";
      if (diff < 2.0)
        return "2";
      if (diff <= 3.0)
        return "3";
    } else {
      log.info("ihk plugin: ausbildungsbeginn is null");
      return "UNKNOWN";
    }
    return "UNKNOWN";
  }

  private String getDocNrByDate(PFDateTime sundayDate)
  {
    long diff = 0;
    if (ausbildungsbeginn != null) {
      diff = DAYS.between(ausbildungsbeginn, sundayDate.getLocalDate());
    } else {
      log.info("ihk plugin: ausbildungsbeginn is null");
    }

    // if beginDate is at a weekend, the first week will be after the weekend. And balance missing week of difference
    boolean isWeekend = PFDateTime.from(ausbildungsbeginn).isWeekend();
    int ifWeekend = isWeekend ? 0 : 1;

    docNr = "" + ((int) Math.ceil((float) diff / 7) + ifWeekend);
    return docNr;
  }

  private String getDepartment()
  {
    if (teamname != null) {
      return teamname;
    } else {
      log.info("ihk plugin: teamname was null");
      return "UNKNOWN";
    }
  }

  public String getDocNr()
  {
    return docNr;
  }
}
