import PropTypes from 'prop-types';
import React from 'react';
import DynamicCheckbox
    from '../../../components/base/dynamicLayout/components/input/DynamicCheckbox';
import { DynamicLayoutContext } from '../../../components/base/dynamicLayout/context';
import CheckBox from '../../../components/design/input/CheckBox';
import ValidationManager from '../../../components/design/input/ValidationManager';

// The checkbox for the SearchFilter that consumes the filter data from the DynamicLayout
function DynamicFilterCheckbox({ id, label }) {
    const { filter, setFilter } = React.useContext(DynamicLayoutContext);

    // Redirect to DynamicCheckbox if FilterCheckbox wasn't unregistered in time.
    if (!filter) {
        return <DynamicCheckbox label={label} id={id} />;
    }

    return React.useMemo(() => {
        const handleCheckboxChange = ({ target }) => setFilter({ [id]: target.checked });

        return (
            <ValidationManager>
                <CheckBox
                    id={id}
                    label={label}
                    checked={filter[id] || false}
                    onChange={handleCheckboxChange}
                />
            </ValidationManager>
        );
    }, [filter[id]]);
}

DynamicFilterCheckbox.propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
};

DynamicFilterCheckbox.defaultProps = {};

export default DynamicFilterCheckbox;
