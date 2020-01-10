import PropTypes from 'prop-types';
import React from 'react';
import ObjectAutoCompletion from '../../../../design/input/autoCompletion/ObjectAutoCompletion';
import { DynamicLayoutContext } from '../../context';

function DynamicQuickSelect(
    {
        dataType,
        id,
        label,
    },
) {
    const { data, setData } = React.useContext(DynamicLayoutContext);

    const handleSelect = completion => setData({ [id]: completion });

    return (
        <ObjectAutoCompletion
            inputId={id}
            inputProps={{ label }}
            onSelect={handleSelect}
            url={`${dataType.toLowerCase()}/autosearch?search=:search`}
            value={data[id] || {}}
        />
    );
}

DynamicQuickSelect.propTypes = {
    dataType: PropTypes.oneOf(['USER', 'EMPLOYEE']).isRequired,
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
};

DynamicQuickSelect.defaultProps = {};

export default DynamicQuickSelect;
