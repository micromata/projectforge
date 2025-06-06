import PropTypes from 'prop-types';
import React from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import AnimatedChevron from '../../../../design/input/chevron/Animated';
import style from './DynamicTable.module.scss';

function DynamicTableHead(
    {
        id,
        sortable = false,
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

export default DynamicTableHead;
