import React from 'react';
import Select from 'react-select';
import makeAnimated from 'react-select/lib/animated';
import PropTypes from "prop-types";
import {dataPropType} from "../../../../utilities/propTypes";

class ReactSelect extends React.Component {
    constructor(props) {
        super(props);

        this.setSelected = this.setSelected.bind(this);
    }

    setSelected(newValue) {
        const { id, changeDataField } = this.props;

        changeDataField(id, newValue);
    }

    render() {
        const {
            label,
            id,
            values,
            data,
        } = this.props;

        return (
            <Select
                closeMenuOnSelect={false}
                components={makeAnimated()}
                //defaultValue={}
                isMulti
                //options={colourOptions}
            />
        );
    }
}

ReactSelect.propTypes = {
    changeDataField: PropTypes.func.isRequired,
    data: dataPropType.isRequired,
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    values: PropTypes.arrayOf(PropTypes.shape({
        title: PropTypes.string,
        value: PropTypes.string,
    })).isRequired,
};

export default ReactSelect;
