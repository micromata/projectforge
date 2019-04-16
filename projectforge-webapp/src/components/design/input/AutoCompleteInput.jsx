import PropTypes from 'prop-types';
import React, { Component } from 'react';
import { getServiceURL, handleHTTPErrors } from '../../../utilities/rest';
import MultiSelect from './MultiSelect';

class AutoComplete extends Component {
    constructor(props) {
        super(props);

        this.state = {
            autoCompletion: [],
        };

        this.fetchAutoCompletion = this.fetchAutoCompletion.bind(this);
        this.handleInputChange = this.handleInputChange.bind(this);

        if (props.value.length >= props.minChars) {
            this.fetchAutoCompletion();
        }
    }

    fetchAutoCompletion() {
        const { autoCompletionUrl, value } = this.props;

        fetch(
            getServiceURL(`${autoCompletionUrl}${value}`),
            {
                method: 'GET',
                credentials: 'include',
            },
        )
            .then(handleHTTPErrors)
            .then(response => response.json())
            .then(autoCompletion => this.setState({ autoCompletion }))
            .catch(() => this.setState({ autoCompletion: [] }));
    }

    handleInputChange(_, newValue) {
        const {
            id,
            minChars,
            onChange,
            value,
        } = this.props;

        onChange(id, newValue);

        if (value.length < minChars && newValue.length >= minChars) {
            console.log('auto completion');
            this.fetchAutoCompletion();
        }
    }

    render() {
        const { value, ...props } = this.props;
        const { autoCompletion } = this.state;

        return (
            <MultiSelect
                {...props}
                autoComplete={autoCompletion}
                setValue={this.handleInputChange}
                value={{ searchString: value }}
            />
        );
    }
}

AutoComplete.propTypes = {
    autoCompletionUrl: PropTypes.string.isRequired,
    id: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    value: PropTypes.string.isRequired,
    minChars: PropTypes.number,
};

AutoComplete.defaultProps = {
    minChars: 2,
};

export default AutoComplete;
