import React from 'react';
import PropTypes from 'prop-types';
import { useLocation, useHistory } from 'react-router-dom';
import { Modal, ModalBody } from 'reactstrap';
import { connect } from 'react-redux';
import { callAction } from '../actions';

function ModalRoutes(props) {
    const { getRoutesWithLocation, onCallAction } = props;
    const location = useLocation();
    const realLocation = location.action ? location.location : location;
    const history = useHistory();
    const { background } = realLocation.state || {};

    return (
        <>
            {getRoutesWithLocation(background || realLocation)}
            <Modal
                size="xl"
                isOpen={!!background}
                toggle={() => onCallAction({ responseAction: { targetType: 'CLOSE_MODAL' } })}
            >
                <ModalBody>
                    {background && getRoutesWithLocation(realLocation)}
                </ModalBody>
            </Modal>
        </>
    );
}

ModalRoutes.propTypes = {
    getRoutesWithLocation: PropTypes.func.isRequired,
    onCallAction: PropTypes.func.isRequired,
};

const actions = {
    onCallAction: callAction,
};

export default connect(() => ({}), actions)(ModalRoutes);
