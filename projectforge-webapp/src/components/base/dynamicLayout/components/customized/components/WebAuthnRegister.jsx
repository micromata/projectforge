/* eslint-disable max-len */
import React from 'react';
import { Button } from '../../../../../design';
import { fetchJsonGet, fetchJsonPost } from '../../../../../../utilities/rest';
import { convertPublicKeyCredentialRequestOptions, convertRegisterCredential } from '../../../../../../utilities/webauthn';
import { DynamicLayoutContext } from '../../../context';

function WebAuthnRegister() {
    const { ui, callAction } = React.useContext(DynamicLayoutContext);

    const finishRegister = async (publicKeyCredentialCreationOptions) => {
        const createRequest = convertPublicKeyCredentialRequestOptions(publicKeyCredentialCreationOptions);
        const credential = await navigator.credentials.create({ publicKey: createRequest });
        const data = convertRegisterCredential(credential, publicKeyCredentialCreationOptions);
        await fetchJsonPost(
            'webauthn/registerFinish',
            { data },
            (json) => {
                callAction({ responseAction: json });
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

    return (
        <>
            <Button color="link" onClick={register}>
                {ui.translations['webauthn.registration.button.register']}
            </Button>
        </>
    );
}

WebAuthnRegister.propTypes = {
};

WebAuthnRegister.defaultProps = {
};

export default WebAuthnRegister;
