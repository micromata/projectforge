import PropTypes from 'prop-types';
import React from 'react';
import RadioButton from '../../../../design/input/RadioButton';
import { DynamicLayoutContext } from '../../context';
import DynamicValidationManager from './DynamicValidationManager';

function DynamicRadioButton({ id, name, label }) {
    const { data, setData } = React.useContext(DynamicLayoutContext);

    return React.useMemo(() => {
        const handleCheckboxChange = ({ target }) => setData({ [id]: target.checked });

        return (
            <DynamicValidationManager id={id}>
                <RadioButton
                    id={id}
                    name={name}
                    label={label}
                    checked={data[id] || false}
                    onChange={handleCheckboxChange}
                />
            </DynamicValidationManager>
        );
    }, [data[id], setData]);
}

DynamicRadioButton.propTypes = {
    id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
};

DynamicRadioButton.defaultProps = {};

export default DynamicRadioButton;
