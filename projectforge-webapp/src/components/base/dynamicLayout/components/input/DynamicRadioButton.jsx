import PropTypes from 'prop-types';
import React from 'react';
import RadioButton from '../../../../design/input/RadioButton';
import { DynamicLayoutContext } from '../../context';
import DynamicValidationManager from './DynamicValidationManager';

function DynamicRadioButton(
    {
        id,
        type,
        value,
        ...props
    },
) {
    const { data, setData, ui } = React.useContext(DynamicLayoutContext);

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
                    id={`${ui.uid}-${id}-${value}`}
                    checked={checked}
                    onChange={handleCheckboxChange}
                    // eslint-disable-next-line react/jsx-props-no-spreading
                    {...props}
                />
            </DynamicValidationManager>
        );
    }, [checked, setData, id, type, value, props]);
}

DynamicRadioButton.propTypes = {
    id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    value: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
};

DynamicRadioButton.defaultProps = {};

export default DynamicRadioButton;
