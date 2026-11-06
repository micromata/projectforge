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

import { AppNotification, canAcknowledge, canDone, createdAt, htmlToText, NotificationsRepository, sortForDisplay } from '../notifications';

const notification = (fields: Partial<AppNotification>): AppNotification => ({
  id: 1,
  severity: 'INFO',
  display: 'TOAST',
  status: 'OPEN',
  manualDone: false,
  ...fields,
});

describe('htmlToText', () => {
  it('turns html into plain text', () => {
    expect(htmlToText('<p>Hello <b>you</b> &amp; &lt;all&gt;</p><ul><li>one</li><li>two</li></ul>')).toBe(
      'Hello you & <all>\n\n• one\n• two',
    );
    expect(htmlToText('')).toBe('');
    expect(htmlToText(null)).toBe('');
    expect(htmlToText('a<br/>b')).toBe('a\nb');
    expect(htmlToText('&amp;lt;')).toBe('&lt;');
  });
});

describe('notification', () => {
  it('can be acknowledged while open, done if a banner the user may end', () => {
    expect(canAcknowledge(notification({}))).toBe(true);
    expect(canAcknowledge(notification({ status: 'ACKNOWLEDGED' }))).toBe(false);
    expect(canDone(notification({ display: 'BANNER', manualDone: true }))).toBe(true);
    expect(canDone(notification({ display: 'BANNER' }))).toBe(false);
    expect(canDone(notification({ manualDone: true }))).toBe(false);
  });

  it('reads the date as millis or ISO', () => {
    expect(createdAt(notification({ created: 1_700_000_000_000 }))?.getTime()).toBe(1_700_000_000_000);
    expect(createdAt(notification({ created: '2026-11-06T10:00:00Z' }))?.toISOString()).toBe('2026-11-06T10:00:00.000Z');
    expect(createdAt(notification({ created: 'nonsense' }))).toBeNull();
    expect(createdAt(notification({}))).toBeNull();
  });

  it('lists banners first, otherwise in the order of the server', () => {
    const list = [notification({ id: 1 }), notification({ id: 2, display: 'BANNER' }), notification({ id: 3 })];
    expect(sortForDisplay(list).map((it) => it.id)).toEqual([2, 1, 3]);
  });
});

describe('NotificationsRepository', () => {
  it('calls the endpoints of the server', async () => {
    const get = jest.fn(async () => []);
    const post = jest.fn(async () => undefined);
    const repository = new NotificationsRepository({ get: get as never, post: post as never });
    await repository.loadOpen();
    await repository.loadArchived();
    await repository.acknowledge(5);
    await repository.done(6);
    expect(get.mock.calls).toEqual([['/rs/notification/my', { renewLogin: false }], ['/rs/notification/my/archived']]);
    expect(post.mock.calls).toEqual([['/rs/notification/5/acknowledge'], ['/rs/notification/6/done']]);
  });
});
