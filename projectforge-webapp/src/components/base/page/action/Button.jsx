import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import {
    abortEditPage,
    clone,
    markAsDeleted,
    resetListFilter,
    undelete,
    updateEditPageData,
    updateList,
} from '../../../../actions';
import { buttonPropType } from '../../../../utilities/propTypes';
import { Button } from '../../../design';

class ActionButton extends React.Component {
    constructor(props) {
        super(props);

        this.handleClick = this.handleClick.bind(this);
    }

    handleClick(event) {
        const { action, ...props } = this.props;

        event.preventDefault();
        event.stopPropagation();

        if (action.handleClick) {
            action.handleClick(action, event);
            return;
        }

        const actionFunction = props[action.id];

        if (actionFunction) {
            actionFunction();
        }
    }

    render() {
        const { action } = this.props;

        let color = 'secondary';
        let type = 'button';


        if (action.default) {
            type = 'submit';
        }

        if (action.style) {
            color = action.style;
        } else if (action.type === 'checkbox' && action.checked) {
            color = 'primary';
        }

        return (
            <Button
                color={color}
                onClick={this.handleClick}
                type={type}
            >
                {action.title}
            </Button>
        );
    }
}

ActionButton.propTypes = {
    action: buttonPropType.isRequired,
    reset: PropTypes.func.isRequired,
    update: PropTypes.func.isRequired,
};

const mapStateToProps = () => ({});

const actions = {
    create: updateEditPageData,
    update: updateEditPageData,
    cancel: abortEditPage,
    reset: resetListFilter,
    search: updateList,
    markAsDeleted,
    undelete,
    clone,
};

export default connect(mapStateToProps, actions)(ActionButton);
