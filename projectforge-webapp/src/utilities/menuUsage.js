import { getServiceURL } from './rest';

/**
 * Reports a menu entry the user just opened, so the quick access search offers it again.
 *
 * The history is the backend's (RecentMenuEntriesService) and shared by all three frontends: an
 * entry opened here shows up in the search of the new UI as well, and in a second browser.
 *
 * Not fetchJsonPost: the answer is 204 (no json to parse) and an error may not raise an alert -
 * nothing about a convenience list is worth interrupting the navigation the user asked for.
 *
 * @param key MenuItem.key as the server sent it. Anything that is no menu entry is ignored there,
 * so a caller may report what it has.
 */
const reportMenuUsage = (key) => {
    if (!key) {
        return;
    }
    fetch(getServiceURL('menu/recent'), {
        method: 'POST',
        credentials: 'include',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ key }),
        // Most menu entries navigate away: the document is being torn down while this is in flight,
        // which would cancel an ordinary fetch.
        keepalive: true,
    }).catch(() => {
        // Ignored on purpose, see above.
    });
};

export default reportMenuUsage;
