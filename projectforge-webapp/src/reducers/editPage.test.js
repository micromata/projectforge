import reducer from './editPage';

describe('reducer', () => {
    it('initial state', () => {
        expect(reducer(undefined, {}))
            .toEqual({
                values: {},
            });
    });

    it('unknown action', () => {
        const state = {
            values: {
                a: 'A',
            },
        };

        Object.freeze(state);

        expect(reducer(state, {
            type: 'UNKNOWN_ACTION',
        }))
            .toEqual(state);
    });
});

describe('handles EDIT_PAGE_FIELD_CHANGE', () => {
    it('initial state', () => {
        const state = {
            values: {},
        };

        Object.freeze(state);

        expect(reducer(state, {
            type: 'EDIT_PAGE_FIELD_CHANGE',
            payload: {
                id: 'title',
                newValue: 'Lorem Ipsum',
            },
        }))
            .toEqual({
                values: {
                    title: 'Lorem Ipsum',
                },
            });
    });

    it('state with other values', () => {
        const state = {
            values: {
                a: 'A',
            },
        };

        Object.freeze(state);

        expect(reducer(state, {
            type: 'EDIT_PAGE_FIELD_CHANGE',
            payload: {
                id: 'title',
                newValue: 'Lorem Ipsum',
            },
        }))
            .toEqual({
                values: {
                    a: 'A',
                    title: 'Lorem Ipsum',
                },
            });
    });

    it('state with value already in it', () => {
        const state = {
            values: {
                a: 'A',
                title: 'Lorem Ipsum dolor sit.',
            },
        };

        Object.freeze(state);

        expect(reducer(state, {
            type: 'EDIT_PAGE_FIELD_CHANGE',
            payload: {
                id: 'title',
                newValue: 'Lorem Ipsum',
            },
        }))
            .toEqual({
                values: {
                    a: 'A',
                    title: 'Lorem Ipsum',
                },
            });
    });
});

describe('handles EDIT_PAGE_ALL_FIELDS_SET', () => {
    it('initial state', () => {
        const state = {
            values: {},
        };

        Object.freeze(state);

        expect(reducer(state, {
            type: 'EDIT_PAGE_ALL_FIELDS_SET',
            payload: {
                values: {
                    title: 'Lorem Ipsum',
                },
            },
        }))
            .toEqual({
                values: {
                    title: 'Lorem Ipsum',
                },
            });
    });

    it('state with values', () => {
        const state = {
            values: {
                a: 'A',
            },
        };

        Object.freeze(state);

        expect(reducer(state, {
            type: 'EDIT_PAGE_ALL_FIELDS_SET',
            payload: {
                values: {
                    title: 'Lorem Ipsum',
                },
            },
        }))
            .toEqual({
                values: {
                    title: 'Lorem Ipsum',
                },
            });
    });

    it('state with values, action to clear', () => {
        const state = {
            values: {
                title: 'Lorem Ipsum',
            },
        };

        Object.freeze(state);

        expect(reducer(state, {
            type: 'EDIT_PAGE_ALL_FIELDS_SET',
            payload: {
                values: {},
            },
        }))
            .toEqual({
                values: {},
            });
    });
});
