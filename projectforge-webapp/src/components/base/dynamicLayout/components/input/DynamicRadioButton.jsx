import PropTypes from 'prop-types';
import React from 'react';
import RadioButton from '../../../../design/input/RadioButton';
import ValidationManager from '../../../../design/input/ValidationManager';
import { DynamicLayoutContext } from '../../context';

function DynamicRadioButton({ id, name, label }) {
    const { data, setData } = React.useContext(DynamicLayoutContext);

    return React.useMemo(() => {
        const handleCheckboxChange = ({ target }) => setData({ [id]: target.checked });

        return (
            <ValidationManager>
                <RadioButton
                    id={id}
                    name={name}
                    label={label}
                    checked={data[id] || false}
                    onChange={handleCheckboxChange}
                />
            </ValidationManager>
        );
    }, [data[id]]);
}

DynamicRadioButton.propTypes = {
    id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
};

DynamicRadioButton.defaultProps = {};

export default DynamicRadioButton;
