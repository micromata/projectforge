import PropTypes from 'prop-types';
import React from 'react';
import ReactCreatableSelect from '../../../../design/react-select/ReactCreatableSelect';
import { DynamicLayoutContext } from '../../context';
import DynamicValidationManager from '../input/DynamicValidationManager';

export const extractDataValue = (
    {
        data,
        id,
    },
) => {
    const dataValue = Object.getByString(data, id);

    return dataValue;
};

function DynamicReactSelect({
    id,
    required = false,
    ...restProps
}) {
    const { data, setData, ui } = React.useContext(DynamicLayoutContext);
    const props = {
        id,
        required,
        ...restProps,
    };

    const valueStringArray = extractDataValue({ data, ...props });
    let value;
    if (valueStringArray) {
        value = valueStringArray.map((x) => ({ label: x, value: x }));
    }
    return React.useMemo(() => {
        const onChange = (newValue) => {
            let newValueArray;
            if (newValue) {
                newValueArray = newValue.map((x) => (x.label));
            }
            setData({ [id]: newValueArray });
        };

        return (
            <DynamicValidationManager id={id}>
                <ReactCreatableSelect
                    className="invalid"
                    onChange={onChange}
                    translations={ui.translations}
                    {...props}
                    value={value}
                />
            </DynamicValidationManager>
        );
    }, [data[id], value, setData]);
}

DynamicReactSelect.propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    additionalLabel: PropTypes.string,
    required: PropTypes.bool,
};

export default DynamicReactSelect;
