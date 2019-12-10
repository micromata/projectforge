import PropTypes from 'prop-types';
import React from 'react';
import { ListPageContext } from '../../../../../containers/page/list/ListPageContext';
import AnimatedChevron from '../../../../design/input/chevron/Animated';
import style from './DynamicTable.module.scss';

function DynamicTableHead({ id, title, sortable, }) {
    const { filter, filterHelper } = React.useContext(ListPageContext);
    const sortProperty = Array.findByField(filter.sortProperties, 'property', id);

    const handleHeadClick = () => {
        if (sortable) {
            filterHelper.sort(id, sortProperty);
        }
    };

    return (
        <th onClick={handleHeadClick} className={sortable && style.clickableTableHead}>
            {sortable && <AnimatedChevron direction={(sortProperty || {}).sortOrder} />}
            {title}
        </th>
    );
}

DynamicTableHead.propTypes = {
    id: PropTypes.string.isRequired,
    title: PropTypes.string.isRequired,
    sortable: PropTypes.bool,
};

DynamicTableHead.defaultProps = {
    sortable: false,
};

export default DynamicTableHead;
