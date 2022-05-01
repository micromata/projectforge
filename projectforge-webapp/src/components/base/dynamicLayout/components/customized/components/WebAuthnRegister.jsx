/* eslint-disable max-len */
import React from 'react';
import PropTypes from 'prop-types';
import { Button } from '../../../../../design';
import { fetchJsonGet, fetchJsonPost } from '../../../../../../utilities/rest';
import { convertPublicKeyCredentialRequestOptions, convertRegisterCredential } from '../../../../../../utilities/webauthn';
import { DynamicLayoutContext } from '../../../context';

function WebAuthnRegister({ values }) {
    const { ui, callAction } = React.useContext(DynamicLayoutContext);

    const finishRegister = async (publicKeyCredentialCreationOptions) => {
        const createRequest = convertPublicKeyCredentialRequestOptions(publicKeyCredentialCreationOptions);
        const credential = await navigator.credentials.create({ publicKey: createRequest });
        const data = convertRegisterCredential(credential, publicKeyCredentialCreationOptions);
        await fetchJsonPost(
            values.registerFinishUrl,
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
    values: PropTypes.shape({
        registerFinishUrl: PropTypes.string.isRequired,
    }).isRequired,
};

WebAuthnRegister.defaultProps = {
};

export default WebAuthnRegister;
