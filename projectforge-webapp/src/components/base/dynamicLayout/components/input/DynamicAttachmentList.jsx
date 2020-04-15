import { faDownload } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React from 'react';
import { getServiceURL, handleHTTPErrors } from '../../../../../utilities/rest';
import { Table } from '../../../../design';
import DropArea from '../../../../design/droparea';
import { DynamicLayoutContext } from '../../context';

function DynamicAttachmentList(
    {
        category,
        id,
        listId,
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
            getServiceURL(`attachments/upload/${category}/${id}/${listId}`),
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
                url: `/react/attachment/dynamic/${id}?category=${category}&fileId=${entry.fileId}&listId=${listId}`,
            },
        });
    };

    const handleDownload = entryId => (event) => {
        event.stopPropagation();
        window.open(
            getServiceURL(`/rs/attachments/download/${category}/${id}`, {
                fileId: entryId,
                listId,
            }), '_blank',
        );
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
                                                <FontAwesomeIcon icon={faDownload} />
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
};

DynamicAttachmentList.defaultProps = {
    id: undefined, // Undefined for new object.
    readOnly: false,
};

export default DynamicAttachmentList;
