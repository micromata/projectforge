import PropTypes from 'prop-types';
import React from 'react';
import { UncontrolledTooltip } from 'reactstrap';
import { Button, Modal, ModalBody, ModalFooter, ModalHeader } from '../../../design';
import { DynamicLayoutContext } from '../context';

function DynamicButton(props) {
    const {
        confirmMessage,
        default: isDefault,
        id,
        title,
        tooltip,
        disabled,
        responseAction,
        handleButtonClick,
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

        if (handleButtonClick) {
            handleButtonClick();
            return;
        }

        callAction(props);
    };

    const toggleShowConfirmMessage = () => setShowConfirmMessage(!showConfirmMessage);
    const confirmAction = () => {
        if (handleButtonClick) {
            handleButtonClick();
            setShowConfirmMessage(false);
            return;
        }
        callAction(props);
        setShowConfirmMessage(false);
    };

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

    const buttonId = `${ui.uid}-${id}`;

    return (
        <>
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
                disabled={disabled}
            >
                <span id={buttonId}>{title}</span>
            </Button>
            {tooltip && ui.uid && id && (
                <UncontrolledTooltip placement="auto" target={buttonId}>
                    {tooltip}
                </UncontrolledTooltip>
            )}
        </>
    );
}

DynamicButton.propTypes = {
    title: PropTypes.string.isRequired,
    confirmMessage: PropTypes.string,
    default: PropTypes.bool,
    id: PropTypes.string,
    responseAction: PropTypes.shape({}),
    tooltip: PropTypes.string,
    disabled: PropTypes.bool,
    handleButtonClick: PropTypes.func,
};

DynamicButton.defaultProps = {
    confirmMessage: undefined,
    default: false,
    id: undefined,
    responseAction: {},
    tooltip: undefined,
    disabled: undefined,
    handleButtonClick: undefined,
};

export default DynamicButton;
