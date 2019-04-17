import React from 'react';
import Select from 'react-select';
import makeAnimated from 'react-select/lib/animated';
import PropTypes from "prop-types";
import {dataPropType} from "../../../../utilities/propTypes";
import style from "../../../design/input/Input.module.scss";
import AdditionalLabel from "../../../design/input/AdditionalLabel";

class ReactSelect extends React.Component {
    constructor(props) {
        super(props);

        this.setSelected = this.setSelected.bind(this);
    }

    setSelected(newValue) {
        const {id, changeDataField} = this.props;

        changeDataField(id, newValue);
    }

    render() {
        const {
            label,
            additionalLabel,
            id,
            values,
            data,
        } = this.props;
        console.log(id, data)
        return (<React.Fragment>
                <span className={style.text}>{label}</span>
                <Select
                    //closeMenuOnSelect={false}
                    components={makeAnimated()}
                    defaultValue={data[id]}
                    isMulti
                    options={values}
                />
                <AdditionalLabel title={additionalLabel}/>
            </React.Fragment>
        );
    }
}

ReactSelect.propTypes = {
    changeDataField: PropTypes.func.isRequired,
    data: dataPropType.isRequired,
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    values: PropTypes.arrayOf(PropTypes.shape({
        label: PropTypes.string,
        value: PropTypes.string,
    })).isRequired,
};

export default ReactSelect;
