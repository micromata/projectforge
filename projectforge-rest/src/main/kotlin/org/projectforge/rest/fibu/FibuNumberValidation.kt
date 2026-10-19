/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.fibu

import org.projectforge.business.PfCaches
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.KundeDao
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.fibu.numberrange.NumberRangeService
import org.projectforge.business.fibu.numberrange.NumberRangeViolation
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.ui.ValidationError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * The number checks of customers and projects as field errors, shared by their edit pages
 * ([CustomerPagesRest], [ProjectEntityRest]) and the project wizard ([ProjectWizardRest]), which
 * checks a new project for a customer that doesn't exist yet.
 *
 * They repeat what [KundeDao.onInsert] and `ProjektDao.assignProjektnummer` check, so the hand built
 * forms mark the number field instead of only toasting the DAO's exception.
 */
@Service
class FibuNumberValidation {
    @Autowired
    private lateinit var caches: PfCaches

    @Autowired
    private lateinit var kundeDao: KundeDao

    @Autowired
    private lateinit var numberRangeService: NumberRangeService

    @Autowired
    private lateinit var projektDao: ProjektDao

    /**
     * The number of a new customer must fit the configured number ranges and be free.
     *
     * @param fieldId The field the errors are reported on.
     */
    fun validateNewCustomerNumber(
        validationErrors: MutableList<ValidationError>,
        nummer: Long?,
        fieldId: String = KundeDO::nummer.name,
    ) {
        nummer ?: return
        numberRangeService.numberRanges.validateCustomerNumber(nummer)?.let { violation ->
            validationErrors.add(ValidationError(translate(violation.i18nKey), fieldId = fieldId))
            return
        }
        if (kundeDao.doesNumberAlreadyExist(KundeDO().also { it.nummer = nummer })) {
            validationErrors.add(
                ValidationError(translate("fibu.kunde.validation.existingCustomerNr"), fieldId = fieldId)
            )
        }
    }

    /**
     * The number of a project must be free within its customer (unique constraint of [ProjektDO]) and its
     * project number must fit the configured number ranges and be free system-wide.
     *
     * @param kundeNummer Number of the project's customer, null for an internal project. The customer
     * need not exist yet: its range follows from the number alone.
     * @param projectId Id of the stored project, null for a new one.
     * @param newKost2Requested Whether cost 2 units are to be created, which take their number from the
     * project number - so it must belong to a number range then.
     * @param fieldPrefix Put in front of the field ids, for a form holding more than the project.
     */
    fun validateProjectNumbers(
        validationErrors: MutableList<ValidationError>,
        kundeNummer: Long?,
        projectId: Long?,
        nummer: Int,
        projektnummer: Int?,
        internKost2_4: Int?,
        newKost2Requested: Boolean,
        fieldPrefix: String = "",
    ) {
        val nummerField = "$fieldPrefix${ProjektDO::nummer.name}"
        val projektnummerField = "$fieldPrefix${ProjektDO::projektnummer.name}"
        val numberRanges = numberRangeService.numberRanges
        val customerRange = numberRanges.rangeForCustomer(kundeNummer)
        val number = if (customerRange != null) {
            if (nummer !in 0..customerRange.maxLocalNumber) {
                validationErrors.add(
                    ValidationError(
                        translateMsg(ProjektDao.LOCAL_NUMBER_OUT_OF_RANGE_I18N_KEY, customerRange.maxLocalNumber),
                        fieldId = nummerField,
                    )
                )
                return
            }
            numberRanges.projectNumber(customerRange, kundeNummer, nummer)
        } else {
            projektnummer ?: projektDao.legacyProjektnummer(kundeNummer != null, internKost2_4, nummer)
        }
        // The form shows number and customer read-only then; this catches a stale form or a direct REST
        // call as a field error rather than ProjektDao.onUpdate's exception.
        val stored = projectId?.let { caches.getProjekt(it) }
        if (stored != null && projektDao.isNumberLocked(stored.id) && number != stored.projektnummer) {
            validationErrors.add(ValidationError(translate(ProjektDao.NUMBER_LOCKED_I18N_KEY), fieldId = nummerField))
            return
        }
        // A customer not stored yet has no projects.
        kundeNummer?.let { caches.getKunde(it) }?.let { kunde ->
            projektDao.getProjekt(kunde, nummer.toLong())?.let { other ->
                if (other.id != projectId) {
                    validationErrors.add(
                        ValidationError(
                            translate(ProjektDao.NUMBER_EXISTS_FOR_CUSTOMER_I18N_KEY),
                            fieldId = nummerField,
                        )
                    )
                }
            }
        }
        if (customerRange == null) {
            // Free project number (or a number of a range without customers), see ProjektDao.assignProjektnummer:
            kundeNummer?.let { numberRanges.validateCustomerNumber(it) }?.let { violation ->
                validationErrors.add(ValidationError(translate(violation.i18nKey), fieldId = "${fieldPrefix}customer"))
                return
            }
            if (number != null) {
                val range = numberRanges.rangeForProjectNumber(number)
                val violation = if (range != null) {
                    if (kundeNummer != null || range.hasCustomers) NumberRangeViolation.FREE_NUMBER_IN_RANGE else null
                } else {
                    numberRanges.validateFreeProjectNumber(number)
                }
                violation?.let {
                    validationErrors.add(ValidationError(translate(it.i18nKey), fieldId = projektnummerField))
                    return
                }
            }
        }
        number?.let { projektDao.getProjektByProjektnummer(it) }?.let { other ->
            if (other.id != projectId) {
                validationErrors.add(
                    ValidationError(
                        translateMsg(ProjektDao.PROJEKTNUMMER_EXISTS_I18N_KEY, number),
                        fieldId = projektnummerField,
                    )
                )
            }
        }
        // Cost 2 units take their number from the project number, which must belong to a number range then.
        val inRange = number?.let { numberRanges.rangeForProjectNumber(it) } != null
        if (!inRange && newKost2Requested) {
            validationErrors.add(
                ValidationError(translate(Kost2Dao.PROJEKT_WITHOUT_NUMBER_RANGE_I18N_KEY), fieldId = projektnummerField)
            )
        }
    }
}
