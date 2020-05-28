/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.getUser;

/**
 * Created by mnuhn on 05.12.2019
 */
class IHKExporter {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IHKExporter.class);
    private static final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");

    private static final int FIRST_DATA_ROW_NUM = 2;

    static private String teamName;
    static private int ausbildungsJahr = -1;
    static private LocalDate ausbildungsStartDate;
    static private String docNr = "error";

    static byte[] getExcel(final List<TimesheetDO> timesheets, LocalDate ausbildungsstartDate, String teamname, int ausbildungsjahr) {
        if (timesheets.size() < 1) {
            return new byte[]{};
        }

        teamName = teamname;
        ausbildungsJahr = ausbildungsjahr;
        ausbildungsStartDate = ausbildungsstartDate;

        ExcelSheet excelSheet = null;
        ExcelRow emptyRow = null;
        ClassPathResource classPathResource = new ClassPathResource("VorlageWochenbericht.xlsx");

        try {
            ExcelWorkbook workbook = new ExcelWorkbook(classPathResource.getInputStream(), classPathResource.getPath());
            excelSheet = workbook.getSheet(0);
            assert excelSheet != null;
            emptyRow = excelSheet.getRow(2);
        } catch (NullPointerException | IOException e) {
            e.printStackTrace();
        }

        /// TODO insert needed rows
        String fileName = "example";
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
    }

    private static void setFirstRow(final List<TimesheetDO> timesheets, ExcelSheet excelSheet) {
        PFDateTime mondayDate = PFDateTime.from(timesheets.get(0).getStartTime()).getBeginOfWeek();
        PFDateTime sundayDate = mondayDate.getEndOfWeek().getBeginOfDay();

        // run exception
        if (excelSheet == null || excelSheet.getRow(0) == null) {
            return;
        }

        String contentOfCell = excelSheet.getRow(0).getCell(0).getStringCellValue();

        contentOfCell = contentOfCell.replace("#idName", getCurrentAzubiName());
        contentOfCell = contentOfCell.replace("#idYear", getCurrentAzubiYear(sundayDate.getUtilDate()));
        contentOfCell = contentOfCell.replace("#idNr", getDocNrByDate(sundayDate.getUtilDate()));
        contentOfCell = contentOfCell.replace("#idFirstDate", sdf.format(mondayDate.getUtilDate()));
        contentOfCell = contentOfCell.replace("#idLastDate", sdf.format(sundayDate.getUtilDate()));
        contentOfCell = contentOfCell.replace("#idDepartment", getDepartment());

        excelSheet.getRow(0).getCell(0).setCellValue(contentOfCell);
    }

    private static void createNewRow(ExcelSheet excelSheet, ExcelRow emptyRow, int anzNewRows) {
        // run exception
        if (excelSheet == null || emptyRow == null) {
            return;
        }

        for (int i = 1; i < anzNewRows; i++) {
            Objects.requireNonNull(excelSheet.getRow(FIRST_DATA_ROW_NUM)).copyAndInsert(emptyRow.getSheet());
        }
    }

    private static double setNewRows(double hourCounter, final TimesheetDO timesheet, ExcelSheet excelSheet, int cell) {
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

        excelSheet.getRow(FIRST_DATA_ROW_NUM + cell).getCell(0).setCellValue(sdf.format(timesheet.getStartTime()));
        excelSheet.getRow(FIRST_DATA_ROW_NUM + cell).getCell(1).setCellValue(description);
        excelSheet.getRow(FIRST_DATA_ROW_NUM + cell).getCell(3).setCellValue(lernfeld);
        excelSheet.getRow(FIRST_DATA_ROW_NUM + cell).getCell(4).setCellValue(trimDouble(durationInHours));
        excelSheet.getRow(FIRST_DATA_ROW_NUM + cell).getCell(5).setCellValue(trimDouble(hourCounter));

        return hourCounter;
    }

    private static String trimDouble(double value) {
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(value);
    }

    private static byte[] returnByteFile(ExcelSheet excelSheet) {
        ExcelWorkbook workbook = excelSheet.getExcelWorkbook();
        ByteArrayOutputStream byteArrayOutputStream = workbook.getAsByteArrayOutputStream();

        return byteArrayOutputStream.toByteArray();
    }

    private static String getCurrentAzubiName() {
        return getUser().getFullname();
    }

    /// TODO set parameters
    private static String getCurrentAzubiYear(Date date) {
        String azubiYear = "";

        if (ausbildungsJahr > 0) {
            azubiYear = ausbildungsJahr + "";
            return azubiYear;
        }

        if (ausbildungsStartDate != null) {
            Period period = Period.between(ausbildungsStartDate, date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            double diff = period.getYears();
            if (diff < 1.0) return "1";
            if (diff < 2.0) return "2";
            if (diff <= 3.0) return "3";
        } else {
            log.info("ihk plugin: ausbildungsStartDate was null");
            return "UNKNOWN";
        }
        return "UNKNOWN";
    }

    private static String getDocNrByDate(Date sundayDate) {
        long diff = 0;
        if (ausbildungsStartDate != null) {
            diff = DAYS.between(ausbildungsStartDate, sundayDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        } else {
            log.info("ihk plugin: ausbildungsStartDate was null");
        }
        docNr = "" + diff / 7;
        return docNr;
    }

    private static String getDepartment() {
        if (teamName != null) {
            return teamName;
        } else {
            log.info("ihk plugin: teamName was null");
            return "UNKNOWN";
        }
    }

    public static String getDocNr() {
        return docNr;
    }
}
