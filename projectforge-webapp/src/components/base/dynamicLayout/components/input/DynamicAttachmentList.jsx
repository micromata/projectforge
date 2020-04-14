import { faDownload } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React from 'react';
import { getServiceURL, handleHTTPErrors } from '../../../../../utilities/rest';
import { Button, Table } from '../../../../design';
import DropArea from '../../../../design/droparea';
import { DynamicLayoutContext } from '../../context';

function DynamicAttachmentList(
    {
        id,
        listId,
        restBaseUrl,
    },
) {
    const {
        callAction,
        data,
        setData,
        ui,
    } = React.useContext(DynamicLayoutContext);
    const { attachments } = data;

    const uploadFile = (files) => {
        const formData = new FormData();
        formData.append('file', files[0]);
        fetch(
            // Set the image with id -1, so the image will be set in the session.
            getServiceURL(`${restBaseUrl}/upload/${id}/${listId}`),
            {
                credentials: 'include',
                method: 'POST',
                body: formData,
            },
        )
            .then(handleHTTPErrors)
            .then(response => response.json())
            .then(json => callAction({ responseAction: json }))
            .catch((catchError) => {
                alert(catchError);
            });
    };

    const handleRowClick = entry => (event) => {
        event.stopPropagation();
        callAction({
            responseAction: {
                targetType: 'MODAL',
                url: `/react/attachment/dynamic/${id}?category=contract&fileId=${entry.fileId}&listId=${listId}`,
            },
        });
    };

    const handleDownload = entryId => (event) => {
        event.stopPropagation();
        window.open(
            getServiceURL(`/rs/${restBaseUrl}/download/${id}/${listId}`, {
                fileId: entryId,
            }), '_blank',
        );
    };

    const handleDelete = entryId => (event) => {
        event.stopPropagation();
        console.log(entryId);
    };

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
                                    <th>{ui.translations['attachment.filename']}</th>
                                    <th>{ui.translations['attachment.size']}</th>
                                    <th>{ui.translations.description}</th>
                                    <th>{ui.translations.created}</th>
                                    <th>{ui.translations.createdBy}</th>
                                    <th>{' '}</th>
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
                                                <FontAwesomeIcon icon={faDownload} />
                                            </span>
                                        </td>
                                        <td>{entry.sizeHumanReadable}</td>
                                        <td>{entry.description}</td>
                                        <td>{entry.createdFormatted}</td>
                                        <td>{entry.createdByUser}</td>
                                        <td>
                                            <Button
                                                color="danger"
                                                onClick={handleDelete(entry.fileId)}
                                            >
                                                {ui.translations.delete}
                                            </Button>
                                        </td>
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
    listId: PropTypes.string.isRequired,
    restBaseUrl: PropTypes.string.isRequired,
    id: PropTypes.number,
    readOnly: PropTypes.bool,
    rowClickAction: PropTypes.shape({}),
};

DynamicAttachmentList.defaultProps = {
    id: undefined, // Undefined for new object.
    readOnly: false,
    rowClickAction: undefined,
};

export default DynamicAttachmentList;
