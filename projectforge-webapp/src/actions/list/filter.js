export const LIST_FILTER_SORT = 'LIST_FILTER_SORT';

const sort = (column, sortProperty) => ({
    type: LIST_FILTER_SORT,
    payload: {
        column,
        sortProperty,
    },
});

export const sortList = (column, sortProperty) => (dispatch) => {
    let newSortProperty = sortProperty;

    if (!newSortProperty) {
        newSortProperty = {
            property: column,
            sortOrder: 'ASCENDING',
        };
    } else if (sortProperty.sortOrder === 'DESCENDING') {
        newSortProperty = undefined;
    } else {
        newSortProperty.sortOrder = 'DESCENDING';
    }

    dispatch(sort(column, newSortProperty));
};
