import { faBan } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React, { Component } from 'react';
import { Badge } from '../../index';
import BasePart from './Part';

class BadgePart extends Component {
    constructor(props) {
        super(props);

        this.handleClick = this.handleClick.bind(this);
        this.handleDeleteClick = this.handleDeleteClick.bind(this);
    }

    handleClick(event) {
        event.preventDefault();

        const { onClick } = this.props;

        if (onClick) {
            onClick(event);
        }
    }

    handleDeleteClick(event) {
        event.preventDefault();
        event.stopPropagation();

        const { id, onDelete } = this.props;

        if (onDelete) {
            onDelete(event, id);
        }
    }

    render() {
        const {
            children,
            onClick,
            onDelete,
            ...props
        } = this.props;
        return (
            <BasePart>
                <Badge onClick={this.handleClick} {...props}>
                    {children}
                    {onDelete
                        ? (
                            <FontAwesomeIcon
                                icon={faBan}
                                onClick={this.handleDeleteClick}
                                onKeyDown={this.handleDeleteClick}
                                role="button"
                                tabIndex={-1}
                            />
                        )
                        : ''}
                </Badge>
            </BasePart>
        );
    }
}

BadgePart.propTypes = {
    children: PropTypes.node.isRequired,
    id: PropTypes.string,
    onClick: PropTypes.func,
    onDelete: PropTypes.func,
};

BadgePart.defaultProps = {
    id: undefined,
    onClick: undefined,
    onDelete: undefined,
};

export default BadgePart;
