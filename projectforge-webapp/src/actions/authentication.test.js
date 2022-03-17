import fetchMock from 'fetch-mock/es5/client';
import cookies from 'react-cookies';
import configureMockStore from 'redux-mock-store';
import thunk from 'redux-thunk';
import {
    loadSessionIfAvailable,
    login,
    logout,
    USER_LOGIN_BEGIN,
    USER_LOGIN_FAILURE,
    USER_LOGIN_SUCCESS,
    USER_LOGOUT,
    userLoginBegin,
    userLoginFailure,
    userLoginSuccess,
    userLogout,
} from './authentication';

describe('login', () => {
    const username = 'demo';
    const password = 'demo123';

    Object.freeze(username);
    Object.freeze(password);

    const mockStore = configureMockStore([thunk]);

    afterEach(() => fetchMock.restore());

    it('should create an action to start the login', () => {
        const expectedAction = {
            type: USER_LOGIN_BEGIN,
        };

        expect(userLoginBegin())
            .toEqual(expectedAction);
    });

    it('should create an action to mark the login as success', () => {
        const expectedAction = {
            type: USER_LOGIN_SUCCESS,
        };

        expect(userLoginSuccess())
            .toEqual(expectedAction);
    });

    it('should create an action to mark the login as success', () => {
        const expectedAction = {
            type: USER_LOGIN_FAILURE,
            payload: {
                error: 'Some uncool error message',
            },
        };

        expect(userLoginFailure('Some uncool error message'))
            .toEqual(expectedAction);
    });

    it('creates USER_LOGIN_SUCCESS when fetching login has been done without keepSignedIn', () => {
        fetchMock
            .mock(
                (url, options) => {
                    if (url !== '/rsPublic/login' || options.method !== 'POST') {
                        return false;
                    }

                    const body = JSON.parse(options.body);

                    return body.username === username
                        && body.password === password
                        && !body.stayLoggedIn;
                },
                {
                    status: 200,
                    headers: { 'Set-Cookie': 'JSESSIONID=ABCDEF0123456789' },
                },
            )
            .catch({ throws: new Error('mock failed') });

        const expectedActions = [
            { type: USER_LOGIN_BEGIN },
            { type: USER_LOGIN_SUCCESS },
        ];

        const store = mockStore({});

        return store.dispatch(login(username, password, false))
            .then(() => {
                expect(store.getActions())
                    .toEqual(expectedActions);

                expect(cookies.loadAll())
                    .toEqual({});
            });
    });

    it('creates USER_LOGIN_SUCCESS when fetching login has been done with keepSignedIn', () => {
        fetchMock
            .mock(
                (url, options) => {
                    if (url !== '/√/login' || options.method !== 'POST') {
                        return false;
                    }

                    const body = JSON.parse(options.body);

                    return body.username === username
                        && body.password === password
                        && body.stayLoggedIn;
                },
                {
                    status: 200,
                    headers: { 'Set-Cookie': 'JSESSIONID=ABCDEF0123456789' },
                },
            )
            .catch({ throws: new Error('mock failed') });

        const expectedActions = [
            { type: USER_LOGIN_BEGIN },
            { type: USER_LOGIN_SUCCESS },
        ];

        const store = mockStore({});

        return store.dispatch(login(username, password, true))
            .then(() => {
                expect(store.getActions())
                    .toEqual(expectedActions);

                expect(cookies.loadAll())
                    .toEqual({
                        KEEP_SIGNED_IN: true,
                    });
            });
    });

    it('creates USER_LOGIN_FAILURE when fetching login has been failed', () => {
        fetchMock
            .mock('/rsPublic/login', 401)
            .catch(() => {
                throw new Error('mock failed');
            });

        const expectedActions = [
            { type: USER_LOGIN_BEGIN },
            {
                type: USER_LOGIN_FAILURE,
                payload: { error: 'Unauthorized' },
            },
        ];

        const store = mockStore({});

        return store.dispatch(login(username, password, false))
            .then(() => {
                expect(store.getActions())
                    .toEqual(expectedActions);
            });
    });
});

describe('logout', () => {
    const mockStore = configureMockStore([thunk]);

    it('should create USER_LOGOUT action', () => {
        const expectedAction = {
            type: USER_LOGOUT,
        };

        expect(userLogout())
            .toEqual(expectedAction);
    });

    it('creates USER_LOGOUT during logout', () => {
        const expectedActions = [
            { type: USER_LOGOUT },
        ];

        const store = mockStore({});

        cookies.save('KEEP_SIGNED_IN', 'ABCDEF');

        store.dispatch(logout());

        expect(store.getActions())
            .toEqual(expectedActions);

        expect(cookies.loadAll())
            .toEqual({});
    });
});

describe('check session', () => {
    const mockStore = configureMockStore([thunk]);

    afterEach(() => fetchMock.restore());

    it('creates no action at all', () => {
        const store = mockStore({});

        expect(store.dispatch(loadSessionIfAvailable()))
            .toEqual(null);
        expect(store.getActions())
            .toEqual([]);
    });

    it('creates USER_LOGIN_SUCCESS', () => {
        fetchMock
        // TODO: ADD AUTHENTICATION TEST ENDPOINT
            .getOnce('/rs/userStatus', 200)
            .catch((url, a, b) => {
                throw new Error('mock failed');
            });

        const expectedActions = [
            { type: USER_LOGIN_BEGIN },
            { type: USER_LOGIN_SUCCESS },
        ];

        cookies.save('KEEP_SIGNED_IN', true);

        const store = mockStore({});

        return store.dispatch(loadSessionIfAvailable())
            .then(() => {
                expect(store.getActions())
                    .toEqual(expectedActions);

                expect(cookies.loadAll())
                    .toEqual({
                        KEEP_SIGNED_IN: true,
                    });
            });
    });
});
