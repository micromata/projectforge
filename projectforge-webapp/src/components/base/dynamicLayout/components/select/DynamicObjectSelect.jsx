import PropTypes from 'prop-types';
import React from 'react';
import ObjectSelect from '../../../../design/input/autoCompletion/ObjectSelect';
import { DynamicLayoutContext } from '../../context';

function DynamicObjectSelect(
    {
        id,
        ...props
    },
) {
    const { data, setData, ui } = React.useContext(DynamicLayoutContext);

    const handleSelect = completion => setData({ [id]: completion });


    return (
        <ObjectSelect
            id={id}
            onSelect={handleSelect}
            translations={ui.translations}
            value={data[id] || {}}
            {...props}
        />
    );
}

DynamicObjectSelect.propTypes = {
    id: PropTypes.string.isRequired,
};

DynamicObjectSelect.defaultProps = {};

export default DynamicObjectSelect;
