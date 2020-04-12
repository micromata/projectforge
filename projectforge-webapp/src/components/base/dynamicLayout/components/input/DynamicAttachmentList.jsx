import PropTypes from 'prop-types';
import React from 'react';
import { Table } from '../../../../design';
import { DynamicLayoutContext } from '../../context';
import DropArea from '../../../../design/droparea';
import { getServiceURL, handleHTTPErrors } from '../../../../../utilities/rest';

function DynamicAttachmentList(
    {
        id,
        value,
        ...props
    },
) {
    const { data, setData, ui } = React.useContext(DynamicLayoutContext);

    const changeFile = (files) => {
        console.log("Upload");

        /*fetch(
            // Set the image with id -1, so the image will be set in the session.
            getServiceURL('address/uploadImage/-1'),
            {
                credentials: 'include',
                method: 'POST',
                body: formData,
            },
        )
            .then(handleHTTPErrors)
            .then(() => {
                setData({ imageData: [1] });

                const fileReader = new FileReader();

                fileReader.onload = ({ currentTarget }) => {
                    setSrc(currentTarget.result);
                    setLoading(false);
                };

                fileReader.readAsDataURL(files[0]);
            })
            .catch((catchError) => {
                setError(catchError);
                setLoading(false);
            });*/
    };

    return React.useMemo(() => {
        const { attachments } = data;
        return (
            <DropArea setFiles={changeFile}>
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
                                <th>{ui.translations.modified}</th>
                                <th>{ui.translations.modifiedBy}</th>
                            </tr>
                        </thead>
                        <tbody>
                            {attachments.map(entry => (
                                <tr
                                    key={entry.id}
                                    onClick={() => window.open('http://localosdf/dlskfsjd.pdf', '_blank')}
                                >
                                    <td>{entry.name}</td>
                                    <td>{entry.sizeHumanReadable}</td>
                                    <td>{entry.description}</td>
                                    <td>{entry.created}</td>
                                    <td>{entry.createdByUser}</td>
                                    <td>{entry.lastUpdate}</td>
                                    <td>{entry.lastUpdateByUser}</td>
                                </tr>
                            ))}
                        </tbody>
                    </Table>
                )}
            </DropArea>
        );
    }, [setData]);
}

DynamicAttachmentList.propTypes = {
    id: PropTypes.string.isRequired,
    readOnly: PropTypes.bool,
};

DynamicAttachmentList.defaultProps = {
    readOnly: false,
};

export default DynamicAttachmentList;
