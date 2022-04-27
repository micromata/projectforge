/* eslint-disable no-param-reassign,max-len */
import React from 'react';
import { Button } from '../../../../../design';
import { fetchJsonGet, fetchJsonPost } from '../../../../../../utilities/rest';
// eslint-disable-next-line import/named
import { convertPublicKeyCredentialRequestOptions, convertCredential } from '../../../../../../utilities/webauthn';
import { DynamicLayoutContext } from '../../../context';

function WebAuthn() {
    const { ui } = React.useContext(DynamicLayoutContext);

    const finishRegister = async (publicKeyCredentialCreationOptions) => {
        const createRequest = convertPublicKeyCredentialRequestOptions(publicKeyCredentialCreationOptions);
        const credential = await navigator.credentials.create({ publicKey: createRequest });
        const data = convertCredential(credential, publicKeyCredentialCreationOptions);
        fetchJsonPost(
            'webauthn/registerFinish',
            { data },
            (json) => {
                console.log(json); // Action here...
            },
        );
    };

    const register = () => {
        fetchJsonGet('webauthn/register',
            { },
            (json) => {
                finishRegister(json);
            });
    };

    const finishAuthenticate = async (publicKeyCredentialCreationOptions) => {
        const createRequest = convertPublicKeyCredentialRequestOptions(publicKeyCredentialCreationOptions);
        console.log(createRequest);
        const credential = await navigator.credentials.get({ publicKey: createRequest });
        const data = convertCredential(credential, publicKeyCredentialCreationOptions);
        console.log(data);
        fetchJsonGet(
            'webauthn/authenticateFinish',
            { data },
            (json) => {
                console.log(json); // Action here...
            },
        );
    };

    const authenticate = () => {
        fetchJsonGet('webauthn/authenticate',
            { },
            (json) => {
                finishAuthenticate(json);
            });
    };

    return (
        <>
            <Button color="link" onClick={register}>
                {ui.translations['webauthn.registration.button.register']}
            </Button>
            <Button color="link" onClick={authenticate}>
                {ui.translations['webauthn.registration.button.authenticate']}
            </Button>
        </>
    );
}

WebAuthn.propTypes = {
};

WebAuthn.defaultProps = {
};

export default WebAuthn;
