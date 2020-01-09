import PropTypes from 'prop-types';
import React from 'react';
import EmployeeSelect from '../../../page/layout/EmployeeSelect';
import { DynamicLayoutContext } from '../../context';
import { extractDataValue } from './DynamicReactSelect';

function DynamicEmployeeSelect(props) {
    const { data, setData, ui } = React.useContext(DynamicLayoutContext);
    const [value, setValue] = React.useState(extractDataValue({ data, ...props }));

    const {
        id,
    } = props;

    return React.useMemo(() => {
        const handleChange = (newValue) => {
            setValue(newValue);
            setData({
                [id]: newValue,
            });
        };

        return (
            <EmployeeSelect
                value={value}
                onChange={handleChange}
                translations={ui.translations}
                {...props}
            />
        );
    }, [props, value, setData]);
}

DynamicEmployeeSelect.propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    required: PropTypes.bool,
};

DynamicEmployeeSelect.defaultProps = {
    required: false,
};

export default DynamicEmployeeSelect;
