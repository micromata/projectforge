import PropTypes from 'prop-types';
import React from 'react';
import ReactSelect from '../../../../design/ReactSelect';
import { DynamicLayoutContext } from '../../context';

export const extractDataValue = (
    {
        data,
        id,
        isMulti,
        valueProperty,
        values,
    },
) => {
    let dataValue = Object.getByString(data, id);
    if (!isMulti && dataValue && values && values.length && values.length > 0) {
        // For react-select it seems to be important, that the current selected element matches
        // its value of the values list.
        const valueOfArray = (typeof dataValue === 'object') ? dataValue[valueProperty] : dataValue;
        dataValue = values.find(it => it[valueProperty] === valueOfArray);
    }
    return dataValue;
};

function DynamicReactSelect(props) {
    const { data, setData, ui } = React.useContext(DynamicLayoutContext);
    const { id } = props;
    const [value, setValue] = React.useState(extractDataValue({ data, ...props }));

    const onChange = (newValue) => {
        setValue(newValue);
        setData({ [id]: newValue });
    };

    return (
        <ReactSelect
            onChange={onChange}
            translations={ui.translations}
            value={value}
            {...props}
        />
    );
}

DynamicReactSelect.propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    additionalLabel: PropTypes.string,
    values: PropTypes.arrayOf(PropTypes.object).isRequired,
    valueProperty: PropTypes.string,
    labelProperty: PropTypes.string,
    isMulti: PropTypes.bool,
    isRequired: PropTypes.bool,
    loadOptions: PropTypes.func,
    getOptionLabel: PropTypes.func,
    className: PropTypes.string,
};

DynamicReactSelect.defaultProps = {
    additionalLabel: undefined,
    valueProperty: 'value',
    labelProperty: 'label',
    isMulti: false,
    isRequired: false,
    loadOptions: undefined,
    getOptionLabel: undefined,
    className: undefined,
};

export default DynamicReactSelect;
