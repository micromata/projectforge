import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { Modal, ModalBody } from '../../../components/design';
import FormPage from './FormPage';
import { callAction } from '../../../actions';

function FormModal(props) {
    const { onCallAction } = props;

    return (
        <Modal
            toggle={() => onCallAction({ responseAction: { targetType: 'CLOSE_MODAL' } })}
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
    onCallAction: PropTypes.func.isRequired,
};

const actions = {
    onCallAction: callAction,
};

export default connect(() => ({}), actions)(FormModal);
