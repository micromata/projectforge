/* eslint-disable no-param-reassign,max-len */
import React from 'react';
import { Button } from '../../../../../design';
import { fetchJsonGet } from '../../../../../../utilities/rest';
// eslint-disable-next-line import/named
import { convertPublicKeyCredentialRequestOptions } from '../../../../../../utilities/webauthn';
import { DynamicLayoutContext } from '../../../context';

/**
 * Shows links to given jira issues (if given), otherwise nothing is shown.
 */
function WebAuthn() {
    const { ui } = React.useContext(DynamicLayoutContext);

    const startRegister = async (publicKeyCredentialCreationOptions) => {
        convertPublicKeyCredentialRequestOptions(publicKeyCredentialCreationOptions);
        const credential = await navigator.credentials.create({ publicKey: publicKeyCredentialCreationOptions });
        console.log(credential);
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
