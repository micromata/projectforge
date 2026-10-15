import reducer from './authentication';

const exampleError = 'Uncool error message.';

Object.freeze(exampleError);

const exampleUser = {
    userId: 42,
    username: 'kai',
};

Object.freeze(exampleUser);

describe('reducer', () => {
    it('initial state', () => {
        expect(reducer(undefined, {}))
            .toEqual({
                loading: true,
                error: null,
                user: null,
            });
    });

    it('unknown action', () => {
        const state = {
            loading: true,
            error: null,
            user: null,
        };

        Object.freeze(state);

        expect(reducer(state, {
            type: 'UNKNOWN_ACTION',
        }))
            .toEqual(state);
    });
});

describe('handles USER_LOGIN_BEGIN', () => {
    it('fresh state', () => {
        const state = {
            loading: false,
            error: null,
            user: null,
        };

        Object.freeze(state);

        expect(reducer(state, {
            type: 'USER_LOGIN_BEGIN',
        }))
            .toEqual({
                loading: true,
                error: null,
                user: null,
            });
    });

    it('error state', () => {
        const state = {
            loading: false,
            error: exampleError,
            user: null,
        };

        Object.freeze(state);

        expect(reducer(state, {
            type: 'USER_LOGIN_BEGIN',
        }))
            .toEqual({
                loading: true,
                error: null,
                user: null,
            });
    });

    it('logged in state', () => {
        const state = {
            loading: false,
            error: null,
            user: exampleUser,
        };

        Object.freeze(state);

        expect(reducer(state, {
            type: 'USER_LOGIN_BEGIN',
        }))
            .toEqual({
                loading: true,
                error: null,
                user: null,
            });
    });
});

describe('handles USER_LOGIN_SUCCESS', () => {
    it('loading state', () => {
        const state = {
            loading: true,
            error: null,
            user: null,
        };

        Object.freeze(state);

        expect(reducer(state, {
            type: 'USER_LOGIN_SUCCESS',
            payload: {
                user: exampleUser,
                version: '1.0.0',
                buildTimestamp: '2026-08-02',
                alertMessage: null,
            },
        }))
            .toEqual({
                loading: false,
                error: null,
                user: exampleUser,
                version: '1.0.0',
                buildTimestamp: '2026-08-02',
                alertMessage: null,
            });
    });

    it('weird state', () => {
        const state = {
            loading: false,
            error: exampleError,
            user: exampleUser,
        };

        Object.freeze(state);

        expect(reducer(state, {
            type: 'USER_LOGIN_SUCCESS',
            payload: {
                user: exampleUser,
                version: '1.0.0',
                buildTimestamp: '2026-08-02',
                alertMessage: null,
            },
        }))
            .toEqual({
                loading: false,
                error: null,
                user: exampleUser,
                version: '1.0.0',
                buildTimestamp: '2026-08-02',
                alertMessage: null,
            });
    });
});

describe('handles USER_LOGIN_FAILURE', () => {
    it('loading state', () => {
        const state = {
            loading: true,
            error: null,
            user: null,
        };

        Object.freeze(state);

        expect(reducer(state, {
            type: 'USER_LOGIN_FAILURE',
            payload: {
                error: exampleError,
            },
        }))
            .toEqual({
                loading: false,
                error: exampleError,
                user: null,
            });
    });

    it('weird state', () => {
        const state = {
            loading: false,
            error: null,
            user: exampleUser,
        };

        Object.freeze(state);

        expect(reducer(state, {
            type: 'USER_LOGIN_FAILURE',
            payload: {
                error: exampleError,
            },
        }))
            .toEqual({
                loading: false,
                error: exampleError,
                user: null,
            });
    });
});
