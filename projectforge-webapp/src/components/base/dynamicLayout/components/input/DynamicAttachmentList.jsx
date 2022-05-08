import { faDownload, faEdit, faLock } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React from 'react';
import { evalServiceURL, getServiceURL } from '../../../../../utilities/rest';
import { Table } from '../../../../design';
import { MultipleFileUploadArea } from '../upload/MultipleFileUploadArea';
import { DynamicLayoutContext } from '../../context';

function DynamicAttachmentList(
    {
        category,
        id,
        listId,
        readOnly,
        serviceBaseUrl,
        restBaseUrl,
        downloadOnRowClick,
        uploadDisabled,
        showExpiryInfo,
    },
) {
    const {
        callAction,
        data,
        setData,
        ui,
    } = React.useContext(DynamicLayoutContext);

    const { attachments } = data;

    const afterFileUpload = (response) => {
        const json = JSON.parse(response);
        callAction({ responseAction: json });
    };

    const download = (entryId) => {
        callAction({
            responseAction: {
                targetType: 'DOWNLOAD',
                url: getServiceURL(`${restBaseUrl}/download/${category}/${id}`, {
                    fileId: entryId,
                    listId,
                }),
                absolute: true,
            },
        });
    };

    const handleRowClick = (entry) => (event) => {
        event.stopPropagation();
        if (readOnly || downloadOnRowClick) {
            download(entry.fileId);
        } else {
            callAction({
                responseAction: {
                    targetType: 'MODAL',
                    url: evalServiceURL(`${serviceBaseUrl}/${id}`, {
                        category,
                        fileId: entry.fileId,
                        listId,
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
                    {showExpiryInfo === true
                    && (
                        <th>{ui.translations['attachment.expires']}</th>
                    )}
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
                                {entry.encrypted
                                && (
                                    <FontAwesomeIcon icon={faLock} />
                                )}
                                {`${entry.name} `}
                                <FontAwesomeIcon icon={faDownload} />
                            </span>
                            {!downloadOnRowClick
                            && (
                                <span className="ml-2">
                                    <FontAwesomeIcon icon={faEdit} />
                                </span>
                            )}
                        </td>
                        <td>{entry.sizeHumanReadable}</td>
                        <td>{entry.description}</td>
                        {showExpiryInfo
                        && (
                            <td>{(entry.info && entry.info.expiryInfo) ? entry.info.expiryInfo : ''}</td>
                        )}
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
            if (readOnly || uploadDisabled) {
                return (<>{table}</>);
            }
            return (
                <>
                    <MultipleFileUploadArea
                        url={getServiceURL(`${restBaseUrl}/upload/${category}/${id}/${listId}`)}
                        // noStyle
                        title={ui.translations['attachment.upload.title']}
                        afterFileUpload={afterFileUpload}
                    />
                    {table}
                </>
            );
        }
        return (
            <>
                {ui.translations['attachment.onlyAvailableAfterSave']}
            </>
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
    downloadOnRowClick: PropTypes.bool,
    uploadDisabled: PropTypes.bool,
    showExpiryInfo: PropTypes.bool,
};

DynamicAttachmentList.defaultProps = {
    id: undefined, // Undefined for new object.
    readOnly: false,
    serviceBaseUrl: '/react/attachment/dynamic',
    restBaseUrl: '/rs/attachments',
    downloadOnRowClick: false,
    uploadDisabled: false,
    showExpiryInfo: undefined,
};

export default DynamicAttachmentList;
