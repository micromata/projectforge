import PropTypes from 'prop-types';
import React, { useState } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faDownload, faEdit, faLock } from '@fortawesome/free-solid-svg-icons';
import { evalServiceURL, getServiceURL } from '../../../../../utilities/rest';
import { MultipleFileUploadArea } from '../upload/MultipleFileUploadArea';
import { DynamicLayoutContext } from '../../context';
import DynamicAgGrid from '../table/DynamicAgGrid';
import DynamicAlert from '../DynamicAlert';
import DynamicButton from '../DynamicButton';

function DynamicAttachmentList(props) {
    const {
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
        locale,
        dateFormat,
        thousandSeparator,
        decimalSeparator,
        timestampFormatSeconds,
        timestampFormatMinutes,
        currency,
    } = props;

    const {
        callAction,
        data,
        setData,
        ui,
    } = React.useContext(DynamicLayoutContext);

    const [gridApi, setGridApi] = useState();

    const onGridApiReady = React.useCallback((api) => {
        setGridApi(api);
    }, []);

    const { attachments } = data;
    const { translations } = ui;

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

    function Action(param) {
        const { data: entry } = param; // Entry
        return (
            <>
                <span
                    role="presentation"
                    onKeyDown={() => undefined}
                    ref={(ref) => {
                        if (!ref) return;
                        // eslint-disable-next-line no-param-reassign
                        ref.onclick = (event) => {
                            download(entry.fileId);
                            event.stopPropagation(); // works not span.onclick :-(
                        };
                    }}
                >
                    <FontAwesomeIcon icon={faDownload} />
                </span>
                {!downloadOnRowClick
                    && (
                        <span className="ml-2">
                            <FontAwesomeIcon icon={faEdit} />
                        </span>
                    )}
            </>
        );
    }

    function Filename(param) {
        const { data: entry } = param; // Entry
        return (
            <>
                {`${entry.name} `}
                {entry.encrypted
                        && (
                            <FontAwesomeIcon icon={faLock} />
                        )}
            </>
        );
    }

    const afterFileUpload = (response) => {
        const json = JSON.parse(response);
        callAction({ responseAction: json });
    };

    const handleDownloadSelectedClick = React.useCallback(() => {
        const selectedIds = gridApi.getSelectedRows().map((item) => item.fileId);
        if (selectedIds.length === 0) {
            return; // Do nothing, no rows selected.
        }
        callAction({
            responseAction: {
                targetType: 'DOWNLOAD',
                url: getServiceURL(`${restBaseUrl}/multiDownload/${category}/${id}`, {
                    fileIds: selectedIds.map((fileId) => String.truncate(fileId, 4)).join(),
                    listId,
                }),
                absolute: true,
            },
        });
    }, [gridApi]);

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

    const handleDeleteSelectedClick = React.useCallback(() => {
        const selectedIds = gridApi.getSelectedRows().map((item) => item.fileId);
        if (selectedIds.length === 0) {
            return; // Do nothing, no rows selected.
        }
        callAction({
            responseAction: {
                targetType: 'POST',
                url: `${restBaseUrl}/multiDelete`,
                myData: {
                    category,
                    id,
                    fileIds: selectedIds,
                    listId,
                },
                absolute: true,
            },
        });
    }, [gridApi]);

    const table = attachments && attachments.length > 0 && (
        <>
            <DynamicAgGrid
                {...agGrid}
                onGridApiReady={onGridApiReady}
                columnDefs={agGrid.columnDefs}
                id="attachments"
                rowClickFunction={handleRowClick}
                rowSelection="multiple"
                suppressRowClickSelection
                components={{
                    action: Action,
                    filename: Filename,
                }}
                locale={locale}
                dateFormat={dateFormat}
                thousandSeparator={thousandSeparator}
                decimalSeparator={decimalSeparator}
                timestampFormatSeconds={timestampFormatSeconds}
                timestampFormatMinutes={timestampFormatMinutes}
                currency={currency}
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
                handleButtonClick={handleDeleteSelectedClick}
            />
            <DynamicButton
                id="downloadSelected"
                color="success"
                outline
                title={translations['file.upload.downloadSelected']}
                handleButtonClick={handleDownloadSelectedClick}
            />
        </>
    );

    return React.useMemo(() => {
        if (id && id > 0) {
            if (readOnly || uploadDisabled) {
                return table;
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
        return ui.translations['attachment.onlyAvailableAfterSave'];
    }, [setData, id, attachments, handleDeleteSelectedClick]);
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
    locale: PropTypes.string,
    dateFormat: PropTypes.string,
    thousandSeparator: PropTypes.string,
    decimalSeparator: PropTypes.string,
    timestampFormatSeconds: PropTypes.string,
    timestampFormatMinutes: PropTypes.string,
    currency: PropTypes.string,
};

DynamicAttachmentList.defaultProps = {
    id: undefined, // Undefined for new object.
    readOnly: false,
    serviceBaseUrl: '/react/attachment/dynamic',
    restBaseUrl: '/rs/attachments',
    downloadOnRowClick: false,
    uploadDisabled: false,
    maxSizeInKB: 1000000, // 1 MB at default
    locale: undefined,
    dateFormat: undefined,
    thousandSeparator: undefined,
    decimalSeparator: undefined,
    timestampFormatSeconds: 'YYYY-MM-dd HH:mm:ss',
    timestampFormatMinutes: 'YYYY-MM-dd HH:mm',
    currency: '€',
};

export default DynamicAttachmentList;
