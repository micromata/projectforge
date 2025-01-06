import PropTypes from 'prop-types';
import React, { useEffect, useRef } from 'react';
import fileDownload from 'js-file-download';
import { UncontrolledTooltip } from 'reactstrap';
import { getServiceURL, handleHTTPErrors } from '../../../../../utilities/rest';
import DropArea from '../../../../design/droparea';
import LoadingContainer from '../../../../design/loading-container';
import { DynamicLayoutContext } from '../../context';

function DynamicDropArea(
    {
        id,
        title,
        uploadUrl,
        tooltip,
    },
) {
    const {
        callAction,
    } = React.useContext(DynamicLayoutContext);

    const uploadUrlRef = useRef(uploadUrl);

    useEffect(() => {
        uploadUrlRef.current = uploadUrl;
    }, [uploadUrl]);

    const [loading, setLoading] = React.useState(false);

    const uploadFile = (files) => {
        setLoading(true);
        const formData = new FormData();
        let filename;
        let status = 0;
        formData.append('file', files[0]);
        return fetch(
            getServiceURL(`${uploadUrlRef.current}`),
            {
                credentials: 'include',
                method: 'POST',
                body: formData,
            },
        )
            .then(handleHTTPErrors)
            .then((response) => {
                ({ status } = response);
                if (response.headers.get('Content-Type')
                    .includes('application/json')) {
                    return response.json();
                }
                if (response.headers.get('Content-Type').includes('application/octet-stream')) {
                    filename = Object.getResponseHeaderFilename(response.headers.get('Content-Disposition'));
                    return response.blob();
                }
                throw Error(`Error ${status}`);
            })
            .then((result) => {
                setLoading(false);
                if (filename) {
                    // result as blob expected:
                    return fileDownload(result, filename);
                }
                return callAction({ responseAction: result });
            })
            .catch((catchError) => {
                // eslint-disable-next-line no-alert
                alert(catchError);
                setLoading(false);
            });
    };

    return React.useMemo(() => (
        <LoadingContainer loading={loading}>
            <DropArea
                id={id}
                setFiles={uploadFile}
                noStyle
                title={title}
            />
            {tooltip && id && (
                <UncontrolledTooltip placement="auto" target={id}>
                    {tooltip}
                </UncontrolledTooltip>
            )}
        </LoadingContainer>
    ), [loading]);
}

DynamicDropArea.propTypes = {
    id: PropTypes.string,
    title: PropTypes.string.isRequired,
    uploadUrl: PropTypes.string.isRequired,
    tooltip: PropTypes.string,
};

export default DynamicDropArea;
