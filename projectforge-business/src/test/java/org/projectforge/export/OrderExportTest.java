package org.projectforge.export;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.projectforge.business.excel.ExcelImport;
import org.projectforge.business.fibu.AuftragDO;
import org.projectforge.business.fibu.AuftragsStatus;
import org.projectforge.business.fibu.KundeDO;
import org.projectforge.business.fibu.OrderExport;
import org.projectforge.business.fibu.PaymentScheduleDO;
import org.projectforge.framework.i18n.I18nHelper;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class OrderExportTest extends AbstractTestBase
{
  @Autowired
  private OrderExport orderExport;

  @Test
  public void testExportPeriodAndStatusComment() throws IOException
  {
    List<AuftragDO> auftragDOList = new ArrayList<>();

    I18nHelper.addBundleName("I18nResources");

    AuftragDO e = new AuftragDO();
    e.setAngebotsDatum(new Date(new java.util.Date().getTime()));
    e.setTitel("Titel_TEST");
    KundeDO kunde = new KundeDO();
    kunde.setName("Kundenname");
    e.setKunde(kunde);
    e.setAuftragsStatus(AuftragsStatus.ESKALATION);
    e.setStatusBeschreibung("TESTBESCHREIBUNG");
    e.setPeriodOfPerformanceBegin(
        new Date(LocalDateTime.of(2020, Month.OCTOBER, 2, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli()));
    e.setPeriodOfPerformanceEnd(
        new Date(LocalDateTime.of(2030, Month.OCTOBER, 2, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli()));

    auftragDOList.add(e);
    byte[] export = orderExport.export(auftragDOList);
    boolean hasperformaceBegin = false, hasPerformanceEnd = false, hasStatusBeschreibung = false;
    ExcelImport excelImport = new ExcelImport(new ByteArrayInputStream(export));
    for (Row row : excelImport.getWorkbook().getSheetAt(0)) {
      for (Cell cell : row) {
        if (cell.toString().equals("02-Okt-2020")) {
          hasperformaceBegin = true;
        }

        if (cell.toString().equals("02-Okt-2030")) {
          hasPerformanceEnd = true;
        }

        if (cell.getCellType() == Cell.CELL_TYPE_STRING && cell.getRichStringCellValue().getString().trim()
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

    I18nHelper.addBundleName("I18nResources");

    AuftragDO e = new AuftragDO();
    e.setAngebotsDatum(new Date(new java.util.Date().getTime()));
    e.setTitel("Titel_TEST");
    KundeDO kunde = new KundeDO();
    kunde.setName("Kundenname");
    e.setKunde(kunde);
    e.setAuftragsStatus(AuftragsStatus.ESKALATION);
    e.setStatusBeschreibung("TESTBESCHREIBUNG");
    e.setPeriodOfPerformanceBegin(
        new Date(LocalDateTime.of(2020, Month.OCTOBER, 2, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli()));
    e.setPeriodOfPerformanceEnd(
        new Date(LocalDateTime.of(2030, Month.OCTOBER, 2, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli()));

    ArrayList<PaymentScheduleDO> paymentSchedules = new ArrayList<>();
    PaymentScheduleDO schedule1 = new PaymentScheduleDO();
    schedule1.setAuftrag(e);
    schedule1.setReached(true);
    schedule1.setComment("SCHEDULE1");
    schedule1.setAmount(new BigDecimal(111));
    schedule1.setScheduleDate(
        new Date(LocalDateTime.of(2020, Month.OCTOBER, 2, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli()));
    schedule1.setNumber((short) 1);
    schedule1.setVollstaendigFakturiert(true);

    PaymentScheduleDO schedule2 = new PaymentScheduleDO();
    schedule2.setAuftrag(e);
    schedule2.setReached(false);
    schedule2.setComment("SCHEDULE2");
    schedule2.setAmount(new BigDecimal(222));
    schedule2.setScheduleDate(
        new Date(LocalDateTime.of(2030, Month.OCTOBER, 2, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli()));
    schedule2.setNumber((short) 2);
    schedule2.setVollstaendigFakturiert(false);
    paymentSchedules.add(schedule1);
    paymentSchedules.add(schedule2);
    paymentSchedules.add(new PaymentScheduleDO());
    e.setPaymentSchedules(paymentSchedules);
    auftragDOList.add(e);
    byte[] export = orderExport.export(auftragDOList);
    boolean hasFirstScheduleDate = false, hasSecondScheduleDate = false, hasCommentfirstSchedule = false,
        hasCommentSecondSchedule = false, hasScheduleNumber = false, hasSetBoolean = false,
        hasAmount1 = false, hasAmount2 = false;
    ExcelImport excelImport = new ExcelImport(new ByteArrayInputStream(export));
    for (Row row : excelImport.getWorkbook().getSheetAt(2)) {
      for (Cell cell : row) {
        if (cell.toString().equals("02-Okt-2020")) {
          hasFirstScheduleDate = true;
        }

        if (cell.toString().equals("02-Okt-2030")) {
          hasSecondScheduleDate = true;
        }

        if (cell.getCellType() == Cell.CELL_TYPE_STRING && cell.getRichStringCellValue().getString().trim()
            .equals("SCHEDULE1")) {
          hasCommentfirstSchedule = true;
        }

        if (cell.getCellType() == Cell.CELL_TYPE_STRING && cell.getRichStringCellValue().getString().trim()
            .equals("SCHEDULE2")) {
          hasCommentSecondSchedule = true;
        }

        if (cell.getCellType() == Cell.CELL_TYPE_STRING && cell.getRichStringCellValue().toString().trim()
            .equals("#2") && excelImport.getWorkbook().getSheetAt(2).getRow(2) == row) {
          hasScheduleNumber = true;
        }

        if (cell.getCellType() == Cell.CELL_TYPE_STRING && cell.getRichStringCellValue().getString().trim()
            .equals("x")) {
          hasSetBoolean = true;
        }

        if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC && cell.toString().trim()
            .equals("111.0")) {
          hasAmount1 = true;
        }

        if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC && cell.toString().trim()
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
