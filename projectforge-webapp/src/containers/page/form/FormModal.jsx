import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { Modal, ModalBody } from '../../../components/design';
import FormPage from './FormPage';
import { callAction } from '../../../actions';

function FormModal(props) {
    const { baseUrl, onCallAction } = props;

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
    baseUrl: PropTypes.string.isRequired,
    onCallAction: PropTypes.func.isRequired,
};

FormModal.defaultProps = {};

const actions = {
    onCallAction: callAction,
};

export default connect(() => ({}), actions)(FormModal);
