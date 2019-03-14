import reducer from './authentication';

const exampleUser = {
    id: 123,
    token: 'ABCDEF',
};
const exampleError = 'Uncool error message.';

Object.freeze(exampleUser);
Object.freeze(exampleError);

describe('reducer', () => {
    it('initial state', () => {
        expect(reducer(undefined, {}))
            .toEqual({
                loading: false,
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

    it('loggedIn state', () => {
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
                userId: exampleUser.id,
                authenticationToken: exampleUser.token,
            },
        }))
            .toEqual({
                loading: false,
                error: null,
                user: exampleUser,
            });
    });

    it('weird state', () => {
        const state = {
            loading: false,
            error: exampleError,
            user: {
                not: 'even',
                a: 'user',
            },
        };

        Object.freeze(state);

        expect(reducer(state, {
            type: 'USER_LOGIN_SUCCESS',
            payload: {
                userId: exampleUser.id,
                authenticationToken: exampleUser.token,
            },
        }))
            .toEqual({
                loading: false,
                error: null,
                user: exampleUser,
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
            error: exampleError,
            user: {
                not: 'even',
                a: 'user',
            },
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

describe('handles USER_LOGOUT', () => {
    const expectedState = {
        loading: false,
        error: null,
        user: null,
    };

    Object.freeze(expectedState);

    it('logged in state', () => {
        const state = {
            loading: false,
            error: null,
            user: exampleUser,
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
            user: null,
        };

        Object.freeze(state);

        expect(reducer(state, {
            type: 'USER_LOGOUT',
        }))
            .toEqual(expectedState);
    });
});
