// The tests of projectforge-flutter/test/core/push/push_service_test.dart.
import { ApiClient } from '../../api/apiClient';
import { fakeHttp } from '../../../testing/fakeHttp';
import { PushPlatform, PushService, PushTap } from '../pushService';

function setup({
  production = false,
  offline = false,
  environment = null,
  launch = null,
  granted = true,
}: {
  production?: boolean;
  offline?: boolean;
  environment?: 'SANDBOX' | 'PRODUCTION' | null;
  launch?: PushTap | null;
  granted?: boolean;
} = {}) {
  const { http, requests } = fakeHttp(() => {
    if (offline) throw new TypeError('Network request failed');
    return {};
  });
  const api = new ApiClient('https://pf.example.com', {
    http,
    appVersion: '1',
    token: () => 't',
    proxyCookies: () => ({}),
    onUnauthorized: () => {},
    onReauthenticate: async () => false,
  });
  const listeners: { token?: (t: string) => void; tap?: (t: PushTap) => void; received?: () => void } = {};
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
  let received = 0;
  const push = new PushService(api, platform, {
    production,
    onLink: (link) => links.push(link),
    onReceived: () => received++,
  });
  const flush = () => new Promise((resolve) => setTimeout(resolve, 0));
  return { push, requests, listeners, links, received: () => received, flush };
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
  const { push, listeners, links, received } = setup({ launch: { id: 'n1', link: '/chat/5' } });
  await push.start();
  expect(links).toEqual(['/chat/5']);
  listeners.tap!({ id: 'n1', link: '/chat/5' });
  listeners.tap!({ id: 'n2', link: '/timesheet' });
  listeners.received!();
  expect(links).toEqual(['/chat/5', '/timesheet']);
  expect(received()).toBe(1);
});
