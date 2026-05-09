import reducer from './authentication';

const exampleError = 'Uncool error message.';

Object.freeze(exampleError);

const initialState = {
    loading: true,
    error: null,
    user: null,
};

describe('reducer', () => {
    it('initial state', () => {
        expect(reducer(undefined, {}))
            .toEqual(initialState);
    });

    it('unknown action returns current state', () => {
        const state = {
            loading: false,
            error: null,
            user: { name: 'test' },
        };

        Object.freeze(state);

        expect(reducer(state, { type: 'UNKNOWN_ACTION' }))
            .toEqual(state);
    });
});

describe('USER_LOGIN_BEGIN', () => {
    it('resets to loading with null user', () => {
        const state = {
            loading: false,
            error: 'some error',
            user: { name: 'old' },
        };

        Object.freeze(state);

        expect(reducer(state, { type: 'USER_LOGIN_BEGIN' }))
            .toEqual({
                loading: true,
                error: null,
                user: null,
            });
    });
});

describe('USER_LOGIN_SUCCESS', () => {
    it('sets user and clears loading/error', () => {
        const state = {
            loading: true,
            error: null,
            user: null,
        };

        Object.freeze(state);

        const payload = {
            user: { name: 'testuser' },
            version: '1.0',
            buildTimestamp: '2024-01-01',
            alertMessage: 'Welcome',
        };

        expect(reducer(state, { type: 'USER_LOGIN_SUCCESS', payload }))
            .toEqual({
                loading: false,
                error: null,
                user: { name: 'testuser' },
                version: '1.0',
                buildTimestamp: '2024-01-01',
                alertMessage: 'Welcome',
            });
    });
});

describe('USER_LOGIN_FAILURE', () => {
    it('sets error and clears user/loading', () => {
        const state = {
            loading: true,
            error: null,
            user: { name: 'old' },
        };

        Object.freeze(state);

        expect(reducer(state, {
            type: 'USER_LOGIN_FAILURE',
            payload: { error: exampleError },
        }))
            .toEqual({
                loading: false,
                error: exampleError,
                user: null,
            });
    });
});
