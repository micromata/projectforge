import { loadList } from './index';

export const LIST_FILTER_SORT = 'LIST_FILTER_SORT';

const sort = (column, sortProperty, category) => ({
    type: LIST_FILTER_SORT,
    payload: {
        category,
        column,
        sortProperty,
    },
});

export const sortList = (property, sortOrder) => (dispatch, getState) => {
    let sortProperty = {
        property,
        sortOrder,
    };

    if (!sortOrder) {
        sortProperty.sortOrder = 'ASCENDING';
    } else if (sortOrder === 'DESCENDING') {
        sortProperty = undefined;
    } else {
        sortProperty.sortOrder = 'DESCENDING';
    }

    const category = getState().list.currentCategory;

    dispatch(sort(property, sortProperty, category));
    loadList(category)(dispatch, getState);
};
