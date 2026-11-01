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

/// A refused or failed call of the server.
class ApiException implements Exception {
  ApiException(
    this.message, {
    this.statusCode,
    this.validationErrors = const [],
    this.messageKey,
  });

  final String message;

  /// An app text (i18n key) instead of [message], for a refusal that isn't the server's (e.g. of an SSO proxy).
  final String? messageKey;
  final int? statusCode;

  /// The field errors of a 406 (`ResponseAction.validationErrors` of the server).
  final List<ValidationError> validationErrors;

  /// Not reachable at all (no network, wrong host): the caller may fall back to cached data.
  bool get isOffline => statusCode == null;

  @override
  String toString() => message;
}

/// Mirror of the server's `ValidationError`.
class ValidationError {
  ValidationError({this.message, this.fieldId, this.messageId});

  factory ValidationError.fromJson(Map<String, dynamic> json) =>
      ValidationError(
        message: json['message'] as String?,
        fieldId: json['fieldId'] as String?,
        messageId: json['messageId'] as String?,
      );

  final String? message;
  final String? fieldId;
  final String? messageId;
}
