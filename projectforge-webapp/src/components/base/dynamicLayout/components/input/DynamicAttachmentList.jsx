import { faDownload } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React from 'react';
import { evalServiceURL, getServiceURL, handleHTTPErrors } from '../../../../../utilities/rest';
import { Button, Table } from '../../../../design';
import DropArea from '../../../../design/droparea';
import LoadingContainer from '../../../../design/loading-container';
import { DynamicLayoutContext } from '../../context';

function DynamicAttachmentList(
    {
        category,
        id,
        listId,
        serviceBaseUrl,
        reloadUrl,
        restBaseUrl,
        accessString,
        userInfo,
        downloadOnRowClick,
        uploadDisabled,
    },
) {
    const {
        callAction,
        data,
        setData,
        ui,
    } = React.useContext(DynamicLayoutContext);

    const [loading, setLoading] = React.useState(false);

    const { attachments } = data;

    const uploadFile = (files) => {
        setLoading(true);
        const formData = new FormData();
        formData.append('file', files[0]);
        fetch(
            getServiceURL(`${restBaseUrl}/upload/${category}/${id}/${listId}`, {
                accessString,
                userInfo,
            }),
            {
                credentials: 'include',
                method: 'POST',
                body: formData,
            },
        )
            .then(handleHTTPErrors)
            .then((response) => response.json())
            .then((json) => {
                callAction({ responseAction: json });
                setLoading(false);
            })
            .catch((catchError) => {
                alert(catchError);
                setLoading(false);
            });
    };

    const download = (entryId) => {
        callAction({
            responseAction: {
                targetType: 'DOWNLOAD',
                url: getServiceURL(`${restBaseUrl}/download/${category}/${id}`, {
                    fileId: entryId,
                    listId,
                    accessString,
                    userInfo,
                }),
                absolute: true,
            },
        });
    };

    const reload = () => {
        fetch(
            getServiceURL(reloadUrl),
            {
                credentials: 'include',
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    Accept: 'application/json',
                },
                body: JSON.stringify({
                    data: {
                        accessString,
                        userInfo,
                    },
                }),
            },
        )
            .then(handleHTTPErrors)
            .then((response) => response.json())
            .then((json) => {
                callAction({ responseAction: json });
                setLoading(false);
            })
            .catch((catchError) => {
                alert(catchError);
                setLoading(false);
            });
    };

    const handleRowClick = (entry) => (event) => {
        event.stopPropagation();
        if (downloadOnRowClick) {
            download(entry.fileId);
        } else {
            callAction({
                responseAction: {
                    targetType: 'MODAL',
                    url: evalServiceURL(`${serviceBaseUrl}/${id}`, {
                        category,
                        fileId: entry.fileId,
                        listId,
                        accessString,
                        userInfo,
                    }),
                },
            });
        }
    };

    const handleDownload = (entryId) => (event) => {
        event.stopPropagation();
        download(entryId);
    };

    const table = attachments && attachments.length > 0 && (
        <Table striped hover>
            <thead>
                <tr>
                    <th>{ui.translations['attachment.fileName']}</th>
                    <th>{ui.translations['attachment.size']}</th>
                    <th>{ui.translations.description}</th>
                    <th>{ui.translations.created}</th>
                    <th>{ui.translations.createdBy}</th>
                    <th>{ui.translations.modified}</th>
                    <th>{ui.translations.modifiededBy}</th>
                </tr>
            </thead>
            <tbody>
                { attachments.map((entry) => (
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
                        <td>{entry.lastUpdateTimeAgo}</td>
                        <td>{entry.lastUpdateByUser}</td>
                    </tr>
                ))}
            </tbody>
        </Table>
    );

    return React.useMemo(() => {
        if (id && id > 0) {
            if (uploadDisabled) {
                return (<>{table}</>);
            }
            return (
                <LoadingContainer loading={loading}>
                    <DropArea
                        setFiles={uploadFile}
                        noStyle
                        title={ui.translations['file.upload.dropArea']}
                    >
                        {table}
                    </DropArea>
                    {reloadUrl
                        ? (
                            <Button color="link" onClick={reload}>
                                {ui.translations.reload}
                            </Button>
                        )
                        : undefined}
                </LoadingContainer>
            );
        }
        return (
            <>
                {ui.translations['attachment.onlyAvailableAfterSave']}
            </>
        );
    }, [setData, loading, id, attachments]);
}

DynamicAttachmentList.propTypes = {
    category: PropTypes.string.isRequired,
    listId: PropTypes.string.isRequired,
    id: PropTypes.number,
    readOnly: PropTypes.bool,
    serviceBaseUrl: PropTypes.string,
    restBaseUrl: PropTypes.string,
    reloadUrl: PropTypes.string,
    accessString: PropTypes.string,
    userInfo: PropTypes.string,
    downloadOnRowClick: PropTypes.bool,
    uploadDisabled: PropTypes.bool,
};

DynamicAttachmentList.defaultProps = {
    id: undefined, // Undefined for new object.
    readOnly: false,
    serviceBaseUrl: '/react/attachment/dynamic',
    reloadUrl: undefined,
    restBaseUrl: '/rs/attachments',
    accessString: '',
    useInfo: null,
    downloadOnRowClick: false,
    uploadDisabled: false,
};

export default DynamicAttachmentList;
