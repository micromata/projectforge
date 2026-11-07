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

import { getLocales } from 'expo-localization';

import { ApiException } from '../api/apiException';

// The catalogs are bundled by Metro (json imports): generated from the I18nResources bundles of the server
// (generated.<locale>.json, GenerateAppI18nMessagesMain) with the hand-written texts of the app laid over them.
const CATALOGS: Record<Locale, Record<string, string>[]> = {
  en: [require('../../../assets/i18n/generated.en.json'), require('../../../assets/i18n/app.en.json')],
  de: [require('../../../assets/i18n/generated.de.json'), require('../../../assets/i18n/app.de.json')],
};

export type Locale = 'en' | 'de';
export const SUPPORTED_LOCALES: readonly Locale[] = ['en', 'de'];

/**
 * The texts of the app (the Flutter app's Translations). Keys are always written out literally (`t('businessCard.new')`):
 * the generator exports only the keys it finds in the sources.
 */
export class Translations {
  private readonly messages: Record<string, string>;

  constructor(
    readonly locale: Locale,
    catalogs: Record<string, string>[],
  ) {
    this.messages = {};
    for (const catalog of catalogs) {
      for (const [key, value] of Object.entries(catalog)) {
        if (!key.startsWith('_')) this.messages[key] = value;
      }
    }
  }

  /**
   * The text of [key] with the placeholders `{0}`, `{1}`, … replaced by [args]. A missing key is shown as the key
   * itself, so it is spotted instead of hidden.
   */
  t = (key: string, ...args: unknown[]): string => {
    let text = this.messages[key] ?? key;
    args.forEach((arg, i) => {
      text = text.split(`{${i}}`).join(arg == null ? '' : String(arg));
    });
    return text;
  };

  /** The text of a failed call: the app text of an ApiException with a messageKey, otherwise its message. */
  errorText = (error: unknown): string => {
    if (error instanceof ApiException && error.messageKey) return this.t(error.messageKey);
    return error instanceof Error ? error.message : String(error);
  };

  /** The catalog of the device's language, English if it isn't supported. */
  static forDevice(): Translations {
    const language = getLocales()[0]?.languageCode ?? 'en';
    const locale = (SUPPORTED_LOCALES as readonly string[]).includes(language) ? (language as Locale) : 'en';
    return new Translations(locale, CATALOGS[locale]);
  }
}

/** The texts of the app for the device's language (fixed while the app runs, as in the Flutter app). */
export const i18n = Translations.forDevice();

/** `t('key', …args)`, see [Translations.t]. Written out as `t('key')` everywhere, for the generator. */
export const t = i18n.t;

export const errorText = i18n.errorText;
