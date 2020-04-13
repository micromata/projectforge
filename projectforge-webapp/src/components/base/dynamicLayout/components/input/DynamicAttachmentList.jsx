import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { Button, Table } from '../../../../design';
import { DynamicLayoutContext } from '../../context';
import DropArea from '../../../../design/droparea';
import { getServiceURL, handleHTTPErrors } from '../../../../../utilities/rest';
import { callAction } from '../../../../../actions';

function DynamicAttachmentList(
    {
        id,
        listId,
        restBaseUrl,
        onCallAction,
    },
) {
    const { data, setData, ui } = React.useContext(DynamicLayoutContext);
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
            .then(json => onCallAction({ responseAction: json }))
            .catch((catchError) => {
                alert(catchError);
            });
    };

    const deleteFile = (file) => {
        console.log(file);
    };

    return React.useMemo(() => {
        if (id && id > 0) {
            return (
                <DropArea setFiles={uploadFile} noStyle>
                    {ui.translations['file.upload.dropArea']}
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
                                    <tr
                                        key={entry.id}
                                        onClick={() => window.open(getServiceURL(`/rs/${restBaseUrl}/download/${id}/${listId}`, {
                                            fileId: entry.id,
                                        }), '_blank')}
                                    >
                                        <td>{entry.name}</td>
                                        <td>{entry.sizeHumanReadable}</td>
                                        <td>{entry.description}</td>
                                        <td>{entry.createdFormatted}</td>
                                        <td>{entry.createdByUser}</td>
                                        <td>
                                            <Button color="danger" onClick={() => deleteFile(entry.id)}>
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
    id: PropTypes.number,
    listId: PropTypes.string.isRequired,
    restBaseUrl: PropTypes.string.isRequired,
    readOnly: PropTypes.bool,
    onCallAction: PropTypes.func.isRequired,
};

DynamicAttachmentList.defaultProps = {
    id: undefined, // Undefined for new object.
    readOnly: false,
};

const actions = {
    onCallAction: callAction,
};

export default connect(undefined, actions)(DynamicAttachmentList);
