import React from 'react';
import PropTypes from 'prop-types';
import { useLocation, useHistory } from 'react-router-dom';
import { Modal, ModalBody } from 'reactstrap';

function ModalRoutes(props) {
    const { getRoutesWithLocation } = props;
    const location = useLocation();
    const realLocation = location.action ? location.location : location;
    const history = useHistory();
    const { background } = realLocation.state || {};

    console.log({
        location, realLocation, history, background,
    });

    return (
        <>
            {getRoutesWithLocation(background || realLocation)}
            <Modal
                size="xl"
                isOpen={!!background}
                toggle={() => history.push(background)}
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
};

export default ModalRoutes;
