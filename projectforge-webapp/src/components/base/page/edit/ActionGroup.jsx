import PropTypes from 'prop-types';
import React from 'react';
import { Button, ButtonGroup, Row } from '../../../design';
import { buttonPropType } from '../../../../utilities/propTypes';
import revisedRandomId from '../../../../utilities/revisedRandomId';

// TODO: ADD FUNCTION
function ActionGroup({ actions }) {
    return (
        <Row>
            <ButtonGroup>
                {actions.map(action => (
                    <Button
                        key={`action-button-${action.title}-${revisedRandomId()}`}
                        color={action.style}
                    >
                        {action.title}
                    </Button>
                ))
                }
            </ButtonGroup>
        </Row>
    );
}

ActionGroup.propTypes = {
    actions: PropTypes.arrayOf(buttonPropType),
};

ActionGroup.defaultProps = {
    actions: [],
};

export default ActionGroup;
