import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { colorPropType } from '../../../../utilities/propTypes';
import AdditionalLabel from '../AdditionalLabel';
import style from './Base.module.scss';
import BaseDropdown from './dropdown';

class InputBase extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            active: false,
            focus: false,
        };

        this.handleInputOnBlur = this.handleInputOnBlur.bind(this);
        this.handleInputOnFocus = this.handleInputOnFocus.bind(this);
    }

    handleInputOnFocus() {
        this.setState({
            active: true,
            focus: true,
        });
    }

    handleInputOnBlur(event) {
        this.setState({
            active: event.target.value !== '',
            focus: false,
        });
    }

    render() {
        const {
            additionalLabel,
            className,
            color,
            dropdownContent,
            id,
            label,
        } = this.props;
        let { children } = this.props;
        const {
            active,
            focus,
        } = this.state;

        children = React.Children.map(children, child => ({
            ...child,
            props: {
                ...child.props,
                onFocus: this.handleInputOnFocus,
                onBlur: this.handleInputOnBlur,
            },
        }));

        let onlyInputs = true;

        React.Children.forEach(children, (child) => {
            if (child.type.name !== 'InputPart') {
                onlyInputs = false;
            }
        });

        return (
            <React.Fragment>
                {/* input is nested in the children */}
                {/* eslint-disable-next-line jsx-a11y/label-has-for */}
                <label
                    htmlFor={id}
                    className={classNames(
                        style.label,
                        style[color],
                        {
                            [style.active]: active || !onlyInputs,
                            [style.focus]: focus,
                        },
                        className,
                    )}
                >
                    <span className={style.title}>{label}</span>
                    <ul className={style.parts}>
                        {children}
                    </ul>
                    {dropdownContent
                        ? <BaseDropdown>{dropdownContent}</BaseDropdown>
                        : undefined}
                    <AdditionalLabel title={additionalLabel} />
                </label>
            </React.Fragment>
        );
    }
}

InputBase.propTypes = {
    children: PropTypes.node.isRequired,
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    additionalLabel: PropTypes.string,
    className: PropTypes.string,
    color: colorPropType,
    dropdownContent: PropTypes.node,
};

InputBase.defaultProps = {
    additionalLabel: undefined,
    className: undefined,
    color: undefined,
    dropdownContent: undefined,
};

export default InputBase;
