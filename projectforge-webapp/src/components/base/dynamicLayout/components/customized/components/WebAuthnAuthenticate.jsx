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
    const { ui, data, callAction } = React.useContext(DynamicLayoutContext);

    const finishAuthenticate = async (publicKeyCredentialCreationOptions) => {
        const createRequest = convertPublicKeyCredentialRequestOptions(publicKeyCredentialCreationOptions);
        const credential = await navigator.credentials.get({ publicKey: createRequest });
        data.webAuthnFinishRequest = convertAuthenticateCredential(credential, publicKeyCredentialCreationOptions);
        await fetchJsonPost(
            values.authenticateFinishUrl,
            { data },
            (json) => {
                callAction({ responseAction: json });
            },
        );
    };

    // The user is always logged in here: the 2FA of the login itself lives in projectforge-next,
    // this component only serves the in-session 2FA. Hence no auto start - the user clicks the button.
    const authenticate = () => {
        fetchJsonGet(
            'webauthn/webAuthn',
            {},
            (json) => {
                finishAuthenticate(json);
            },
        );
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
        authenticateFinishUrl: PropTypes.string.isRequired,
    }).isRequired,
};

export default WebAuthn;
