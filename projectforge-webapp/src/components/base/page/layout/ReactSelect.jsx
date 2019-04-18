import React from 'react';
import Select from 'react-select';
import makeAnimated from 'react-select/lib/animated';
import PropTypes from "prop-types";
import {dataPropType} from "../../../../utilities/propTypes";
import style from "../../../design/input/Input.module.scss";
import AdditionalLabel from "../../../design/input/AdditionalLabel";
import {getTranslation} from "../../../../utilities/layout";

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
        return (<React.Fragment>
                <span className={style.text}>{label}</span>
                <Select
                    //closeMenuOnSelect={false}
                    components={makeAnimated()}
                    defaultValue={data[id]}
                    isMulti={this.props.isMulti}
                    options={values}
                    getOptionValue={(option) => (option[this.props.valueProperty])}
                    getOptionLabel={(option) => (option[this.props.labelProperty])}
                    onChange={this.setSelected}
                    placeholder={this.props.translations['select.placeholder']}
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
    values: PropTypes.arrayOf(PropTypes.object).isRequired,
    valueProperty : PropTypes.string,
    labelProperty : PropTypes.string,
    isMulti: PropTypes.bool,
};

ReactSelect.defaultProps = {
    valueProperty: 'value',
    labelProperty: 'label',
    isMulti: false
};
export default ReactSelect;
