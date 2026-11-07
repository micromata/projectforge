// The tests of the earlier Flutter app's push_service_test.
import { ApiClient } from '../../api/apiClient';
import { fakeHttp } from '../../../testing/fakeHttp';
import { Session } from '../../auth/session';
import { PushPlatform, PushReceived, PushService, PushTap, pushTarget } from '../pushService';

function setup({
  production = false,
  offline = false,
  environment = null,
  launch = null,
  granted = true,
  others = [],
}: {
  production?: boolean;
  offline?: boolean;
  environment?: 'SANDBOX' | 'PRODUCTION' | null;
  launch?: PushTap | null;
  granted?: boolean;
  /** The other logins: their server and whether it names the device in its pushes. */
  others?: { url: string; tagged: boolean }[];
} = {}) {
  const { http, requests } = fakeHttp((request) => {
    if (offline) throw new TypeError('Network request failed');
    const other = others.find((it) => request.url.startsWith(it.url));
    return other?.tagged && request.url.endsWith('/pushToken') ? { body: { deviceTagged: true } } : {};
  });
  const client = (url: string) =>
    new ApiClient(url, {
      http,
      appVersion: '1',
      token: () => 't',
      proxyCookies: () => ({}),
      onUnauthorized: () => {},
      onReauthenticate: async () => false,
    });
  const api = client('https://pf.example.com');
  const listeners: {
    token?: (t: string) => void;
    tap?: (t: PushTap) => void;
    received?: (push: PushReceived) => void;
  } = {};
  const none = { remove: () => {} };
  const platform: PushPlatform = {
    requestPermission: async () => granted,
    deviceToken: async () => 'ab12',
    onToken: (l) => ((listeners.token = l), none),
    onTap: (l) => ((listeners.tap = l), none),
    takeLastTap: async () => launch,
    onReceived: (l) => ((listeners.received = l), none),
    setBadge: async () => {},
    environment: () => environment,
  };
  const links: string[] = [];
  const received: (number | null)[] = [];
  const push = new PushService(api, platform, {
    production,
    onTap: (tap) => links.push(tap.link),
    onReceived: (it) => received.push(it.badge),
    others: async () => others.map((it) => client(it.url)),
  });
  const flush = () => new Promise((resolve) => setTimeout(resolve, 0));
  return { push, requests, listeners, links, received, flush };
}

test('token goes to the server once, with the environment', async () => {
  const { push, requests, listeners, flush } = setup();
  await push.start();
  listeners.token!('ab12');
  await flush();
  expect(requests).toHaveLength(1);
  expect(requests[0].url).toBe('https://pf.example.com/rs/appAuth/pushToken');
  expect(requests[0].body).toEqual({ token: 'ab12', environment: 'SANDBOX' });
});

test('the token of the registration coming as an event too is sent once', async () => {
  const { push, requests, listeners, flush } = setup();
  const started = push.start();
  await flush();
  listeners.token!('ab12');
  await started;
  await flush();
  expect(requests).toHaveLength(1);
});

test('the signing told by the native part wins over the build mode', async () => {
  const { push, requests } = setup({ production: true, environment: 'SANDBOX' });
  await push.start();
  expect((requests[0].body as { environment: string }).environment).toBe('SANDBOX');
});

test('release builds use production', async () => {
  const { push, requests } = setup({ production: true });
  await push.start();
  expect((requests[0].body as { environment: string }).environment).toBe('PRODUCTION');
});

test('offline the token is sent again next time', async () => {
  const { push, requests, listeners, flush } = setup({ offline: true });
  await push.start();
  listeners.token!('ab12');
  await flush();
  expect(requests).toHaveLength(2);
});

test('without the permission no token is sent', async () => {
  const { push, requests } = setup({ granted: false });
  await push.start();
  expect(requests).toHaveLength(0);
});

test('links of taps and of the start, the tap of the start only once', async () => {
  const tap = (id: string, link: string): PushTap => ({ id, link, device: null, server: null });
  const { push, listeners, links, received } = setup({ launch: tap('n1', '/chat/5') });
  await push.start();
  expect(links).toEqual(['/chat/5']);
  listeners.tap!(tap('n1', '/chat/5'));
  listeners.tap!(tap('n2', '/timesheet'));
  listeners.received!({ badge: 3, device: null, server: null });
  expect(links).toEqual(['/chat/5', '/timesheet']);
  expect(received).toEqual([3]);
});

test('the other logins get the token, a server not naming the device in its pushes has it removed again', async () => {
  const { push, requests } = setup({
    others: [
      { url: 'https://new.example.com', tagged: true },
      { url: 'https://old.example.com', tagged: false },
    ],
  });
  await push.start();
  expect(requests.map((it) => it.url).sort()).toEqual([
    'https://new.example.com/rs/appAuth/pushToken',
    'https://old.example.com/rs/appAuth/pushToken',
    'https://old.example.com/rs/appAuth/pushToken/remove',
    'https://pf.example.com/rs/appAuth/pushToken',
  ]);
});

describe('pushTarget', () => {
  const session = (serverUrl: string, deviceId: number, userId = 1): Session => ({
    serverUrl,
    token: 't',
    userId,
    deviceId,
    proxyCookies: {},
    appLoginPath: true,
  });
  const active = session('https://a.example.com', 3);
  const other = session('https://b.example.com', 5);
  const sameId = session('https://c.example.com', 3);
  const sessions = [active, other, sameId];

  it('is the other login of the device', () => {
    expect(pushTarget({ device: 5, server: 'b.example.com' }, sessions, active)).toBe(other);
  });

  it('is the active one without device (an older server), for its own device or an unknown one', () => {
    expect(pushTarget({ device: null, server: null }, sessions, active)).toBeNull();
    expect(pushTarget({ device: 9, server: null }, sessions, active)).toBeNull();
    expect(pushTarget({ device: 3, server: 'a.example.com' }, sessions, active)).toBeNull();
  });

  it('tells two logins with the same device id apart by the host', () => {
    expect(pushTarget({ device: 3, server: 'c.example.com' }, sessions, active)).toBe(sameId);
    expect(pushTarget({ device: 3, server: null }, sessions, active)).toBeNull();
  });
});
