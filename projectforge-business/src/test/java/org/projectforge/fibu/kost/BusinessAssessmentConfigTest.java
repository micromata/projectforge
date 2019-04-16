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

package org.projectforge.fibu.kost;

import org.junit.jupiter.api.Test;
import org.projectforge.business.fibu.kost.BusinessAssessmentConfig;
import org.projectforge.business.fibu.kost.BusinessAssessmentRowConfig;
import org.projectforge.common.i18n.Priority;
import org.projectforge.framework.utils.Range;
import org.projectforge.framework.xstream.AliasMap;
import org.projectforge.framework.xstream.XmlHelper;
import org.projectforge.framework.xstream.XmlObjectReader;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BusinessAssessmentConfigTest
{
  @Test
  public void testReadXml()
  {
    final BusinessAssessmentConfig bwa = getBusinessAssessmentConfig();
    assertEquals(50, bwa.getRows().size());
    {
      final BusinessAssessmentRowConfig row = bwa.getRow("1060");
      assertEquals(Priority.HIGH, row.getPriority());
      assertEquals(0, row.getAccountNumberRanges().getValues().size());
      assertEquals(1, row.getAccountNumberRanges().getRanges().size());
      final Range<Integer> range = row.getAccountNumberRanges().getRanges().get(0);
      assertEquals(5700, (int) range.getMinValue());
      assertEquals(5999, (int) range.getMaxValue());
    }
    {
      final BusinessAssessmentRowConfig row = bwa.getRow("sonstigeKosten");
      assertEquals(1, row.getAccountNumberRanges().getValues().size());
      assertEquals(6300, (int) row.getAccountNumberRanges().getValues().get(0));
      assertEquals(1, row.getAccountNumberRanges().getRanges().size());
      final Range<Integer> range = row.getAccountNumberRanges().getRanges().get(0);
      assertEquals(6800, (int) range.getMinValue());
      assertEquals(6855, (int) range.getMaxValue());
    }
    {
      final BusinessAssessmentRowConfig row = bwa.getRow("1312");
      assertEquals(3, row.getAccountNumberRanges().getValues().size());
      assertEquals(6392, (int) row.getAccountNumberRanges().getValues().get(0));
      assertEquals(6895, (int) row.getAccountNumberRanges().getValues().get(1));
      assertEquals(6960, (int) row.getAccountNumberRanges().getValues().get(2));
      assertEquals(0, row.getAccountNumberRanges().getRanges().size());
    }
    {
      final BusinessAssessmentRowConfig row = bwa.getRow("1390");
      assertEquals(0, row.getAccountNumberRanges().getValues().size());
      assertEquals(0, row.getAccountNumberRanges().getRanges().size());
    }
  }

  static BusinessAssessmentConfig getBusinessAssessmentConfig()
  {
    final AliasMap aliasMap = new AliasMap();
    aliasMap.put(BusinessAssessmentRowConfig.class, "row");
    final XmlObjectReader reader = new XmlObjectReader();
    reader.setAliasMap(aliasMap);
    reader.initialize(BusinessAssessmentConfig.class);
    final BusinessAssessmentConfig bwa = (BusinessAssessmentConfig) reader.read(xml);
    return bwa;
  }

  private static final String xml = XmlHelper
      .replaceQuotes(XmlHelper.XML_HEADER + "\n" //
          + "<businessAssessment heading='BWA' overallPerformance='gesamtleistung' merchandisePurchase='matWareneinkauf' preliminaryResult='vorlaeufigesErgebnis'>\n" //
          + "  <rows>\n" //
          + "    <!-- Empty row: -->\n" //
          + "    <row no='1010' />\n" //
          + "    <row no='1020' id='umsatzErloese' accountRange='4000-4799' priority='middle' title='Umsatzerlöse' />\n" //
          + "    <row no='1040' id='bestVerdg' priority='low' title='Best.Verdg. FE/UE' />\n" //
          + "    <row no='1045' id='aktEigenleistungen' priority='low' title='Akt.Eigenleistungen' />\n" //
          + "    <row no='1050' />\n" //
          + "    <row no='1051' id='gesamtleistung' value='=umsatzErloese+bestVerdg+aktEigenleistungen' priority='high' title='Gesamtleistung' />\n" //
          + "    <row no='1052' />\n" //
          + "    <row no='1060' id='matWareneinkauf' accountRange='5700-5999' priority='high' title='Mat./Wareneinkauf' />\n" //
          + "    <row no='1070' />\n" //
          + "    <row no='1080' id='rohertrag' value='=gesamtleistung+matWareneinkauf' priority='high' title='Rohertrag' />\n" //
          + "    <row no='1081' />\n" //
          + "    <row no='1090' id='soBetrErloese' accountRange='4830,4947' priority='low' title='So. betr. Erlöse' />\n" //
          + "    <row no='1091' />\n" //
          + "    <row no='1092' id='betrieblRohertrag' priority='middle' title='Betriebl. Rohertrag' />\n" //
          + "    <row no='1093' />\n" //
          + "    <row no='1094' id='kostenarten' priority='low' title='Kostenarten' />\n" //
          + "    <row no='1100' id='personalkosten' accountRange='6000-6199' priority='high' title='Personalkosten' />\n" //
          + "    <row no='1120' id='raumkosten' accountRange='6310-6350' priority='low' title='Raumkosten' />\n" //
          + "    <row no='1140' id='betrieblSteuern' accountRange='7685' priority='low' title='Betriebl. Steuern' />\n" //
          + "    <row no='1150' id='versichBeitraege' accountRange='6400-6430' priority='low' title='Versich./Beiträge' />\n" //
          + "    <row no='1160' id='fremdleistungen' accountRange='7800' priority='low' title='Fremdleistungen' />\n" //
          + "    <row no='1180' id='kfzKosten' accountRange='6520-6599' priority='low' title='Kfz-Kosten (o. St.)' />\n" //
          + "    <row no='1200' id='werbeReisekosten' accountRange='6600-6699' priority='low' title='Werbe-/Reisekosten' />\n" //
          + "    <row no='1220' id='kostenWarenabgabe' accountRange='6740' priority='low' title='Kosten Warenabgabe' />\n" //
          + "    <row no='1240' id='abschreibungen' accountRange='6200-6299' priority='low' title='Abschreibungen' />\n" //
          + "    <row no='1250' id='reparaturInstandh' accountRange='6470-6490' priority='low' title='Reparatur/Instandh.' />\n" //
          + "    <row no='1260' id='sonstigeKosten' accountRange='6300,6800-6855' priority='low' title='sonstige Kosten' />\n" //
          + "    <row no='1280' id='gesamtKosten'\n" //
          + "      value='=personalkosten+raumkosten+betrieblSteuern+versichBeitraege+fremdleistungen+kfzKosten+werbeReisekosten+kostenWarenabgabe+abschreibungen+reparaturInstandh+sonstigeKosten'\n" //
          + "      priority='high' title='Gesamtkosten' />\n" //
          + "    <row no='1290' />\n" //
          + "    <row no='1300' id='betriebsErgebnis' value='=rohertrag+gesamtKosten' priority='high' title='Betriebsergebnis' />\n" //
          + "    <row no='1301' />\n" //
          + "    <row no='1310' id='zinsaufwand' accountRange='7305,7310' priority='low' indent='2' title='Zinsaufwand' />\n" //
          + "    <row no='1312' id='sonstNeutrAufw' accountRange='6392,6895,6960' priority='low' indent='2' title='Sonst. neutr. Aufw.' />\n" //
          + "    <row no='1320' id='neutralerAufwand' value='=zinsaufwand+sonstNeutrAufw' priority='low' title='Neutraler Aufwand' />\n" //
          + "    <row no='1321' />\n" //
          + "    <row no='1322' id='zinsertraege' accountRange='7100,7110' priority='low' indent='2' title='Zinserträge' />\n" //
          + "    <row no='1323' id='sonstNeutrErtr' accountRange='4845,4855,4925,4930,4937,4960,4970,4975' priority='low' indent='2' title='Sonst. neutr. Ertr' />\n" //
          + "    <row no='1324' id='verrKalkKosten' priority='low' indent='2' title='Verr. kalk. Kosten' />\n" //
          + "    <row no='1330' id='neutralerErtrag' priority='low' title='Neutraler Ertrag' />\n" //
          + "    <row no='1331' />\n" //
          + "    <row no='1340' id='kontenklUnbesetzt' priority='low' title='Kontenkl. unbesetzt' />\n" //
          + "    <row no='1342' />\n" //
          + "    <row no='1345' id='ergebnisVorSteuern' value='=betriebsErgebnis+neutralerAufwand+neutralerErtrag' priority='high' title='Ergebnis vor Steuern' />\n" //
          + "    <row no='1350' />\n" //
          + "    <row no='1355' id='steuernEinkUErtr' accountRange='7600-7640' priority='low' title='Steuern Eink.u.Ertr' />\n" //
          + "    <row no='1360' />\n" //
          + "    <row no='1380' id='vorlaeufigesErgebnis' value='=ergebnisVorSteuern+steuernEinkUErtr' priority='high' title='Vorläufiges Ergebnis' />\n" //
          + "    <row no='1390' value='='/>\n" //
          + "    <row no='' id='erfolgsquote' priority='high' title='Erfolgsquote'>\n" //
          + "      <value>\n" //
          + "        if (gesamtleistung != 0.0) {\n" //
          + "          return betriebsErgebnis * 100 / gesamtleistung;\n" //
          + "        } else {\n" //
          + "          return 0.0;\n" //
          + "        }\n" //
          + "      </value>\n" //
          + "    </row>\n" //
          + "    <row no='' id='relativePerformance' priority='high' title='relative Performance'>\n" //
          + "      <value>\n" //
          + "        if (gesamtleistung != 0.0) {\n" //
          + "          return vorlaeufigesErgebnis / gesamtleistung;\n" //
          + "        } else {\n" //
          + "          return 0.0;\n" //
          + "        }\n" //
          + "      </value>\n" //
          + "    </row>\n" //
          + "  </rows>\n" //
          + "</businessAssessment>");
}
