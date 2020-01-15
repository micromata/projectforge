import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { sortList } from '../../../../../actions/list/filter';
import AnimatedChevron from '../../../../design/input/chevron/Animated';
import style from './DynamicTable.module.scss';

function DynamicTableHead(
    {
        id,
        sortable,
        direction,
        dispatchSort,
        title,
    },
) {
    const handleHeadClick = () => {
        if (sortable) {
            dispatchSort(id, direction);
        }
    };

    return (
        <th onClick={handleHeadClick} className={sortable ? style.clickableTableHead : ''}>
            {title}
            {sortable && <AnimatedChevron direction={direction} />}
        </th>
    );
}

DynamicTableHead.propTypes = {
    id: PropTypes.string.isRequired,
    dispatchSort: PropTypes.func.isRequired,
    title: PropTypes.string.isRequired,
    direction: PropTypes.string,
    sortable: PropTypes.bool,
};

DynamicTableHead.defaultProps = {
    sortable: false,
    direction: undefined,
};

const mapStateToProps = ({ list }, { id }) => ({
    direction: (Array.findByField(
        list.categories[list.currentCategory].filter.sortProperties,
        'property',
        id,
    ) || {}).sortOrder,
});

const actions = {
    dispatchSort: sortList,
};

export default connect(mapStateToProps, actions)(DynamicTableHead);
