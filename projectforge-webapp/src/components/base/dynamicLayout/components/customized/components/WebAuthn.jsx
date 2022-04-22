import React from 'react';
import { Button } from '../../../../../design';
import { fetchJsonGet } from '../../../../../../utilities/rest';
import { DynamicLayoutContext } from '../../../context';

/**
 * Shows links to given jira issues (if given), otherwise nothing is shown.
 */
function WebAuthn() {
    const { ui } = React.useContext(DynamicLayoutContext);
    const register = () => {
        fetchJsonGet('webauthn/register',
            { },
            (json) => {
                console.log(json);
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
