import React from 'react';
import PropTypes from 'prop-types';
import { getServiceURL } from '../../../../utilities/rest';
import ReactSelect from './ReactSelect';
import style from '../../../design/input/Input.module.scss';

class UserSelect extends React.Component {
    constructor(props) {
        super(props);

        this.loadOptions = this.loadOptions.bind(this);
    }

    static getOptionLabel(option) {
        if (!option) {
            return '';
        }
        return `${option.fullname} (${option.username})`;
    }

    loadOptions(inputValue, callback) {
        fetch(getServiceURL('user/aco',
            { search: inputValue }), {
            method: 'GET',
            credentials: 'include',
            headers: {
                Accept: 'application/json',
            },
        })
            .then(response => response.json())
            .then((json) => {
                callback(json);
            })
            .catch(() => this.setState({}));
    }

    render() {
        const {
            data,
            id,
            changeDataField,
            label,
            translations,
            required,
        } = this.props;
        return (
            <React.Fragment>
                <ReactSelect
                    label={label}
                    data={data}
                    id={id}
                    values={[]}
                    changeDataField={changeDataField}
                    translations={translations}
                    valueProperty="id"
                    labelProperty="fullname"
                    loadOptions={this.loadOptions}
                    isRequired={required}
                    getOptionLabel={UserSelect.getOptionLabel}
                    className={style.userSelect}
                />
            </React.Fragment>
        );
    }
}

UserSelect.propTypes = {
    changeDataField: PropTypes.func.isRequired,
    data: PropTypes.shape({}).isRequired,
    required: PropTypes.bool,
    variables: PropTypes.shape({}).isRequired,
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    translations: PropTypes.shape({}).isRequired,
};

UserSelect.defaultProps = {
    required: undefined,
};

export default UserSelect;
