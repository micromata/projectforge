/* eslint-disable max-len */
import React from 'react';
import PropTypes from 'prop-types';
import { UncontrolledTooltip } from 'reactstrap';
import { Button } from '../../../../../design';
import { fetchJsonGet, fetchJsonPost } from '../../../../../../utilities/rest';
import {
    convertAuthenticateCredential,
    convertPublicKeyCredentialRequestOptions,
} from '../../../../../../utilities/webauthn';
import { DynamicLayoutContext } from '../../../context';

function WebAuthn({ values }) {
    const { ui, callAction } = React.useContext(DynamicLayoutContext);

    const finishAuthenticate = async (publicKeyCredentialCreationOptions) => {
        const createRequest = convertPublicKeyCredentialRequestOptions(publicKeyCredentialCreationOptions);
        const credential = await navigator.credentials.get({ publicKey: createRequest });
        const data = convertAuthenticateCredential(credential, publicKeyCredentialCreationOptions);
        await fetchJsonPost(
            values?.authenticateFinishUrl || 'webauthn/authenticateFinish',
            { data },
            (json) => {
                callAction({ responseAction: json });
            },
        );
    };

    const authenticate = () => {
        fetchJsonGet('webauthn/authenticate',
            {},
            (json) => {
                finishAuthenticate(json);
            });
    };

    return (
        <>
            <Button color="secondary" outline onClick={authenticate}>
                <span id="webauthn_authenticate">{ui.translations['webauthn.registration.button.authenticate']}</span>
            </Button>
            <UncontrolledTooltip placement="auto" target="webauthn_authenticate">
                {ui.translations['webauthn.registration.button.authenticate.info']}
            </UncontrolledTooltip>
        </>
    );
}

WebAuthn.propTypes = {
    values: PropTypes.shape({
        authenticateFinishUrl: PropTypes.string,
    }),
};

WebAuthn.defaultProps = {
    values: undefined,
};

export default WebAuthn;
