import PropTypes from 'prop-types';
import React from 'react';
import { Button, Modal, ModalBody, ModalFooter, ModalHeader } from '../../../design';
import { DynamicLayoutContext } from '../context';

function DynamicButton(props) {
    const {
        confirmMessage,
        default: isDefault,
        title,
        responseAction,
        ...stylingProps
    } = props;

    const [showConfirmMessage, setShowConfirmMessage] = React.useState(false);
    const { callAction, ui } = React.useContext(DynamicLayoutContext);

    const handleClick = (event) => {
        event.preventDefault();
        event.stopPropagation();

        if (confirmMessage) {
            setShowConfirmMessage(true);
            return;
        }

        callAction(props);
    };

    const toggleShowConfirmMessage = () => setShowConfirmMessage(!showConfirmMessage);
    const confirmAction = () => callAction(props);

    React.useEffect(() => {
        if (showConfirmMessage) {
            const listener = ({ key }) => {
                if (key === 'Enter') {
                    confirmAction();
                }
            };

            document.addEventListener('keydown', listener);

            return () => document.removeEventListener('keydown', listener);
        }

        return () => {
        };
    }, [showConfirmMessage]);

    let type = 'button';

    if (isDefault) {
        type = 'submit';
    }

    return (
        <React.Fragment>
            {confirmMessage && (
                <Modal
                    isOpen={showConfirmMessage}
                    toggle={toggleShowConfirmMessage}
                >
                    <ModalHeader toggle={toggleShowConfirmMessage} />
                    <ModalBody>{confirmMessage}</ModalBody>
                    <ModalFooter>
                        <Button color="secondary" outline onClick={toggleShowConfirmMessage}>
                            {ui.translations.cancel}
                        </Button>
                        <Button color="primary" onClick={confirmAction}>
                            {ui.translations.yes}
                        </Button>
                    </ModalFooter>
                </Modal>
            )}
            <Button
                {...stylingProps}
                onClick={handleClick}
                type={type}
            >
                {title}
            </Button>
        </React.Fragment>
    );
}

DynamicButton.propTypes = {
    title: PropTypes.string.isRequired,
    confirmMessage: PropTypes.string,
    default: PropTypes.bool,
    responseAction: PropTypes.shape({}),
};

DynamicButton.defaultProps = {
    confirmMessage: undefined,
    default: false,
    responseAction: {},
};

export default DynamicButton;
