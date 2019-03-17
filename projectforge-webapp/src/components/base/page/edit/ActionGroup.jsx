import PropTypes from 'prop-types';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { updateEditPageData } from '../../../../actions';
import { buttonPropType } from '../../../../utilities/propTypes';
import revisedRandomId from '../../../../utilities/revisedRandomId';
import { Button, ButtonGroup, Row } from '../../../design';

// TODO: ADD FUNCTION
class ActionGroup extends Component {
    constructor(props) {
        super(props);

        this.handleClick = this.handleClick.bind(this);
    }

    handleClick(event) {
        const { ...props } = this.props;

        const actionFunction = props[event.target.dataset.action];

        if (actionFunction) {
            actionFunction();
        }
    }

    render() {
        const { actions } = this.props;
        return (
            <Row>
                <ButtonGroup>
                    {actions.map(action => (
                        <Button
                            key={`action-button-${action.title}-${revisedRandomId()}`}
                            color={action.style}
                            onClick={this.handleClick}
                            data-action={action.id}
                        >
                            {action.title}
                        </Button>
                    ))
                    }
                </ButtonGroup>
            </Row>
        );
    }
}

ActionGroup.propTypes = {
    update: PropTypes.func.isRequired,
    actions: PropTypes.arrayOf(buttonPropType),
};

ActionGroup.defaultProps = {
    actions: [],
};

const mapStateToProps = () => ({});

const actions = {
    update: updateEditPageData,
};

export default connect(mapStateToProps, actions)(ActionGroup);
