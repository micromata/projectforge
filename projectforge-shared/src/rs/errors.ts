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

// The answers of failed requests (see src/rs/app-auth.ts for @server).

/** A refusal with field errors (406). @server org.projectforge.ui.ResponseAction */
export interface ResponseAction {
  validationErrors?: ValidationError[] | null;
}

/** @server org.projectforge.ui.ValidationError */
export interface ValidationError {
  fieldId?: string | null;
  messageId?: string | null;
  message?: string | null;
}

/** The second factor of the login is too old for the url (403). @server org.projectforge.rest.pub.next.TwoFactorRequired */
export interface TwoFactorRequired {
  twoFactorRequired: boolean;
}

/**
 * The json body of other errors (RestAuthenticationUtils.sendError, exception handlers): a message only.
 * @noServerClass Several classes answer it.
 */
export interface ErrorMessage {
  /** @optionalRead Not every error answer has one. */
  message?: string | null;
}
