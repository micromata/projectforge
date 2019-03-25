import PropTypes from 'prop-types';
import React from 'react';
import { dataPropType } from '../../../../utilities/propTypes';
import { Select } from '../../../design';

class LayoutSelect extends React.Component {
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
                id={id}
                label={label}
                options={values}
                selected={data[id] || values[0].value}
                setSelected={this.setSelected}
            />
        );
    }
}

LayoutSelect.propTypes = {
    label: PropTypes.string.isRequired,
    id: PropTypes.string.isRequired,
    values: PropTypes.arrayOf(PropTypes.shape({
        value: PropTypes.string,
        title: PropTypes.string,
    })).isRequired,
    data: dataPropType.isRequired,
    changeDataField: PropTypes.func.isRequired,
};

export default LayoutSelect;
