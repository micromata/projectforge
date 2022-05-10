import PropTypes from 'prop-types';
import React from 'react';
import { evalServiceURL, getServiceURL } from '../../../../../utilities/rest';
import { MultipleFileUploadArea } from '../upload/MultipleFileUploadArea';
import { DynamicLayoutContext } from '../../context';
import DynamicAgGrid from '../table/DynamicAgGrid';
import DynamicAlert from '../DynamicAlert';
import DynamicButton from '../DynamicButton';

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
        maxSizeInKB,
        agGrid,
    },
) {
    const {
        callAction,
        data,
        setData,
        ui,
    } = React.useContext(DynamicLayoutContext);

    const { attachments } = data;
    const { translations } = ui;

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

    const handleRowClick = (event) => {
        const entry = event.data;
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

    /*
    const handleDownload = (entryId) => (event) => {
        event.stopPropagation();
        download(entryId);
    }; */

    const table = attachments && attachments.length > 0 && (
        <>
            <DynamicAgGrid
                columnDefs={agGrid.columnDefs}
                id="attachments"
                rowClickFunction={handleRowClick}
                rowSelection="multiple"
                suppressRowClickSelection="true"
            />
            <DynamicAlert
                markdown
                color="info"
                title={translations['multiselection.aggrid.selection.info.title']}
                message={translations['multiselection.aggrid.selection.info.message']}
            />
            <DynamicButton
                id="deleteSelected"
                color="danger"
                confirmMessage={translations['file.upload.deleteSelected.confirm']}
                outline
                title={translations['file.upload.deleteSelected']}
            />
            <DynamicButton
                id="downloadSelected"
                color="success"
                outline
                title={translations['file.upload.downloadSelected']}
            />
        </>
        /*
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
                    </tr>
                ))}
            </tbody>
        </Table> */
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
                        maxSizeInKB={maxSizeInKB}
                        existingFiles={attachments}
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
    maxSizeInKB: PropTypes.number,
};

DynamicAttachmentList.defaultProps = {
    id: undefined, // Undefined for new object.
    readOnly: false,
    serviceBaseUrl: '/react/attachment/dynamic',
    restBaseUrl: '/rs/attachments',
    downloadOnRowClick: false,
    uploadDisabled: false,
    maxSizeInKB: 1000000, // 1 MB at default
};

export default DynamicAttachmentList;
