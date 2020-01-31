import PropTypes from 'prop-types';
import React from 'react';
import { Modal, ModalBody } from '../../../components/design';
import history from '../../../utilities/history';
import EditPage from './EditPage';

function EditModal(props) {
    const { baseUrl } = props;

    return (
        <Modal
            toggle={() => history.push(baseUrl)}
            isOpen
            className="modal-xl"
        >
            <ModalBody>
                <EditPage {...props} />
            </ModalBody>
        </Modal>
    );
}

EditModal.propTypes = {
    baseUrl: PropTypes.string.isRequired,
};

EditModal.defaultProps = {};

export default EditModal;
