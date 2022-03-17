import PropTypes from 'prop-types';
import React from 'react';
import { Modal, ModalBody } from '../../../components/design';
import history from '../../../utilities/history';
import FormPage from './FormPage';

function FormModal(props) {
    const { baseUrl } = props;

    return (
        <Modal
            toggle={() => history.push(baseUrl)}
            isOpen
            className="modal-xl"
        >
            <ModalBody>
                <FormPage {...props} />
            </ModalBody>
        </Modal>
    );
}

FormModal.propTypes = {
    baseUrl: PropTypes.string.isRequired,
};

FormModal.defaultProps = {};

export default FormModal;
