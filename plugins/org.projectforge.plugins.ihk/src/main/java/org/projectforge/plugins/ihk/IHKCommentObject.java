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

/**
 * Created by mweishaar, jhpeters and mopreusser on 28.05.2020
 */
public class IHKCommentObject {

    String ausbildungsbeginn;
    int ausbildungsjahr;
    String teamname;

    public IHKCommentObject(String ausbildungsbeginn, int ausbildungsjahr, String teamname){

        this.ausbildungsbeginn = ausbildungsbeginn;
        this.ausbildungsjahr = ausbildungsjahr;
        this.teamname = teamname;

    }

    public int getAusbildungsjahr() {
        return ausbildungsjahr;
    }

    public String getAusbildungStartDatum() {
        return ausbildungsbeginn;
    }

    public String getTeamname() {
        return teamname;
    }
}
