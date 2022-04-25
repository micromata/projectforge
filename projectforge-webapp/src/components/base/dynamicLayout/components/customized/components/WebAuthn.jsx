/* eslint-disable no-param-reassign,max-len */
import React from 'react';
import { Button } from '../../../../../design';
import { fetchJsonGet, fetchJsonPost } from '../../../../../../utilities/rest';
// eslint-disable-next-line import/named
import { convertPublicKeyCredentialRequestOptions, convertCredential } from '../../../../../../utilities/webauthn';
import { DynamicLayoutContext } from '../../../context';

/**
 * Shows links to given jira issues (if given), otherwise nothing is shown.
 */
function WebAuthn() {
    const { ui } = React.useContext(DynamicLayoutContext);

    const startRegister = async (publicKeyCredentialCreationOptions) => {
        const createRequest = convertPublicKeyCredentialRequestOptions(publicKeyCredentialCreationOptions);
        const credential = await navigator.credentials.create({ publicKey: createRequest });
        const data = convertCredential(credential, publicKeyCredentialCreationOptions);
        fetchJsonPost(
            'webauthn/finish',
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
                startRegister(json);
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

WebAuthn.propTypes = {
};

WebAuthn.defaultProps = {
};

export default WebAuthn;
