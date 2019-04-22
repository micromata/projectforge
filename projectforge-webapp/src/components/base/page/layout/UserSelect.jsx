import React from 'react';
import PropTypes from 'prop-types';
import { getServiceURL } from '../../../../utilities/rest';
import ReactSelect from './ReactSelect';

class UserSelect extends React.Component {
    constructor(props) {
        super(props);

        this.loadOptions = this.loadOptions.bind(this);
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
    };

    render() {
        const {
            data,
            id,
            changeDataField,
            label,
            translations,
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
                    labelProperty="username"
                    loadOptions={this.loadOptions}
                />
            </React.Fragment>
        );
    }
}

UserSelect.propTypes = {
    changeDataField: PropTypes.func.isRequired,
    data: PropTypes.shape({}).isRequired,
    variables: PropTypes.shape({}).isRequired,
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    translations: PropTypes.shape({}).isRequired,
};

UserSelect.defaultProps = {};

export default UserSelect;
