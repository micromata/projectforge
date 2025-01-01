/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.export;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.projectforge.Constants;
import org.projectforge.business.excel.ExcelImport;
import org.projectforge.business.fibu.*;
import org.projectforge.framework.i18n.I18nHelper;
import org.projectforge.business.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

public class OrderExportTest extends AbstractTestBase
{
  @Autowired
  private OrderExport orderExport;

  @Test
  public void testExportPeriodAndStatusComment() throws IOException
  {
    List<AuftragDO> auftragDOList = new ArrayList<>();

    I18nHelper.addBundleName(Constants.RESOURCE_BUNDLE_NAME);

    AuftragDO a = new AuftragDO();
    a.setId(1L);
    a.setAngebotsDatum(LocalDate.now());
    a.setTitel("Titel_TEST");
    KundeDO kunde = new KundeDO();
    kunde.setName("Kundenname");
    a.setKunde(kunde);
    a.setStatus(AuftragsStatus.ESKALATION);
    a.setStatusBeschreibung("TESTBESCHREIBUNG");
    a.setPeriodOfPerformanceBegin(LocalDate.of(2020, Month.OCTOBER, 2));
    a.setPeriodOfPerformanceEnd(LocalDate.of(2030, Month.OCTOBER, 2));

    auftragDOList.add(a);
    byte[] export = orderExport.export(auftragDOList);
    boolean hasperformaceBegin = false, hasPerformanceEnd = false, hasStatusBeschreibung = false;
    ExcelImport excelImport = new ExcelImport(new ByteArrayInputStream(export));
    for (Row row : excelImport.getWorkbook().getSheetAt(0)) {
      for (Cell cell : row) {
        if (cell.toString().equals("2020-10-02")) {
          hasperformaceBegin = true;
        }

        if (cell.toString().equals("2030-10-02")) {
          hasPerformanceEnd = true;
        }

        if (cell.getCellType() == CellType.STRING && cell.getRichStringCellValue().getString().trim()
            .equals("TESTBESCHREIBUNG")) {
          hasStatusBeschreibung = true;
        }
      }
    }
    Assertions.assertTrue(hasperformaceBegin);
    Assertions.assertTrue(hasPerformanceEnd);
    Assertions.assertTrue(hasStatusBeschreibung);
  }

  @Test
  public void testExportPaymentSchedule() throws IOException
  {
    List<AuftragDO> auftragDOList = new ArrayList<>();

    I18nHelper.addBundleName(Constants.RESOURCE_BUNDLE_NAME);

    AuftragDO a = new AuftragDO();
    a.setId(2L);
    a.setAngebotsDatum(LocalDate.now());
    a.setTitel("Titel_TEST");
    KundeDO kunde = new KundeDO();
    kunde.setName("Kundenname");
    a.setKunde(kunde);
    a.setStatus(AuftragsStatus.ESKALATION);
    a.setStatusBeschreibung("TESTBESCHREIBUNG");
    a.setPeriodOfPerformanceBegin(LocalDate.of(2020, Month.OCTOBER, 2));
    a.setPeriodOfPerformanceEnd(LocalDate.of(2030, Month.OCTOBER, 2));

    ArrayList<PaymentScheduleDO> paymentSchedules = new ArrayList<>();
    PaymentScheduleDO schedule1 = new PaymentScheduleDO();
    schedule1.setAuftrag(a);
    schedule1.setReached(true);
    schedule1.setComment("SCHEDULE1");
    schedule1.setAmount(new BigDecimal(111));
    schedule1.setScheduleDate(LocalDate.of(2020, Month.OCTOBER, 2));
    schedule1.setNumber((short) 1);
    schedule1.setVollstaendigFakturiert(true);

    PaymentScheduleDO schedule2 = new PaymentScheduleDO();
    schedule2.setAuftrag(a);
    schedule2.setReached(false);
    schedule2.setComment("SCHEDULE2");
    schedule2.setAmount(new BigDecimal(222));
    schedule2.setScheduleDate(LocalDate.of(2030, Month.OCTOBER, 2));
    schedule2.setNumber((short) 2);
    schedule2.setVollstaendigFakturiert(false);
    paymentSchedules.add(schedule1);
    paymentSchedules.add(schedule2);
    paymentSchedules.add(new PaymentScheduleDO());
    a.setPaymentSchedules(paymentSchedules);
    auftragDOList.add(a);
    byte[] export = orderExport.export(auftragDOList);
    boolean hasFirstScheduleDate = false, hasSecondScheduleDate = false, hasCommentfirstSchedule = false,
        hasCommentSecondSchedule = false, hasScheduleNumber = false, hasSetBoolean = false,
        hasAmount1 = false, hasAmount2 = false;
    ExcelImport excelImport = new ExcelImport(new ByteArrayInputStream(export));
    for (Row row : excelImport.getWorkbook().getSheetAt(2)) {
      for (Cell cell : row) {
        if (cell.toString().equals("2020-10-02")) {
          hasFirstScheduleDate = true;
        }

        if (cell.toString().equals("2030-10-02")) {
          hasSecondScheduleDate = true;
        }

        if (cell.getCellType() == CellType.STRING && cell.getRichStringCellValue().getString().trim()
            .equals("SCHEDULE1")) {
          hasCommentfirstSchedule = true;
        }

        if (cell.getCellType() == CellType.STRING && cell.getRichStringCellValue().getString().trim()
            .equals("SCHEDULE2")) {
          hasCommentSecondSchedule = true;
        }

        if (cell.getCellType() == CellType.STRING && cell.getRichStringCellValue().toString().trim()
            .equals("#2") && excelImport.getWorkbook().getSheetAt(2).getRow(2) == row) {
          hasScheduleNumber = true;
        }

        if (cell.getCellType() == CellType.STRING && cell.getRichStringCellValue().getString().trim()
            .equals("x")) {
          hasSetBoolean = true;
        }

        if (cell.getCellType() == CellType.NUMERIC && cell.toString().trim()
            .equals("111.0")) {
          hasAmount1 = true;
        }

        if (cell.getCellType() == CellType.NUMERIC && cell.toString().trim()
            .equals("222.0")) {
          hasAmount2 = true;
        }
      }
    }
    Assertions.assertTrue(hasFirstScheduleDate);
    Assertions.assertTrue(hasSecondScheduleDate);
    Assertions.assertTrue(hasCommentfirstSchedule);
    Assertions.assertTrue(hasAmount1);
    Assertions.assertTrue(hasAmount2);
    Assertions.assertTrue(hasCommentSecondSchedule);
    Assertions.assertTrue(hasScheduleNumber);
    Assertions.assertTrue(hasSetBoolean);
  }
}
