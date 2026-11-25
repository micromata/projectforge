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

import { numberOf, payloadOf } from '../pushPayload';

describe('payloadOf', () => {
  it('reads an iOS push: the ids beside aps, in the payload of the trigger, as numbers', () => {
    const request = {
      trigger: {
        type: 'push',
        payload: { aps: { badge: 3 }, link: '/chat/5', notificationId: 42, device: 7, server: 'pf.example.org' },
      },
      content: { data: {} },
    };
    expect(payloadOf(request)).toEqual({ link: '/chat/5', device: 7, server: 'pf.example.org', badge: null });
  });

  it('reads an Android push: the data of the message, text only', () => {
    const request = {
      trigger: { type: 'push', remoteMessage: {} },
      content: { data: { link: '/chat/5', notificationId: '42', device: '7', server: 'pf.example.org', badge: '3' } },
    };
    expect(payloadOf(request)).toEqual({ link: '/chat/5', device: 7, server: 'pf.example.org', badge: 3 });
  });

  it('the trigger wins over the data (iOS: data is the body of the payload)', () => {
    const request = { trigger: { payload: { device: 7 } }, content: { data: { device: '9' } } };
    expect(payloadOf(request).device).toBe(7);
  });

  it('a push of an older server or without anything: empty link, no ids', () => {
    expect(payloadOf({ trigger: null, content: { data: null } })).toEqual({
      link: '',
      device: null,
      server: null,
      badge: null,
    });
    expect(payloadOf({ trigger: { payload: { aps: {} } }, content: {} })).toEqual({
      link: '',
      device: null,
      server: null,
      badge: null,
    });
  });

  it('values of the wrong type are ignored', () => {
    const request = { trigger: { payload: { link: 5, device: 'x7', server: 3 } }, content: { data: {} } };
    expect(payloadOf(request)).toEqual({ link: '', device: null, server: null, badge: null });
  });
});

describe('numberOf', () => {
  it('takes numbers and digit strings only', () => {
    expect(numberOf(7)).toBe(7);
    expect(numberOf('42')).toBe(42);
    expect(numberOf('')).toBeNull();
    expect(numberOf('-1')).toBeNull();
    expect(numberOf('4.2')).toBeNull();
    expect(numberOf(null)).toBeNull();
    expect(numberOf(undefined)).toBeNull();
  });
});
