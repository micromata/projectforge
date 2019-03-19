import PropTypes from 'prop-types';
import React from 'react';
import { buttonPropType } from '../../../../utilities/propTypes';
import revisedRandomId from '../../../../utilities/revisedRandomId';
import { ButtonGroup } from '../../../design';
import ActionButton from './Button';

function ActionGroup({ actions }) {
    return (
        <ButtonGroup>
            {actions.map(action => (
                <ActionButton
                    key={`action-button-${action.title}-${revisedRandomId()}`}
                    action={action}
                />
            ))}
        </ButtonGroup>
    );
}

ActionGroup.propTypes = {
    actions: PropTypes.arrayOf(buttonPropType),
};

ActionGroup.defaultProps = {
    actions: [],
};


export default ActionGroup;
