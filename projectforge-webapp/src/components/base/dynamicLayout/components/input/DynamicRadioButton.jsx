import PropTypes from 'prop-types';
import React from 'react';
import RadioButton from '../../../../design/input/RadioButton';
import { DynamicLayoutContext } from '../../context';
import DynamicValidationManager from './DynamicValidationManager';

function DynamicRadioButton(
    {
        id,
        name,
        value,
        label,
    },
) {
    const { data, setData } = React.useContext(DynamicLayoutContext);

    const checked = Object.getByString(data, id) === value;

    return React.useMemo(() => {
        const handleCheckboxChange = ({ target }) => {
            if (target.checked) {
                setData({ [id]: value });
            }
        };
        return (
            <DynamicValidationManager id={id}>
                <RadioButton
                    id={`${id}-${value}`}
                    name={name}
                    label={label}
                    checked={checked}
                    onChange={handleCheckboxChange}
                />
            </DynamicValidationManager>
        );
    }, [checked, setData]);
}

DynamicRadioButton.propTypes = {
    id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    value: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
};

DynamicRadioButton.defaultProps = {};

export default DynamicRadioButton;
