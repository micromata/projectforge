import React from 'react';
import PropTypes from 'prop-types';
import { useLocation, useHistory } from 'react-router-dom';
import { Modal, ModalBody } from 'reactstrap';

function ModalRoutes(props) {
    const { getRoutesWithLocation } = props;
    const location = useLocation();
    const history = useHistory();
    const { background } = location.state || {};

    return (
        <>
            {getRoutesWithLocation(background || location)}
            <Modal
                size="xl"
                isOpen={!!background}
                toggle={() => history.push(background)}
            >
                <ModalBody>
                    {background && getRoutesWithLocation(location)}
                </ModalBody>
            </Modal>
        </>
    );
}

ModalRoutes.propTypes = {
    getRoutesWithLocation: PropTypes.func.isRequired,
};

export default ModalRoutes;
