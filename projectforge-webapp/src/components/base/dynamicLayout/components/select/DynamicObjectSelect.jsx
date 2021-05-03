import PropTypes from 'prop-types';
import React from 'react';
import ObjectSelect from '../../../../design/input/autoCompletion/ObjectSelect';
import { DynamicLayoutContext } from '../../context';
import DynamicValidationManager from '../input/DynamicValidationManager';

function DynamicObjectSelect(
    {
        id,
        ...props
    },
) {
    const { data, setData, ui } = React.useContext(DynamicLayoutContext);

    const handleSelect = (completion) => setData({ [id]: completion });

    return (
        <DynamicValidationManager id={id}>
            <ObjectSelect
                id={`${ui.uid}-${id}`}
                onSelect={handleSelect}
                translations={ui.translations}
                value={data[id] || {}}
                {...props}
            />
        </DynamicValidationManager>
    );
}

DynamicObjectSelect.propTypes = {
    id: PropTypes.string.isRequired,
};

DynamicObjectSelect.defaultProps = {};

export default DynamicObjectSelect;
