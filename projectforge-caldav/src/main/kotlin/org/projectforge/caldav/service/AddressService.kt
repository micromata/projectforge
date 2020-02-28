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

package org.projectforge.caldav.service

import org.projectforge.business.address.PersonalAddressDao
import org.projectforge.caldav.model.AddressBook
import org.projectforge.caldav.model.Contact
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.persistence.EntityManager

@Service
class AddressService {
    @Autowired
    private lateinit var em: EntityManager

    private val log = LoggerFactory.getLogger(AddressService::class.java)

    @Autowired
    private lateinit var addressCache: AddressCache

    @Autowired
    private lateinit var personalAddressDao: PersonalAddressDao

    @Autowired
    private lateinit var vCardService: VCardService

    fun getContactList(addressBook: AddressBook): List<Contact> {
        val favorites = personalAddressDao.favoriteAddressIdList
        return addressCache.getContacts(addressBook, favorites)
    }

    fun createContact(ab: AddressBook?, vcardBytearray: ByteArray?): Contact {
        log.warn("Creation of contacts not supported.")
        return Contact()
        /* val user = ab.user
         try {
             val vcard = vCardService!!.getVCardFromByteArray(vcardBytearray)
             val request = vCardService.getAddressObject(vcard)
             val mapper = ObjectMapper()
             val json = mapper.writeValueAsString(request)
             val url = projectforgeServerAddress + ":" + projectforgeServerPort + RestPaths.buildPath(RestPaths.ADDRESS, RestPaths.SAVE_OR_UDATE)
             val headers = HttpHeaders()
             headers["Accept"] = MediaType.APPLICATION_JSON_VALUE
             headers.contentType = MediaType.APPLICATION_JSON
             headers["authenticationUserId"] = user.pk.toString()
             headers["authenticationToken"] = user.authenticationToken
             val entity: HttpEntity<*> = HttpEntity(json, headers)
             val builder = UriComponentsBuilder.fromHttpUrl(url)
             val response = restTemplate
                     .exchange(builder.build().encode().toUri(), HttpMethod.PUT, entity, AddressObject::class.java)
             val addressObject = response.body
             log.info("Result of rest call: $addressObject")
             return convertRestResponse(ab, addressObject)
         } catch (e: Exception) {
             log.error("Exception while creating contact.", e)
         }
         return null*/
    }

    fun updateContact(contact: Contact, vcardBytearray: ByteArray?): Contact {
        log.warn("Updating of contacts not supported.")
        return Contact()
        /*val user = contact.addressBook.user
        try {
            val vcard = vCardService!!.getVCardFromByteArray(vcardBytearray)
            val request = vCardService.getAddressObject(vcard)
            val mapper = ObjectMapper()
            val json = mapper.writeValueAsString(request)
            val url = projectforgeServerAddress + ":" + projectforgeServerPort + RestPaths.buildPath(RestPaths.ADDRESS, RestPaths.SAVE_OR_UDATE)
            val headers = HttpHeaders()
            headers["Accept"] = MediaType.APPLICATION_JSON_VALUE
            headers.contentType = MediaType.APPLICATION_JSON
            headers["authenticationUserId"] = user.pk.toString()
            headers["authenticationToken"] = user.authenticationToken
            val entity: HttpEntity<*> = HttpEntity(json, headers)
            val builder = UriComponentsBuilder.fromHttpUrl(url)
            val response = restTemplate
                    .exchange(builder.build().encode().toUri(), HttpMethod.PUT, entity, AddressObject::class.java)
            val addressObject = response.body
            log.info("Result of rest call: $addressObject")
            return convertRestResponse(contact.addressBook, addressObject)
        } catch (e: Exception) {
            log.error("Exception while updating contact: " + contact.name, e)
        }
        return contact*/
    }

    fun deleteContact(contact: Contact) {
        log.warn("Updating of contacts not supported.")
        /*val user = contact.addressBook.user
        try {
            val vcard = vCardService!!.getVCardFromByteArray(contact.vcardData)
            val request = vCardService.getAddressObject(vcard)
            val mapper = ObjectMapper()
            val json = mapper.writeValueAsString(request)
            val url = projectforgeServerAddress + ":" + projectforgeServerPort + RestPaths.buildPath(RestPaths.ADDRESS, RestPaths.DELETE)
            val headers = HttpHeaders()
            headers["Accept"] = MediaType.APPLICATION_JSON_VALUE
            headers.contentType = MediaType.APPLICATION_JSON
            headers["authenticationUserId"] = user.pk.toString()
            headers["authenticationToken"] = user.authenticationToken
            val entity: HttpEntity<*> = HttpEntity(json, headers)
            val builder = UriComponentsBuilder.fromHttpUrl(url)
            restTemplate
                    .exchange(builder.build().encode().toUri(), HttpMethod.DELETE, entity, AddressObject::class.java)
            log.info("Delete contact success.")
        } catch (e: Exception) {
            log.error("Exception while deleting contact: " + contact.name, e)
        }*/
    }

    /*private fun convertRestResponse(ab: AddressBook, contactArray: Array<AddressObject>): List<Contact> {
        val result =  mutableListOf<Contact>()
        val calObjList = Arrays.asList(*contactArray)
        calObjList.forEach(Consumer { conObj: AddressObject -> result.add(convertRestResponse(ab, conObj)) })
        return result
    }*/

    companion object {
        private val log = LoggerFactory.getLogger(AddressService::class.java)
    }
}
