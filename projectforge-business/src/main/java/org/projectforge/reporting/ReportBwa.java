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

package org.projectforge.reporting;

public interface ReportBwa
{
  public ReportBwaZeile getUmsatzerloese();

  public ReportBwaZeile getBestVerdg();

  public ReportBwaZeile getAktEigenleistungen();

  public ReportBwaZeile getGesamtleistung();

  public ReportBwaZeile getMatWareneinkauf();

  public ReportBwaZeile getRohertrag();

  public ReportBwaZeile getSoBetrErloese();

  public ReportBwaZeile getBetrieblRohertrag();

  public ReportBwaZeile getKostenarten();

  public ReportBwaZeile getPersonalkosten();

  public ReportBwaZeile getRaumkosten();

  public ReportBwaZeile getBetrieblSteuern();

  public ReportBwaZeile getVersichBeitraege();

  public ReportBwaZeile getFremdleistungen();

  public ReportBwaZeile getKfzKosten();

  public ReportBwaZeile getWerbeReisekosten();

  public ReportBwaZeile getKostenWarenabgabe();

  public ReportBwaZeile getAbschreibungen();

  public ReportBwaZeile getReparaturInstandh();

  public ReportBwaZeile getSonstigeKosten();

  public ReportBwaZeile getGesamtkosten();

  public ReportBwaZeile getBetriebsErgebnis();

  public ReportBwaZeile getZinsaufwand();

  public ReportBwaZeile getSonstNeutrAufw();

  public ReportBwaZeile getNeutralerAufwand();

  public ReportBwaZeile getZinsertraege();

  public ReportBwaZeile getSonstNeutrErtr();

  public ReportBwaZeile getVerrKalkKosten();

  public ReportBwaZeile getNeutralerErtrag();

  public ReportBwaZeile getKontenklUnbesetzt();

  public ReportBwaZeile getErgebnisVorSteuern();

  public ReportBwaZeile getSteuernEinkUErtr();

  public ReportBwaZeile getVorlaeufigesErgebnis();
}
