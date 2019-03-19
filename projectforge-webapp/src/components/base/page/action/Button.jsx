import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { updateEditPageData } from '../../../../actions';
import { buttonPropType } from '../../../../utilities/propTypes';
import { Button } from '../../../design';

class ActionButton extends React.Component {
    constructor(props) {
        super(props);

        this.handleClick = this.handleClick.bind(this);
    }

    handleClick(event) {
        const { action, ...props } = this.props;

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

        if (action.style) {
            color = action.style;
        } else if (action.type === 'checkbox' && action.checked) {
            color = 'primary';
        }

        return (
            <Button
                color={color}
                onClick={this.handleClick}
            >
                {action.title}
            </Button>
        );
    }
}

ActionButton.propTypes = {
    update: PropTypes.func.isRequired,
    action: buttonPropType.isRequired,
};

const mapStateToProps = () => ({});

// TODO: ADD FUNCTIONS
const actions = {
    update: updateEditPageData,
};

export default connect(mapStateToProps, actions)(ActionButton);
