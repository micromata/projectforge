import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { sortList } from '../../../../../actions/list/filter';
import AnimatedChevron from '../../../../design/input/chevron/Animated';
import style from './DynamicTable.module.scss';

function DynamicTableHead(
    {
        handleHeadClick,
        sortable,
        sortProperty,
        title,
    },
) {
    // TODO DISPLAY SORT ORDER.

    return (
        <th onClick={handleHeadClick} className={sortable ? style.clickableTableHead : ''}>
            {sortable && <AnimatedChevron direction={(sortProperty || {}).sortOrder} />}
            {title}
        </th>
    );
}

DynamicTableHead.propTypes = {
    // Prop is used by redux
    // eslint-disable-next-line react/no-unused-prop-types
    id: PropTypes.string.isRequired,
    handleHeadClick: PropTypes.func.isRequired,
    title: PropTypes.string.isRequired,
    sortable: PropTypes.bool,
    sortProperty: PropTypes.string,
};

DynamicTableHead.defaultProps = {
    sortable: false,
    sortProperty: undefined,
};

const mapStateToProps = ({ list }, { id }) => ({
    sortProperty: Array.findByField(
        list.categories[list.currentCategory].filter.sortProperties,
        'property',
        id,
    ),
});

const actions = (dispatch, { id, sortable, sortProperty }) => ({
    handleHeadClick: () => {
        if (sortable) {
            dispatch(sortList(id, sortProperty));
        }
    },
});

export default connect(mapStateToProps, actions)(DynamicTableHead);
