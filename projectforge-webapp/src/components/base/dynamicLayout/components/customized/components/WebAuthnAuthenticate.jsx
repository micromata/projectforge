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
    const [authenticationStarted, setAuthenticationStarted] = React.useState(false);

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

    const authenticate = () => {
        setAuthenticationStarted(true);
        fetchJsonGet(
            values.authenticateUrl || 'webauthn/webAuthn',
            {},
            (json) => {
                finishAuthenticate(json);
            },
        );
    };

    // Auto-start WebAuthn authentication when component mounts, but only for login context
    // (when authenticateUrl is explicitly provided, indicating this is the public login service)
    React.useEffect(() => {
        if (!authenticationStarted && values.authenticateUrl) {
            authenticate();
        }
    }, []);

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
        authenticateUrl: PropTypes.string,
    }).isRequired,
};

export default WebAuthn;
