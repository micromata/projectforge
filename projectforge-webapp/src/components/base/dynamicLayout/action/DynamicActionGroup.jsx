import PropTypes from 'prop-types';
import React from 'react';
import { colorPropType } from '../../../../utilities/propTypes';
import { ButtonGroup, Col, Row, Spinner } from '../../../design';
import DynamicButton from '../components/DynamicButton';
import { DynamicLayoutContext } from '../context';

export const actionPropType = PropTypes.shape({
    id: PropTypes.string.isRequired,
    title: PropTypes.string.isRequired,
    key: PropTypes.string.isRequired,
    style: colorPropType,
    type: PropTypes.oneOf(['BUTTON']),
});

function DynamicActionGroup({ actions = [] }) {
    const { isFetching } = React.useContext(DynamicLayoutContext);

    if (!actions) {
        return null;
    }

    return (
        <Row>
            <Col>
                <ButtonGroup>
                    {actions.map((action) => (
                        <DynamicButton
                            {...action}
                            key={`dynamic-action-button-${action.id}-${action.key}`}
                        />
                    ))}
                </ButtonGroup>
                {isFetching && <Spinner color="primary" style={{ marginLeft: '1em' }} />}
            </Col>
        </Row>
    );
}

DynamicActionGroup.propTypes = {
    actions: PropTypes.arrayOf(actionPropType),
};

export default DynamicActionGroup;
