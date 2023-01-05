/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.dvelop

import com.fasterxml.jackson.annotation.JsonIgnore

/**
 * Fragen:
 * * Unterschiedliche Rechnungsadressen für einen TradingPartner?
 * @author K. Reinhard (k.reinhard@micromata.de)
 */
class TradingPartner {
  enum class TypeValue { VENDOR, CUSTOMER, PARTNER }
  enum class ContactTypeValue { COMPANY, PRIVATE }
  enum class ActiveValue { TRUE, FALSE }

  class Type(var value: TypeValue? = null)
  class ContactType(var value: ContactTypeValue? = null)
  class Active(var value: ActiveValue? = null)
  class Organization(
    var id: String = "",
    var number: String? = null,
    var name: String? = null,
  ) {
    override fun equals(other: Any?): Boolean {
      if (other == null || other !is Organization) {
        return false
      }
      return id.equals(other.id)
    }

    override fun hashCode(): Int {
      return id.hashCode()
    }
  }

  companion object {
    var datevKontoFieldId: String? = null
      set(value) {
        field = if (value.isNullOrBlank()) {
          null
        } else {
          value
        }
      }
  }

  /**
   * Id in D.velop
   */
  var id: String? = null

  /**
   * Required
   */
  var number: String? = null

  /**
   * DATEV-Konto-Nr
   */
  @JsonIgnore
  var datevKonto: Int? = null

  var customFields: Map<String, CustomField>?
    set(value) {
      value?.values?.find { it.name == "datevKonto" }?.value?.let {
        datevKonto = it as Int?
      }
      if (value == null) {
        datevKonto = null
        return
      }

    }
    get() {
      if (datevKonto == null || datevKontoFieldId == null) {
        return null
      }
      val result = mutableMapOf<String, CustomField>()
      datevKontoFieldId?.let { id ->
        result[id] =
          CustomField(configID = id, name = "datevKonto", value = datevKonto)
      }
      return result
    }

  /**
   * Read Only Parameter, wird durch companyName oder Vorname/Nachname gesetzt
   */
  var name: String? = null
  var shortName: String? = null

  /**
   * Required
   */
  var contactType: ContactType? = null
  var firstName: String? = null

  /**
   * Pflicht wenn contactType = PRIVATE
   */
  var lastName: String? = null

  /**
   * Pflicht wenn contactType = COMPANY
   */
  var company: String? = null

  /**
   * Required. Objekt mit Wert des Feldes: { value: ""} mögliche Werte durch den Kunden definierbar
   */
  var type: Type? = null

  /**
   * Required
   */
  var active: Active? = null

  /**
   * Required. Objekt mit Id der alphaflow Organisation: { id: ""}
   */
  var organization: Organization? = null
  var street: String? = null
  var zip: String? = null
  var city: String? = null
  var region: String? = null
  var country: String? = null
  var postBoxZip: String? = null
  var postBox: String? = null
  var addressAdditional: String? = null

  var billToStreet: String? = null
  var billToZip: String? = null
  var billToCity: String? = null
  var billToRegion: String? = null
  var billToCountry: String? = null
  var billToAddressAdditional: String? = null

  val isBillToAddressEmpty: Boolean
    @JsonIgnore
    get() {
      return billToStreet.isNullOrBlank() && billToZip.isNullOrBlank() && billToCity.isNullOrBlank() && billToCountry.isNullOrBlank() && billToRegion.isNullOrBlank() && billToAddressAdditional.isNullOrBlank()
    }

  var shipToStreet: String? = null
  var shipToZip: String? = null
  var shipToCity: String? = null
  var shipToRegion: String? = null
  var shipToCountry: String? = null
  var shipToAddressAdditional: String? = null

  var phoneNumber: String? = null
  var faxNumber: String? = null
  var eMail: String? = null
  var website: String? = null
  var remarks: String? = null

  /**
   * Verantwortlicher: Object containing the id of the user {id: "A6930AE8-5754-40FF-BD83-BA75BC66266E"}.
   */
  var responsible: String? = null

  /**
   * Eintrittsdatum: Datumsstring im Format: "2022-05-11 00:00:00"
   */
  var relationSince: String? = null

  /**
   * Austrittsdatum: Datumsstring im Format: "2022-05-11 00:00:00"
   */
  var relationUntil: String? = null

  /**
   * Führende ID im externen System
   */
  var importCode: String? = null
}
