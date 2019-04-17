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

        this.getFilteredChildren = this.getFilteredChildren.bind(this);
        this.calculateActiveState = this.calculateActiveState.bind(this);
        this.handleInputOnBlur = this.handleInputOnBlur.bind(this);
        this.handleInputOnFocus = this.handleInputOnFocus.bind(this);

        this.state = {
            active: this.calculateActiveState(),
            focus: false,
        };
    }

    getFilteredChildren() {
        const { children } = this.props;

        return React.Children
            .toArray(children)
            .filter(child => child);
    }

    calculateActiveState() {
        const children = this.getFilteredChildren();

        if (React.Children.count(children) === 1) {
            const child = React.Children.only(children[0]);

            return !(child.type.name === 'InputPart' && !child.props.value);
        }

        return true;
    }


    handleInputOnFocus() {
        this.setState({
            active: true,
            focus: true,
        });
    }

    handleInputOnBlur() {
        this.setState({
            active: this.calculateActiveState(),
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
        const {
            active,
            focus,
        } = this.state;

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
                            [style.active]: active,
                            [style.focus]: focus,
                        },
                        className,
                    )}
                >
                    <span className={style.title}>{label}</span>
                    <ul className={style.parts}>
                        {this.getFilteredChildren()
                            .map(child => ({
                                ...child,
                                props: {
                                    ...child.props,
                                    onFocus: this.handleInputOnFocus,
                                    onBlur: this.handleInputOnBlur,
                                },
                            }))}
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
