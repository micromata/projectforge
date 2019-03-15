import configureMockStore from 'redux-mock-store';
import thunk from 'redux-thunk';
import {
    allFieldsSet,
    changeField,
    EDIT_PAGE_ALL_FIELDS_SET,
    EDIT_PAGE_FIELD_CHANGE,
    fieldChanged,
    setAllFields,
} from './editPage';

describe('change field', () => {
    const mockStore = configureMockStore([thunk]);

    it('should create EDIT_PAGE_FIELD_CHANGE action', () => {
        const expectedAction = {
            type: EDIT_PAGE_FIELD_CHANGE,
            payload: {
                id: 'title',
                newValue: 'Lorem Ipsum',
            },
        };

        expect(fieldChanged('title', 'Lorem Ipsum'))
            .toEqual(expectedAction);
    });

    it('should dispatch an action to change the fields value', () => {
        const expectedActions = [
            {
                type: EDIT_PAGE_FIELD_CHANGE,
                payload: {
                    id: 'title',
                    newValue: 'Lorem Ipsum',
                },
            },
        ];

        const store = mockStore({});

        store.dispatch(changeField('title', 'Lorem Ipsum'));

        expect(store.getActions())
            .toEqual(expectedActions);
    });
});

describe('set all fields', () => {
    const mockStore = configureMockStore([thunk]);

    it('should create EDIT_PAGE_ALL_FIELDS_SET', () => {
        const expectedAction = {
            type: EDIT_PAGE_ALL_FIELDS_SET,
            payload: {
                values: {
                    title: 'Lorem Ipsum',
                },
            },
        };

        const values = {
            title: 'Lorem Ipsum',
        };

        Object.freeze(values);

        expect(allFieldsSet({
            ...values,
        }))
            .toEqual(expectedAction);
    });

    it('should dispatch an action to change all values', () => {
        const expectedActions = [
            {
                type: EDIT_PAGE_ALL_FIELDS_SET,
                payload: {
                    values: {
                        title: 'Lorem Ipsum',
                    },
                },
            },
        ];

        const store = mockStore({});

        store.dispatch(setAllFields({ title: 'Lorem Ipsum' }));

        expect(store.getActions())
            .toEqual(expectedActions);
    });
});
