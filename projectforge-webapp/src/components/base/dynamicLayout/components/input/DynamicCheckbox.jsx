import PropTypes from 'prop-types';
import React from 'react';
import CheckBox from '../../../../design/input/CheckBox';
import { DynamicLayoutContext } from '../../context';
import DynamicValidationManager from './DynamicValidationManager';

function DynamicCheckbox({ id, label, ...props }) {
    const { data, setData } = React.useContext(DynamicLayoutContext);

    const value = Object.getByString(data, id) || false;

    return React.useMemo(() => {
        const handleCheckboxChange = ({ target }) => setData({ [id]: target.checked });

        return (
            <DynamicValidationManager id={id}>
                <CheckBox
                    id={id}
                    label={label}
                    checked={value}
                    onChange={handleCheckboxChange}
                    {...props}
                />
            </DynamicValidationManager>
        );
    }, [value, setData]);
}

DynamicCheckbox.propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
};

DynamicCheckbox.defaultProps = {};

export default DynamicCheckbox;
