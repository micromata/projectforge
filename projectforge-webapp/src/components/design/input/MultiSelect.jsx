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

        this.state = {
            cursorPos: value.searchString.length,
            selectIndex: -1,
        };

        this.getDropdownValues = this.getDropdownValues.bind(this);
        this.handleBadgeClick = this.handleBadgeClick.bind(this);
        this.handleBadgeDelete = this.handleBadgeDelete.bind(this);
        this.handleInputChange = this.handleInputChange.bind(this);
        this.selectFromDropdown = this.selectFromDropdown.bind(this);
        this.setSelectIndex = this.setSelectIndex.bind(this);
        this.handleInputKey = this.handleInputKey.bind(this);
        this.getFilteredValueKeys = this.getFilteredValueKeys.bind(this);
        this.editValue = this.editValue.bind(this);
    }

    getDropdownValues() {
        const { value, autoComplete } = this.props;

        return autoComplete.filter(entry => entry.toLowerCase()
            .includes(value.searchString.toLowerCase()));
    }

    getFilteredValueKeys() {
        const { value } = this.props;

        // filter searchString and deleted values
        return Object.keys(value)
            .filter(key => key !== 'searchString' && value[key] !== undefined);
    }

    setSelectIndex(index) {
        this.setState({
            selectIndex: index,
        });
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

    selectFromDropdown(index, target) {
        const { autoCompleteForm, single } = this.props;
        const values = this.getDropdownValues();

        if (values.length <= index) {
            return;
        }

        const { setValue } = this.props;

        setValue('searchString', autoCompleteForm.replace('$AUTOCOMPLETE', values[index]));

        if (single && target !== undefined) {
            target.blur();
        }
    }

    handleInputKey(event) {
        const {
            setValue,
            pills,
            value,
        } = this.props;
        const { selectIndex } = this.state;

        switch (event.key) {
            case 'ArrowLeft':
            case 'Backspace':
                if (!pills) {
                    return;
                }

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
                // handling dropdown selection
                if (
                    event.key === 'Enter'
                    && selectIndex > -1
                    && selectIndex < this.getDropdownValues().length
                ) {
                    event.stopPropagation();
                    event.preventDefault();
                    this.selectFromDropdown(selectIndex, event.target);
                    return;
                }

                if (!pills) {
                    return;
                }

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
            case 'ArrowUp':
            case 'ArrowDown': {
                let newValue = selectIndex + (event.key === 'ArrowUp' ? -1 : 1);
                const dropdownLength = this.getDropdownValues().length;

                if (newValue < -1) {
                    newValue = -1;
                } else if (newValue >= dropdownLength) {
                    newValue = dropdownLength - 1;
                }

                this.setSelectIndex(newValue);
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
        const { cursorPos, selectIndex } = this.state;

        let dropdownContent;

        if (autoComplete) {
            dropdownContent = (
                <DropdownSelectContent
                    select={this.selectFromDropdown}
                    setSelected={this.setSelectIndex}
                    values={this.getDropdownValues()}
                    selectIndex={selectIndex}
                />
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
                    // disable browser autocomplete when using custom
                    autoComplete={autoComplete ? 'off' : 'on'}
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
    autoComplete: PropTypes.arrayOf(PropTypes.string),
    autoCompleteForm: PropTypes.string,
    pills: PropTypes.bool,
    single: PropTypes.bool,
};

MultiSelect.defaultProps = {
    additionalLabel: undefined,
    autoComplete: undefined,
    autoCompleteForm: '$AUTOCOMPLETE',
    pills: false,
    single: false,
};

export default MultiSelect;
