import PropTypes from 'prop-types';
import React, { Component } from 'react';
import InputBase from './base';
import BadgePart from './base/BadgePart';
import DropdownSelectContent from './base/dropdown/SelectContent';
import InputPart from './base/InputPart';

class MultiSelect extends Component {
    constructor(props) {
        super(props);

        const { value } = this.props;

        this.state = { cursorPos: value.searchString.length };

        this.handleBadgeClick = this.handleBadgeClick.bind(this);
        this.handleBadgeDelete = this.handleBadgeDelete.bind(this);
        this.handleInputChange = this.handleInputChange.bind(this);
        this.handleInputKey = this.handleInputKey.bind(this);
        this.getFilteredValueKeys = this.getFilteredValueKeys.bind(this);
        this.editValue = this.editValue.bind(this);
    }

    getFilteredValueKeys() {
        const { value } = this.props;

        // filter searchString and deleted values
        return Object.keys(value)
            .filter(key => key !== 'searchString' && value[key] !== undefined);
    }

    editValue(key) {
        const { setValue, value } = this.props;

        if (value[key] === undefined) {
            return;
        }

        const newValue = `${key}:${value[key]}`;

        setValue('searchString', `${newValue} ${value.searchString}`);
        setValue(key, undefined);

        this.setState({ cursorPos: newValue.length });
    }

    handleInputChange(event) {
        const { setValue } = this.props;

        setValue('searchString', event.target.value);
    }

    handleInputKey(event) {
        const { setValue, pills, value } = this.props;

        if (!pills) {
            return;
        }

        switch (event.key) {
            case 'ArrowLeft':
            case 'Backspace':
                if (event.target.selectionStart === 0 && event.target.selectionEnd === 0) {
                    // prevent deleting the last character of new searchString
                    event.preventDefault();

                    const keys = this.getFilteredValueKeys();

                    if (keys.length === 0) {
                        return;
                    }

                    this.editValue(keys[keys.length - 1]);
                }
                break;
            case ' ':
            case 'Enter': {
                // matches [name]:[value] in the searchString
                const matches = value.searchString.match(/[^ ]+:[^ ]+/g);
                if (matches) {
                    event.stopPropagation();
                    event.preventDefault();
                    matches
                    // split to name(index 0) and value(index 1)
                        .map(match => match.split(/:(.+)/))
                        // set all matches
                        .forEach(match => setValue(match[0], match[1]));

                    // replacing applied filters
                    setValue(
                        'searchString',
                        matches
                            .reduce((accumulator, currentValue) => accumulator
                                .replace(currentValue, ''), value.searchString),
                    );
                }
                break;
            }
            default:
        }
    }

    handleBadgeDelete(event, id) {
        const { setValue } = this.props;

        setValue(id, undefined);
    }

    handleBadgeClick(event) {
        this.editValue(event.target.id);
    }

    render() {
        const {
            additionalLabel,
            autoComplete,
            id,
            label,
            pills,
            value,
        } = this.props;
        const { cursorPos } = this.state;

        let dropdownContent;

        if (autoComplete) {
            dropdownContent = (
                <DropdownSelectContent value={value.searchString} values={autoComplete} />
            );
        }

        return (
            <InputBase
                label={label}
                id={id}
                additionalLabel={additionalLabel}
                dropdownContent={dropdownContent}
            >
                {pills
                    ? this.getFilteredValueKeys()
                        .map(key => (
                            <BadgePart
                                key={`multiselect-${id}-badge-${key}`}
                                id={key}
                                onClick={this.handleBadgeClick}
                                onDelete={this.handleBadgeDelete}
                            >
                                {`${key}: ${value[key]}`}
                            </BadgePart>
                        ))
                    : undefined}
                <InputPart
                    key="multi-selection-search-string"
                    id={id}
                    value={value.searchString}
                    onChange={this.handleInputChange}
                    onKeyDown={this.handleInputKey}
                    selectionStart={cursorPos}
                />
            </InputBase>
        );
    }
}

MultiSelect.propTypes = {
    label: PropTypes.string.isRequired,
    id: PropTypes.string.isRequired,
    setValue: PropTypes.func.isRequired,
    value: PropTypes.shape({
        searchString: PropTypes.string,
    }).isRequired,
    additionalLabel: PropTypes.string,
    autoComplete: PropTypes.arrayOf(PropTypes.shape({
        id: PropTypes.number,
        title: PropTypes.title,
    })),
    pills: PropTypes.bool,
};

MultiSelect.defaultProps = {
    additionalLabel: undefined,
    pills: false,
    autoComplete: undefined,
};

export default MultiSelect;
