/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.carddav

/**
 * Properties that can be requested in a PROPFIND request.
 */
internal enum class PropType(val tag: String, val xmlns: String = CardDavUtils.D, val supported: Boolean = true) {
    /** On call /carddav/users/joe/addressbooks/, not supported by us. */
    ADD_MEMBER("add-member", supported = false),
    ADDRESSBOOK_HOME_SET("addressbook-home-set", CardDavUtils.CARD),
    ADDRESS_DATA("caddress-data"),

    /** On call /carddav/users/joe/addressbooks/, not supported by us. */
    BULK_REQUESTS("bulk-requests", CardDavUtils.ME, supported = false),
    CURRENT_USER_PRINCIPAL("current-user-principal"),
    CURRENT_USER_PRIVILEGE_SET("current-user-privilege-set"),

    /** Not supported by us. */
    DIRECTORY_GATEWAY("directory-gateway", CardDavUtils.CARD, supported = false),

    /** A user-friendly name for the address book collection. */
    DISPLAYNAME("displayname"),
    EMAIL_ADDRESS_SET("email-address-set"),

    /** On call /carddav/users/joe/addressbooks/, not supported by us. */
    GUARDIAN_RESTRICTED("guardian-restricted", CardDavUtils.ME, supported = false),
    GETCTAG("getctag"),
    GETETAG("getetag"),

    /** Maximum image size that can be stored in a vCard. */
    MAX_IMAGE_SIZE("max-image-size", CardDavUtils.CARD),

    /** Maximum size for a resource (e.g. a vCard). */
    MAX_RESOURCE_SIZE("max-resource-size", CardDavUtils.CARD),

    /** On call /carddav/users/joe/addressbooks/, not supported by us. */
    ME_CARD("me-card", CardDavUtils.CS, supported = false),

    /** On call /carddav/users/joe/addressbooks/, not supported by us. */
    OWNER("owner"),
    PRINCIPAL_COLLECTION_SET("principal-collection-set"),
    PRINCIPAL_URL("principal-URL"),

    /** On call /carddav/users/joe/addressbooks/, not supported by us. */
    PUSH_TRANSPORTS("push-transports", CardDavUtils.CS, supported = false),

    /** On call /carddav/users/joe/addressbooks/, not supported by us. */
    PUSHKEY("pushkey", CardDavUtils.CS, supported = false),

    /** Information about storage space availability. */
    QUOTA_AVAILABLE_BYTES("quota-available-bytes"),

    /** Information about used storage space. */
    QUOTA_USED_BYTES("quota-used-bytes"),
    RESOURCE_ID("resource-id"),

    /** Shows that the resource is a collection and optional an address book. */
    RESOURCETYPE("resourcetype"),

    /** Supported REPORT queries, including address book-query and sync-collection. */
    SUPPORTED_REPORT_SET("supported-report-set"),

    /** Used for incremental synchronization. */
    SYNCTOKEN("sync-token"),
}
