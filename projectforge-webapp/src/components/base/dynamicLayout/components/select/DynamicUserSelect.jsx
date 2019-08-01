import PropTypes from 'prop-types';
import React from 'react';
import UserSelect from '../../../page/layout/UserSelect';
import { DynamicLayoutContext } from '../../context';
import { extractDataValue } from './DynamicReactSelect';

function DynamicUserSelect(props) {
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
            <UserSelect
                value={value}
                onChange={handleChange}
                translations={ui.translations}
                {...props}
            />
        );
    }, [props, value, setData]);
}

DynamicUserSelect.propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    required: PropTypes.bool,
};

DynamicUserSelect.defaultProps = {
    required: false,
};

export default DynamicUserSelect;
