import reducer from './authentication';

const exampleError = 'Uncool error message.';

Object.freeze(exampleError);

describe('reducer', () => {
    it('initial state', () => {
        expect(reducer(undefined, {}))
            .toEqual({
                loading: false,
                error: null,
                loggedIn: false,
            });
    });

    it('unknown action', () => {
        const state = {
            loading: true,
            error: null,
            loggedIn: false,
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
            loggedIn: false,
        };

        Object.freeze(state);

        expect(reducer(state, {
            type: 'USER_LOGIN_BEGIN',
        }))
            .toEqual({
                loading: true,
                error: null,
                loggedIn: false,
            });
    });

    it('error state', () => {
        const state = {
            loading: false,
            error: exampleError,
            loggedIn: false,
        };

        Object.freeze(state);

        expect(reducer(state, {
            type: 'USER_LOGIN_BEGIN',
        }))
            .toEqual({
                loading: true,
                error: null,
                loggedIn: false,
            });
    });

    it('loggedIn state', () => {
        const state = {
            loading: false,
            error: null,
            loggedIn: false,
        };

        Object.freeze(state);

        expect(reducer(state, {
            type: 'USER_LOGIN_BEGIN',
        }))
            .toEqual({
                loading: true,
                error: null,
                loggedIn: false,
            });
    });
});

describe('handles USER_LOGIN_SUCCESS', () => {
    it('loading state', () => {
        const state = {
            loading: true,
            error: null,
            loggedIn: false,
        };

        Object.freeze(state);

        expect(reducer(state, {
            type: 'USER_LOGIN_SUCCESS',
        }))
            .toEqual({
                loading: false,
                error: null,
                loggedIn: true,
            });
    });

    it('weird state', () => {
        const state = {
            loading: false,
            error: exampleError,
            loggedIn: true,
        };

        Object.freeze(state);

        expect(reducer(state, {
            type: 'USER_LOGIN_SUCCESS',
        }))
            .toEqual({
                loading: false,
                error: null,
                loggedIn: true,
            });
    });
});

describe('handles USER_LOGIN_FAILURE', () => {
    it('loading state', () => {
        const state = {
            loading: true,
            error: null,
            loggedIn: false,
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
                loggedIn: false,
            });
    });

    it('weird state', () => {
        const state = {
            loading: false,
            error: exampleError,
            loggedIn: true,
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
                loggedIn: false,
            });
    });
});

describe('handles USER_LOGOUT', () => {
    const expectedState = {
        loading: false,
        error: null,
        loggedIn: false,
    };

    Object.freeze(expectedState);

    it('logged in state', () => {
        const state = {
            loading: false,
            error: null,
            loggedIn: true,
        };

        Object.freeze(state);

        expect(reducer(state, {
            type: 'USER_LOGOUT',
        }))
            .toEqual(expectedState);
    });

    it('weird state', () => {
        const state = {
            loading: true,
            error: exampleError,
            loggedIn: false,
        };

        Object.freeze(state);

        expect(reducer(state, {
            type: 'USER_LOGOUT',
        }))
            .toEqual(expectedState);
    });
});
