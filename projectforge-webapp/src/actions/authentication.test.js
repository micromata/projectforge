/* eslint-disable */
import { vi } from 'vitest';
import configureMockStore from 'redux-mock-store';
import { thunk } from 'redux-thunk';
import {
    USER_LOGIN_BEGIN,
    USER_LOGIN_FAILURE,
    USER_LOGIN_SUCCESS,
    userLoginBegin,
    userLoginFailure,
    userLoginSuccess,
    login,
    loadUserStatus,
} from './authentication';

const mockStore = configureMockStore([thunk]);

describe('action creators', () => {
    it('userLoginBegin', () => {
        expect(userLoginBegin()).toEqual({ type: USER_LOGIN_BEGIN });
    });

    it('userLoginSuccess', () => {
        expect(userLoginSuccess('user', '1.0', '2024', undefined))
            .toEqual({
                type: USER_LOGIN_SUCCESS,
                payload: { user: 'user', version: '1.0', buildTimestamp: '2024', alertMessage: undefined },
            });
    });

    it('userLoginFailure', () => {
        expect(userLoginFailure('Some error'))
            .toEqual({
                type: USER_LOGIN_FAILURE,
                payload: { error: 'Some error' },
            });
    });
});

describe('login', () => {
    beforeEach(() => {
        vi.restoreAllMocks();
    });

    it('dispatches BEGIN + BEGIN + SUCCESS for valid credentials', async () => {
        const userData = { username: 'demo', admin: false };
        const systemData = { version: '2.0.0', buildTimestamp: '2025-01-01 00:00' };

        global.fetch = vi.fn()
            .mockResolvedValueOnce(
                { ok: true, status: 200, json: () => Promise.resolve({}) },
            )
            .mockResolvedValueOnce({
                ok: true,
                status: 200,
                json: () => Promise.resolve({ userData, systemData, alertMessage: undefined }),
            });

        const store = mockStore({});
        await store.dispatch(login('demo', 'demo123', false));

        expect(store.getActions()).toEqual([
            { type: USER_LOGIN_BEGIN },
            { type: USER_LOGIN_BEGIN },
            {
                type: USER_LOGIN_SUCCESS,
                payload: {
                    user: userData,
                    version: systemData.version,
                    buildTimestamp: systemData.buildTimestamp,
                    alertMessage: undefined,
                },
            },
        ]);
    });

    it('dispatches BEGIN + FAILURE for invalid credentials', async () => {
        global.fetch = vi.fn()
            .mockResolvedValue({
                ok: false,
                status: 401,
                json: () => Promise.resolve({}),
            });

        const store = mockStore({});
        await store.dispatch(login('demo', 'wrong', false));

        expect(store.getActions()).toEqual([
            { type: USER_LOGIN_BEGIN },
            { type: USER_LOGIN_FAILURE, payload: { error: 'Fetch failed: Error 401' } },
        ]);
    });

    it('dispatches BEGIN + FAILURE for network error', async () => {
        global.fetch = vi.fn()
            .mockRejectedValue(new Error('Network error'));

        const store = mockStore({});
        await store.dispatch(login('demo', 'demo123', false));

        expect(store.getActions()).toEqual([
            { type: USER_LOGIN_BEGIN },
            { type: USER_LOGIN_FAILURE, payload: { error: 'Network error' } },
        ]);
    });
});

describe('loadUserStatus', () => {
    beforeEach(() => {
        vi.restoreAllMocks();
    });

    it('dispatches BEGIN + SUCCESS on valid session', async () => {
        const userData = { username: 'existinguser', admin: true };
        const systemData = { version: '2.0.0', buildTimestamp: '2025-05-05 10:00' };
        const alertMessage = 'Some alert';

        global.fetch = vi.fn()
            .mockResolvedValue({
                ok: true,
                status: 200,
                json: () => Promise.resolve({ userData, systemData, alertMessage }),
            });

        const store = mockStore({});
        await store.dispatch(loadUserStatus());

        expect(store.getActions()).toEqual([
            { type: USER_LOGIN_BEGIN },
            {
                type: USER_LOGIN_SUCCESS,
                payload: {
                    user: userData,
                    version: systemData.version,
                    buildTimestamp: systemData.buildTimestamp,
                    alertMessage,
                },
            },
        ]);
    });

    it('dispatches BEGIN + FAILURE on session expired', async () => {
        global.fetch = vi.fn()
            .mockResolvedValue({
                ok: false,
                status: 401,
                json: () => Promise.resolve({}),
            });

        const store = mockStore({});
        await store.dispatch(loadUserStatus());

        const actions = store.getActions();
        expect(actions[0]).toEqual({ type: USER_LOGIN_BEGIN });
        expect(actions[1]).toEqual({ type: USER_LOGIN_FAILURE, payload: { error: undefined } });
    });
});
