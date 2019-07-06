import PropTypes from 'prop-types';
import React from 'react';
import { colorPropType } from '../../../../utilities/propTypes';
import { ButtonGroup } from '../../../design';
import DynamicActionButton from './DynamicActionButton';

export const actionPropType = PropTypes.shape({
    id: PropTypes.string.isRequired,
    title: PropTypes.string.isRequired,
    key: PropTypes.string.isRequired,
    style: colorPropType,
    type: PropTypes.oneOf(['BUTTON']),
});

function DynamicActionGroup({ actions }) {
    if (!actions) {
        return <React.Fragment />;
    }

    return (
        <ButtonGroup>
            {actions.map(action => (
                <DynamicActionButton
                    key={`dynamic-action-button-${action.id}-${action.key}`}
                    {...action}
                />
            ))}
        </ButtonGroup>
    );
}

DynamicActionGroup.propTypes = {
    actions: PropTypes.arrayOf(actionPropType),
};

DynamicActionGroup.defaultProps = {
    actions: [],
};

export default DynamicActionGroup;
