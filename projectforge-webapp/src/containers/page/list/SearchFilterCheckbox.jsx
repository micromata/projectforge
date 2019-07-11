import PropTypes from 'prop-types';
import React from 'react';
import DynamicCheckbox
    from '../../../components/base/dynamicLayout/components/input/DynamicCheckbox';
import CheckBox from '../../../components/design/input/CheckBox';
import { ListPageContext } from './ListPageContext';

// The checkbox for the SearchFilter that consumes the filter data from the DynamicLayout
function SearchFilterCheckbox({ id, label }) {
    const { filter, filterHelper } = React.useContext(ListPageContext);

    // Redirect to DynamicCheckbox if FilterCheckbox wasn't unregistered in time.
    if (!filter) {
        return <DynamicCheckbox label={label} id={id} />;
    }

    return React.useMemo(() => {
        const handleCheckboxChange = ({ target }) => filterHelper
            .setSearchFilter(id, target.checked);

        return (
            <CheckBox
                id={id}
                label={label}
                checked={filter.searchFilter[id] || false}
                onChange={handleCheckboxChange}
            />
        );
    }, [filter]);
}

SearchFilterCheckbox.propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
};

SearchFilterCheckbox.defaultProps = {};

export default SearchFilterCheckbox;
