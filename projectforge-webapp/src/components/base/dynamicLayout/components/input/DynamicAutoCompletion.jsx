import PropTypes from 'prop-types';
import React from 'react';
import TextAutoCompletion from '../../../../design/input/autoCompletion/TextAutoCompletion';
import { DynamicLayoutContext } from '../../context';
import DynamicValidationManager from './DynamicValidationManager';

function DynamicAutoCompletion(
    {
        id,
        url,
        label,
    },
) {
    const { data, setData } = React.useContext(DynamicLayoutContext);

    return (
        <DynamicValidationManager id={id}>
            <TextAutoCompletion
                inputId={id}
                inputProps={{ label }}
                onChange={completion => setData({ [id]: completion })}
                url={url}
                value={data[id]}
            />
        </DynamicValidationManager>
    );
}

DynamicAutoCompletion.propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    url: PropTypes.string.isRequired,
};

DynamicAutoCompletion.defaultProps = {};

export default DynamicAutoCompletion;
