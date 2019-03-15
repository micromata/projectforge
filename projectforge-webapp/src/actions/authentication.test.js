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
    const token = 'ABCDEF';
    const userId = 123;

    Object.freeze(username);
    Object.freeze(password);
    Object.freeze(token);
    Object.freeze(userId);

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
            payload: {
                userId,
                authenticationToken: token,
            },
        };

        expect(userLoginSuccess(userId, token))
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
            .getOnce('/rest/authenticate/getToken', {
                status: 200,
                body: {
                    id: userId,
                    authenticationToken: token,
                },
            }, {
                headers: {
                    'Authentication-Username': username,
                    'Authentication-Password': password,
                },
            })
            .catch(() => {
                throw new Error('mock failed');
            });

        const expectedActions = [
            { type: USER_LOGIN_BEGIN },
            {
                type: USER_LOGIN_SUCCESS,
                payload: {
                    userId,
                    authenticationToken: token,
                },
            },
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
            .getOnce('/rest/authenticate/getToken', {
                status: 200,
                body: {
                    id: userId,
                    authenticationToken: token,
                },
            }, {
                headers: {
                    'Authentication-Username': username,
                    'Authentication-Password': password,
                },
            })
            .catch(() => {
                throw new Error('mock failed');
            });

        const expectedActions = [
            { type: USER_LOGIN_BEGIN },
            {
                type: USER_LOGIN_SUCCESS,
                payload: {
                    userId,
                    authenticationToken: token,
                },
            },
        ];

        const store = mockStore({});

        return store.dispatch(login(username, password, true))
            .then(() => {
                expect(store.getActions())
                    .toEqual(expectedActions);

                expect(cookies.loadAll())
                    .toEqual({
                        TOKEN: token,
                        USER_ID: userId,
                    });
            });
    });

    it('creates USER_LOGIN_FAILURE when fetching login has been failed', () => {
        fetchMock
            .getOnce('/rest/authenticate/getToken', 401)
            .catch(() => {
                throw new Error('mock failed');
            });

        const expectedActions = [
            { type: USER_LOGIN_BEGIN },
            {
                type: USER_LOGIN_FAILURE,
                payload: {
                    error: 'Unauthorized',
                },
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

        cookies.save('TOKEN', 'ABCDEF');

        store.dispatch(logout());

        expect(store.getActions())
            .toEqual(expectedActions);

        expect(cookies.loadAll())
            .toEqual({});
    });
});

describe('check session', () => {
    const userId = 123;
    const token = 'ABCDEF';

    Object.freeze(userId);
    Object.freeze(token);

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
            .getOnce('/rest/authenticate/initialContact', 200, {
                headers: {
                    'Authentication-User-Id': userId,
                    'Authentication-Token': token,
                },
            })
            .catch(() => {
                throw new Error('mock failed');
            });

        const expectedActions = [
            { type: USER_LOGIN_BEGIN },
            {
                type: USER_LOGIN_SUCCESS,
                payload: {
                    userId,
                    authenticationToken: token,
                },
            },
        ];

        cookies.save('TOKEN', token);
        cookies.save('USER_ID', userId);

        const store = mockStore({});

        return store.dispatch(loadSessionIfAvailable())
            .then(() => {
                expect(store.getActions())
                    .toEqual(expectedActions);

                expect(cookies.loadAll())
                    .toEqual({
                        TOKEN: token,
                        USER_ID: userId,
                    });
            });
    });
});
