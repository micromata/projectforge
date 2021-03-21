import {faDownload} from '@fortawesome/free-solid-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React from 'react';
import {getServiceURL, handleHTTPErrors} from '../../../../../utilities/rest';
import {Table} from '../../../../design';
import DropArea from '../../../../design/droparea';
import {DynamicLayoutContext} from '../../context';

function DynamicAttachmentList(
    {
        category,
        id,
        listId,
        serviceBaseUrl,
        restBaseUrl,
        accessString,
        downloadOnRowClick,
    },
) {
    const {
        callAction,
        data,
        setData,
        ui,
    } = React.useContext(DynamicLayoutContext);
    const {attachments} = data;

    const uploadFile = (files) => {
        const formData = new FormData();
        formData.append('file', files[0]);
        const params = accessString ? `?accessString=${encodeURIComponent(accessString)}` : ''
        fetch(
            // Set the image with id -1, so the image will be set in the session.
            getServiceURL(`${restBaseUrl}/upload/${category}/${id}/${listId}${params}`),
            {
                credentials: 'include',
                method: 'POST',
                body: formData,
            },
        )
            .then(handleHTTPErrors)
            .then(response => response.json())
            .then(json => callAction({responseAction: json}))
            .catch((catchError) => {
                alert(catchError);
            });
    };

    const handleRowClick = entry => (event) => {
        event.stopPropagation();
        if (downloadOnRowClick) {
            download(entry.fileId)
        } else {
            callAction({
                responseAction: {
                    targetType: 'MODAL',
                    url: `${serviceBaseUrl}/${id}?category=${category}&fileId=${entry.fileId}&listId=${listId}${accessString}`,
                },
            });
        }
    };

    const handleDownload = entryId => (event) => {
        event.stopPropagation();
        download(entryId)
    };

    const download = (entryId) => {
        callAction({
            responseAction: {
                targetType: 'DOWNLOAD',
                url: getServiceURL(`${restBaseUrl}/download/${category}/${id}`, {
                    fileId: entryId,
                    listId,
                    accessString,
                }),
                absolute: true,
            },
        });
    }

    return React.useMemo(() => {
        if (id && id > 0) {
            return (
                <DropArea
                    setFiles={uploadFile}
                    noStyle
                    title={ui.translations['file.upload.dropArea']}
                >
                    {attachments && attachments.length > 0 && (
                        <Table striped hover>
                            <thead>
                            <tr>
                                <th>{ui.translations['attachment.fileName']}</th>
                                <th>{ui.translations['attachment.size']}</th>
                                <th>{ui.translations.description}</th>
                                <th>{ui.translations.created}</th>
                                <th>{ui.translations.createdBy}</th>
                            </tr>
                            </thead>
                            <tbody>
                            {attachments.map(entry => (
                                <tr key={entry.fileId} onClick={handleRowClick(entry)}>
                                    <td>
                                            <span
                                                role="presentation"
                                                onKeyDown={() => {
                                                }}
                                                onClick={handleDownload(entry.fileId)}
                                            >
                                                {`${entry.name} `}
                                                <FontAwesomeIcon icon={faDownload}/>
                                            </span>
                                    </td>
                                    <td>{entry.sizeHumanReadable}</td>
                                    <td>{entry.description}</td>
                                    <td>{entry.createdFormatted}</td>
                                    <td>{entry.createdByUser}</td>
                                </tr>
                            ))}
                            </tbody>
                        </Table>
                    )}
                </DropArea>
            );
        }
        return (
            <React.Fragment>
                {ui.translations['attachment.onlyAvailableAfterSave']}
            </React.Fragment>
        );
    }, [setData, id, attachments]);
}

DynamicAttachmentList.propTypes = {
    category: PropTypes.string.isRequired,
    listId: PropTypes.string.isRequired,
    id: PropTypes.number,
    readOnly: PropTypes.bool,
    serviceBaseUrl: PropTypes.string,
    restBaseUrl: PropTypes.string,
    accessString: PropTypes.string,
    downloadOnRowClick: PropTypes.bool,
};

DynamicAttachmentList.defaultProps = {
    id: undefined, // Undefined for new object.
    readOnly: false,
    serviceBaseUrl: '/react/attachment/dynamic',
    restBaseUrl: '/rs/attachments',
    accessString: '',
    downloadOnRowClick: false,
};

export default DynamicAttachmentList;
