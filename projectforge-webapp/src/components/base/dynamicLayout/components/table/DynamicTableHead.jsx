import PropTypes from 'prop-types';
import React from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
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
        titleIcon,
    },
) {
    const handleHeadClick = () => {
        if (sortable) {
            dispatchSort(id, direction);
        }
    };

    const head = titleIcon ? <FontAwesomeIcon icon={titleIcon} /> : title;

    return (
        <th onClick={handleHeadClick} className={sortable ? style.clickableTableHead : ''}>
            {head}
            {sortable && <AnimatedChevron direction={direction} />}
        </th>
    );
}

DynamicTableHead.propTypes = {
    id: PropTypes.string.isRequired,
    dispatchSort: PropTypes.func.isRequired,
    title: PropTypes.string,
    titleIcon: PropTypes.arrayOf(PropTypes.string),
    direction: PropTypes.string,
    sortable: PropTypes.bool,
};

DynamicTableHead.defaultProps = {
    sortable: false,
    direction: undefined,
    title: undefined,
    titleIcon: undefined,
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
