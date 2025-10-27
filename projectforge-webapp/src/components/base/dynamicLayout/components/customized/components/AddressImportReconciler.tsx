/* eslint-disable max-len */
import React, { useState } from 'react';
import {
    Button, Card, CardBody, Collapse, FormGroup, Label, Input, FormFeedback,
    Alert, Badge, Row, Col, UncontrolledTooltip,
} from 'reactstrap';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faChevronRight } from '@fortawesome/free-solid-svg-icons';
import { useLocation } from 'react-router';
import { fetchJsonPost, getServiceURL } from '../../../../../../utilities/rest';
import { DynamicLayoutContext } from '../../../context';
import AddressFieldSelector from './AddressFieldSelector';

interface AddressImportReconcilerProps {
    values?: {
        title?: string;
        buttonText?: string;
        initiallyCollapsed?: boolean;
    };
}

interface ParsedField {
    value: string;
    confidence?: 'high' | 'medium' | 'low';
    currentValue?: string;
    selected?: boolean;
}

interface ParsedData {
    fields: Record<string, ParsedField>;
    warnings?: string[];
}

interface FieldMappings {
    [key: string]: string;
}

interface AddressBlockMappings {
    business: string;
    private: string;
    postal: string;
    [key: string]: string;
}

interface SelectedFields {
    [key: string]: boolean;
}

interface VcfComparisonDataItem {
    vcf?: string;
    db?: string;
}

interface VcfComparisonData {
    [key: string]: VcfComparisonDataItem;
}

interface ParseResponse {
    variables?: {
        parsedData?: ParsedData;
        data?: any;
        error?: string;
        vcfImageData?: string;
        vcfImageFilename?: string;
        vcfImageMimeType?: string;
    };
}

interface AddressData {
    id?: number | string;
    [key: string]: any;
}

function AddressImportReconciler({ values }: AddressImportReconcilerProps) {
    const {
        ui, setData, data, variables,
    } = React.useContext(DynamicLayoutContext);
    const location = useLocation();
    const {
        title = 'Parse Address from Text',
        buttonText = 'Parse from Text',
        initiallyCollapsed = true,
    } = values || {};

    // Check if we're in VCF import mode
    // IMPORTANT: We check BOTH the URL parameter AND the context variables
    // This prevents stale context data from incorrectly triggering VCF mode
    const urlParams = new URLSearchParams(location.search);
    const hasImportIndexInUrl = urlParams.has('importIndex');

    const vcfComparisonData = variables?.vcfComparisonData as VcfComparisonData | undefined;
    const importIndex = variables?.importIndex;
    const addressId = (data as AddressData)?.id;

    // Only use VCF mode if importIndex is in URL AND vcfComparisonData exists
    const isVcfImportMode = hasImportIndexInUrl && !!vcfComparisonData;

    // Create a unique key that changes when context changes
    // Use URL presence AND vcfComparisonData to determine mode
    const contextKey = React.useMemo(
        () => {
            const mode = isVcfImportMode ? 'vcf' : 'normal';
            const idx = isVcfImportMode ? importIndex : 'noImport';
            return `${addressId || 'new'}_${idx}_${mode}`;
        },
        [addressId, importIndex, isVcfImportMode],
    );

    // Store the previous context key to detect changes
    const prevContextKeyRef = React.useRef<string>(contextKey);

    const [inputText, setInputText] = useState<string>('');
    const [parsedData, setParsedData] = useState<ParsedData | null>(null);
    const [selectedFields, setSelectedFields] = useState<SelectedFields>({});
    const [fieldMappings, setFieldMappings] = useState<FieldMappings>({});
    const [addressBlockMappings, setAddressBlockMappings] = useState<AddressBlockMappings>({
        business: 'business',
        private: 'private',
        postal: 'postal',
    });
    const [loading, setLoading] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);
    const [vcfError, setVcfError] = useState<string | null>(null);
    // Initialize isOpen based on VCF import mode: open if in import mode, otherwise use initiallyCollapsed
    const [isOpen, setIsOpen] = useState<boolean>(isVcfImportMode ? true : !initiallyCollapsed);
    const [dragActive, setDragActive] = useState<boolean>(false);
    const [vcfUploading, setVcfUploading] = useState<boolean>(false);
    const [hasImage, setHasImage] = useState<boolean>(false);
    const [imageTooLarge, setImageTooLarge] = useState<boolean>(false);
    const [applyImage, setApplyImage] = useState<boolean>(false);

    // Helper function to check if a field has a non-empty value
    const isOriginalValueNotBlank = (fieldName: string): boolean => {
        // Check if the field has a non-empty value in the current form data
        const currentValue = (data as AddressData)[fieldName];
        return currentValue != null && String(currentValue).trim() !== '';
    };

    // Reset state completely when context key changes (address or import context changed)
    React.useEffect(() => {
        if (prevContextKeyRef.current !== contextKey) {
            // Context changed - reset all state
            setInputText('');
            setParsedData(null);
            setSelectedFields({});
            setFieldMappings({});
            setAddressBlockMappings({ business: 'business', private: 'private', postal: 'postal' });
            setLoading(false);
            setError(null);
            setVcfError(null);
            setIsOpen(isVcfImportMode ? true : !initiallyCollapsed);
            setHasImage(false);
            setImageTooLarge(false);
            setApplyImage(false);

            prevContextKeyRef.current = contextKey;
        }
    }, [contextKey, isVcfImportMode, initiallyCollapsed]);

    // Load VCF data as parsedData (convert to same format as text parser)
    React.useEffect(() => {
        if (isVcfImportMode && vcfComparisonData) {
            // Convert VCF comparison data to parsedData format
            const fields: Record<string, ParsedField> = {};
            const selected: SelectedFields = {};

            Object.keys(vcfComparisonData).forEach((fieldName) => {
                const vcfValue = vcfComparisonData[fieldName]?.vcf;
                const currentFormValue = (data as AddressData)[fieldName];

                // Normalize values for comparison
                const vcfNormalized = (vcfValue || '').trim();
                const currentNormalized = (typeof currentFormValue === 'string' ? currentFormValue.trim() : String(currentFormValue || '')) || '';

                // Only show fields that are new or changed
                // New: vcfValue exists but current form value is empty/null
                // Changed: vcfValue differs from current form value
                if (vcfNormalized && vcfNormalized !== currentNormalized) {
                    fields[fieldName] = {
                        value: vcfValue || '',
                        confidence: 'high', // VCF data is always high confidence
                        currentValue: currentNormalized,
                    };

                    // Pre-select all fields (since they're all new or changed)
                    selected[fieldName] = true;
                }
            });

            // Special case: Protect name and firstName if they already have values.
            // This prevents accidentally overwriting the name when VCF might be for a different person
            if (isOriginalValueNotBlank('name')) {
                selected.name = false;
            }
            if (isOriginalValueNotBlank('firstName')) {
                selected.firstName = false;
            }

            setParsedData({
                fields,
                warnings: [],
            });
            setSelectedFields(selected);
        }
    }, [isVcfImportMode, vcfComparisonData]);

    const isAddressField = (fieldName: string): boolean => [
        'addressText',
        'addressText2',
        'zipCode',
        'city',
        'state',
        'country',
        'privateAddressText',
        'privateAddressText2',
        'privateZipCode',
        'privateCity',
        'privateState',
        'privateCountry',
        'postalAddressText',
        'postalAddressText2',
        'postalZipCode',
        'postalCity',
        'postalState',
        'postalCountry',
    ].includes(fieldName);

    const getAddressBlockType = (fieldName: string): string => {
        if (fieldName.startsWith('private')) return 'private';
        if (fieldName.startsWith('postal')) return 'postal';
        return 'business';
    };

    const getBaseFieldName = (fieldName: string): string => {
        // Extract base field name without prefix: "privateCity" → "city", "postalZipCode" → "zipCode"
        if (fieldName.startsWith('private')) {
            const withoutPrivate = fieldName.substring(7); // Remove "private"
            return withoutPrivate.charAt(0).toLowerCase() + withoutPrivate.substring(1); // lowercase first char
        }
        if (fieldName.startsWith('postal')) {
            const withoutPostal = fieldName.substring(6); // Remove "postal"
            return withoutPostal.charAt(0).toLowerCase() + withoutPostal.substring(1); // lowercase first char
        }
        return fieldName; // Already base name
    };

    const buildTargetFieldName = (baseFieldName: string, targetBlockType: string): string => {
        // Build target field name: "city" + "postal" → "postalCity"
        if (targetBlockType === 'private') {
            return `private${baseFieldName.charAt(0).toUpperCase()}${baseFieldName.substring(1)}`;
        }
        if (targetBlockType === 'postal') {
            return `postal${baseFieldName.charAt(0).toUpperCase()}${baseFieldName.substring(1)}`;
        }
        return baseFieldName; // business uses base name
    };

    const handleToggle = () => {
        setIsOpen(!isOpen);
    };

    const handleParse = () => {
        if (!inputText.trim()) {
            setError('Please enter text to parse');
            return;
        }

        setLoading(true);
        setError(null);

        // Reset manual mappings before parsing new text
        setFieldMappings({});
        setAddressBlockMappings({ business: 'business', private: 'private', postal: 'postal' });

        fetchJsonPost(
            'address/parseText',
            { data: { inputText } },
            (json: ParseResponse) => {
                setLoading(false);
                if (json && json.variables && json.variables.parsedData) {
                    const parsed = json.variables.parsedData;

                    // Filter out fields that already match current form values
                    const filteredFields: Record<string, ParsedField> = {};
                    if (parsed.fields) {
                        Object.entries(parsed.fields).forEach(([fieldName, field]) => {
                            const currentValue = (data as AddressData)[fieldName];
                            const parsedValue = (field as ParsedField).value;

                            // Only include field if value is different from current
                            const currentNormalized = (typeof currentValue === 'string' ? currentValue.trim() : String(currentValue || '')) || '';
                            const parsedNormalized = parsedValue?.trim() || '';

                            if (parsedNormalized && currentNormalized !== parsedNormalized) {
                                filteredFields[fieldName] = field as ParsedField;
                            }
                        });
                    }

                    const parsedWithFilteredFields = {
                        ...parsed,
                        fields: filteredFields,
                    };
                    setParsedData(parsedWithFilteredFields);

                    // Initialize selected fields based on filtered parsed data
                    const selected: SelectedFields = {};
                    const nameFields = ['name', 'firstName']; // Name fields that need special handling

                    Object.entries(filteredFields).forEach(([fieldName]) => {
                        // Protect name and firstName only if they already have values
                        // This prevents accidentally overwriting existing names, but allows setting them for new addresses
                        if (nameFields.includes(fieldName) && isOriginalValueNotBlank(fieldName)) {
                            selected[fieldName] = false; // Don't pre-select name fields that already have a value
                        } else {
                            selected[fieldName] = true; // Pre-select all freshly parsed fields
                        }
                    });
                    setSelectedFields(selected);
                } else {
                    setError('Failed to parse text. Please check the format.');
                }
            },
        );
    };

    const handleFieldToggle = (fieldName: string) => {
        setSelectedFields({
            ...selectedFields,
            [fieldName]: !selectedFields[fieldName],
        });
    };

    const handleFieldMappingChange = (fieldName: string, newMapping: string) => {
        setFieldMappings({
            ...fieldMappings,
            [fieldName]: newMapping,
        });
    };

    const handleAddressBlockMappingChange = (blockType: string, newMapping: string) => {
        setAddressBlockMappings({
            ...addressBlockMappings,
            [blockType]: newMapping,
        });
    };

    const handleSelectAll = () => {
        const newSelected: SelectedFields = {};
        Object.keys(parsedData?.fields || {}).forEach((fieldName) => {
            // Protect name and firstName only if they already have values
            if ((fieldName === 'name' || fieldName === 'firstName') && isOriginalValueNotBlank(fieldName)) {
                // Preserve current state for name/firstName when they have original values
                newSelected[fieldName] = selectedFields[fieldName] || false;
            } else {
                // Select all other fields, including name/firstName if they are empty
                newSelected[fieldName] = true;
            }
        });
        setSelectedFields(newSelected);
        // Also select image checkbox if available and not too large
        if (hasImage && !imageTooLarge) {
            setApplyImage(true);
        }
    };

    const handleUnselectAll = () => {
        const newSelected: SelectedFields = {};
        Object.keys(parsedData?.fields || {}).forEach((fieldName) => {
            newSelected[fieldName] = false;
        });
        setSelectedFields(newSelected);
        // Also unselect image checkbox if present
        setApplyImage(false);
    };

    const handleVcfUpload = (file: File) => {
        setVcfUploading(true);
        setVcfError(null);

        // Reset manual mappings before uploading VCF
        setFieldMappings({});
        setAddressBlockMappings({ business: 'business', private: 'private', postal: 'postal' });

        const formData = new FormData();
        formData.append('file', file);
        formData.append('addressId', String((data as AddressData).id || '0'));

        fetch(getServiceURL('address/parseVcf'), {
            method: 'POST',
            credentials: 'include',
            body: formData,
        })
            .then((response) => response.json())
            .then((json) => {
                setVcfUploading(false);

                if (json.variables?.error) {
                    setVcfError(json.variables.error);
                    return;
                }

                if (json.variables?.parsedData) {
                    const parsed = json.variables.parsedData;
                    setParsedData(parsed);

                    // Handle image data from VCF
                    if (json.variables.hasImage) {
                        setHasImage(true);
                        setImageTooLarge(json.variables.imageTooLarge || false);
                        // Pre-check the apply image checkbox if image is not too large
                        setApplyImage(!json.variables.imageTooLarge);
                    } else {
                        setHasImage(false);
                        setImageTooLarge(false);
                        setApplyImage(false);
                    }

                    // Pre-select all fields
                    const selected: SelectedFields = {};
                    Object.keys(parsed.fields || {}).forEach((fieldName) => {
                        selected[fieldName] = true;
                    });

                    // Special case: Protect name and firstName if they already have values.
                    // This prevents accidentally overwriting the name when VCF might be for a different person
                    if (isOriginalValueNotBlank('name')) {
                        selected.name = false;
                    }
                    if (isOriginalValueNotBlank('firstName')) {
                        selected.firstName = false;
                    }

                    setSelectedFields(selected);
                } else {
                    setVcfError(ui.translations['address.book.vCardsImport.error.parsing'] || 'Failed to parse VCF file');
                }
            })
            .catch(() => {
                setVcfUploading(false);
                setVcfError(ui.translations['address.book.vCardsImport.error.parsing'] || 'Failed to parse VCF file');
            });
    };

    const handleVcfDrop = (e: React.DragEvent<HTMLDivElement>) => {
        e.preventDefault();
        setDragActive(false);

        const file = e.dataTransfer.files[0];
        if (!file) return;

        // Check if file is VCF
        if (!file.name.toLowerCase().endsWith('.vcf')) {
            setVcfError(ui.translations['address.book.vCardsImport.wrongFileType'] || 'Please drop a VCF file');
            return;
        }

        handleVcfUpload(file);
    };

    const getDropAreaBorderStyle = (): string => {
        if (vcfError) {
            return '2px solid #dc3545'; // Red border on error
        }
        if (dragActive) {
            return '2px dashed #007bff'; // Blue border when dragging
        }
        return '2px dashed #ccc'; // Default gray border
    };

    const handleApply = () => {
        // Apply parsed fields (works for both text parser and VCF import)
        if (!parsedData || !parsedData.fields) return;

        // Build map of selected fields with their values and apply remapping
        const fieldsToApply: Record<string, string> = {};
        Object.entries(parsedData.fields).forEach(([fieldName, field]) => {
            if (selectedFields[fieldName] && field.value) {
                let targetFieldName = fieldName;

                // Apply address block remapping
                if (isAddressField(fieldName)) {
                    const sourceBlockType = getAddressBlockType(fieldName);
                    const targetBlockType = addressBlockMappings[sourceBlockType as keyof AddressBlockMappings] || sourceBlockType;
                    const baseFieldName = getBaseFieldName(fieldName);
                    targetFieldName = buildTargetFieldName(baseFieldName, targetBlockType);
                } else {
                    // Apply individual field remapping (phone/email)
                    targetFieldName = fieldMappings[fieldName] || fieldName;
                }

                fieldsToApply[targetFieldName] = field.value;
            }
        });

        fetchJsonPost(
            'address/applyParsedData',
            {
                address: data,
                selectedFields: fieldsToApply,
                applyImage: hasImage && applyImage, // Only send true if VCF has image AND checkbox is checked
            },
            (json: ParseResponse) => {
                if (json && json.variables && json.variables.data) {
                    // Update form data with parsed values
                    (setData as any)(json.variables.data);

                    // Handle VCF image upload if present
                    if (json.variables.vcfImageData && json.variables.vcfImageFilename) {
                        // Decode Base64 to binary
                        const base64Data = json.variables.vcfImageData as string;
                        const binaryString = atob(base64Data);
                        const bytes = new Uint8Array(binaryString.length);
                        for (let i = 0; i < binaryString.length; i += 1) {
                            bytes[i] = binaryString.charCodeAt(i);
                        }

                        // Use correct MIME type from backend
                        const mimeType = json.variables.vcfImageMimeType as string || 'image/png';

                        // Determine correct file extension from MIME type
                        let extension = 'png';
                        if (mimeType === 'image/jpeg') {
                            extension = 'jpg';
                        } else if (mimeType === 'image/gif') {
                            extension = 'gif';
                        }

                        // Create filename with correct extension (backend validates by extension)
                        const filename = `vcf-image.${extension}`;

                        // Create Blob from bytes with correct MIME type
                        const blob = new Blob([bytes], { type: mimeType });
                        const file = new File([blob], filename, { type: mimeType });

                        // Create Data URL for immediate display
                        const reader = new FileReader();
                        reader.onload = (e) => {
                            // Set the image data URL (for immediate display like manual upload)
                            (setData as any)({
                                hasUploadedImage: true,
                                imageDataUrl: e.target?.result,
                                imageDeleted: false,
                            });
                        };
                        reader.readAsDataURL(blob);

                        // Upload image to session (same as manual upload in CustomizedAddressImage)
                        const formData = new FormData();
                        formData.append('file', file);

                        fetch(getServiceURL('address/uploadImage/-1'), {
                            credentials: 'include',
                            method: 'POST',
                            body: formData,
                        })
                            .catch(() => {
                                // Silently ignore upload errors - image is already displayed via Data URL
                            });
                    }

                    // Close collapse and reset state after applying
                    setIsOpen(false);
                    if (!isVcfImportMode) {
                        setInputText('');
                        setParsedData(null);
                        setSelectedFields({});
                        setFieldMappings({});
                        setAddressBlockMappings({ business: 'business', private: 'private', postal: 'postal' });
                    }
                } else {
                    setError('Error applying data');
                }
            },
        );
    };

    return (
        <div className="address-text-parser mb-3">
            <Button
                id="address-text-parser-btn"
                color="warning"
                onClick={handleToggle}
                className="mb-2"
            >
                <FontAwesomeIcon
                    icon={faChevronRight}
                    style={{
                        transform: isOpen ? 'rotate(90deg)' : 'rotate(0deg)',
                        transition: 'transform 0.3s cubic-bezier(0.25, 0.8, 0.25, 1)',
                        marginRight: '8px',
                    }}
                />
                {buttonText}
            </Button>
            <UncontrolledTooltip
                placement="top"
                target="address-text-parser-btn"
            >
                {ui.translations['address.parseText.button.tooltip'] || 'Paste email signatures or other text, or drop a VCard file to import address data'}
            </UncontrolledTooltip>

            <Collapse isOpen={isOpen}>
                <Card>
                    <CardBody>
                        <h5>{title}</h5>

                        {/* VCF Import Mode: Show info message */}
                        {isVcfImportMode && (
                            <Alert color="info" className="mb-3">
                                {ui.translations['address.book.vCardsImport.dataLoaded']
                                    || 'VCF data has been loaded. Select the fields you want to apply.'}
                            </Alert>
                        )}

                        {/* Text Parser Mode: Show text input and parse button */}
                        {!isVcfImportMode && (
                            <>
                                <Row>
                                    <Col md={8}>
                                        <FormGroup>
                                            <Label for="addressTextInput">
                                                {ui.translations['address.parseText.inputLabel']}
                                            </Label>
                                            <Input
                                                type="textarea"
                                                id="addressTextInput"
                                                rows={10}
                                                value={inputText}
                                                onChange={(e) => setInputText(e.target.value)}
                                                placeholder={
                                                    (ui.translations['address.parseText.inputPlaceholder'] || '')
                                                        .replace(/\\n/g, '\n')
                                                }
                                                invalid={!!error && !parsedData}
                                            />
                                            {error && !parsedData && <FormFeedback>{error}</FormFeedback>}
                                        </FormGroup>
                                    </Col>
                                    <Col md={4}>
                                        <Label>
                                            {ui.translations['address.book.vCardsImport.dropLabel'] || 'Or drop VCF file'}
                                        </Label>
                                        <div
                                            onDrop={handleVcfDrop}
                                            onDragOver={(e) => e.preventDefault()}
                                            onDragEnter={() => setDragActive(true)}
                                            onDragLeave={() => setDragActive(false)}
                                            style={{
                                                border: getDropAreaBorderStyle(),
                                                borderRadius: '8px',
                                                padding: '20px',
                                                textAlign: 'center',
                                                backgroundColor: dragActive ? '#f0f8ff' : '#f9f9f9',
                                                minHeight: '120px',
                                                cursor: 'pointer',
                                                display: 'flex',
                                                flexDirection: 'column',
                                                justifyContent: 'center',
                                                alignItems: 'center',
                                            }}
                                        >
                                            <i className="fa fa-upload fa-3x mb-2" style={{ color: '#6c757d' }} />
                                            <p className="mb-1">{ui.translations['address.book.vCardsImport.dropHint'] || 'Drop VCF file here'}</p>
                                            <small className="text-muted">
                                                {ui.translations['address.book.vCardsImport.dropInfo'] || 'Matching address will be selected automatically'}
                                            </small>
                                        </div>
                                        {vcfUploading && (
                                            <Alert color="info" className="mt-2">
                                                <i className="fa fa-spinner fa-spin mr-2" />
                                                {ui.translations['address.book.vCardsImport.uploading'] || 'Parsing VCF file...'}
                                            </Alert>
                                        )}
                                        {vcfError && (
                                            <Alert color="danger" className="mt-2">
                                                {vcfError}
                                            </Alert>
                                        )}
                                    </Col>
                                </Row>

                                <Button
                                    color="primary"
                                    outline
                                    onClick={handleParse}
                                    disabled={!inputText.trim() || loading}
                                >
                                    {loading
                                        ? `${ui.translations.parse || 'Parse'}...`
                                        : ui.translations.parse || 'Parse'}
                                </Button>
                            </>
                        )}

                        {/* Show parsed data (works for both text parser and VCF import) */}
                        {parsedData && (
                            <div className="mt-4">
                                {/* Heading - only show in text parser mode */}
                                {!isVcfImportMode && (
                                    <h4>
                                        {ui.translations['address.parseText.fieldsParsed']}
                                    </h4>
                                )}

                                {parsedData.warnings && parsedData.warnings.length > 0 && (
                                    <Alert color="warning" className="mt-2">
                                        <strong>Warnings:</strong>
                                        <ul className="mb-0 mt-2">
                                            {parsedData.warnings.map((warning) => (
                                                <li key={warning}>{warning}</li>
                                            ))}
                                        </ul>
                                    </Alert>
                                )}

                                {/* Select All / Unselect All controls */}
                                <div className="mb-2">
                                    <button
                                        type="button"
                                        onClick={handleSelectAll}
                                        style={{
                                            background: 'none',
                                            border: 'none',
                                            padding: 0,
                                            color: '#007bff',
                                            textDecoration: 'underline',
                                            cursor: 'pointer',
                                            marginRight: '10px',
                                        }}
                                    >
                                        {ui.translations['common.import.action.selectAll'] || 'Select all'}
                                    </button>
                                    <span style={{ color: '#6c757d' }}>|</span>
                                    <button
                                        type="button"
                                        onClick={handleUnselectAll}
                                        style={{
                                            background: 'none',
                                            border: 'none',
                                            padding: 0,
                                            color: '#007bff',
                                            textDecoration: 'underline',
                                            cursor: 'pointer',
                                            marginLeft: '10px',
                                        }}
                                    >
                                        {ui.translations['common.import.action.deselectAll'] || 'Unselect all'}
                                    </button>
                                </div>

                                <div className="mt-3">
                                    <AddressFieldSelector
                                        fields={parsedData.fields}
                                        currentData={data}
                                        selectedFields={selectedFields}
                                        onFieldToggle={handleFieldToggle}
                                        fieldMappings={fieldMappings}
                                        onFieldMappingChange={handleFieldMappingChange}
                                        addressBlockMappings={addressBlockMappings}
                                        onAddressBlockMappingChange={handleAddressBlockMappingChange}
                                        translations={ui.translations}
                                        showConfidence
                                        showComparison
                                        highlightNameFields={(data as AddressData).id != null}
                                    />
                                </div>

                                {/* VCF Image Import Section - only show in VCF mode when image exists */}
                                {hasImage && (
                                    <div className="mt-3">
                                        {imageTooLarge ? (
                                            <Alert color="warning">
                                                {ui.translations['address.book.vCardsImport.imageTooLarge'] || 'Bild zu groß'}
                                            </Alert>
                                        ) : (
                                            <div>
                                                <div className="mt-2 mb-2">
                                                    <img
                                                        src={getServiceURL('address/vcfImagePreview')}
                                                        alt="VCF Preview"
                                                        style={{ maxWidth: '100px', maxHeight: '100px', border: '1px solid #ccc' }}
                                                    />
                                                </div>
                                                <FormGroup check className="mb-2">
                                                    <Label check>
                                                        <Input
                                                            type="checkbox"
                                                            checked={applyImage}
                                                            onChange={(e) => setApplyImage(e.target.checked)}
                                                        />
                                                        {' '}
                                                        {ui.translations['address.book.vCardsImport.importImage'] || 'Bild importieren'}
                                                    </Label>
                                                </FormGroup>
                                            </div>
                                        )}
                                    </div>
                                )}

                                <div className="mt-3">
                                    <Button
                                        color="success"
                                        onClick={handleApply}
                                        className="mr-2"
                                    >
                                        {ui.translations.apply || 'Apply'}
                                    </Button>
                                    {/* Cancel button - only show in text parser mode */}
                                    {!isVcfImportMode && (
                                        <Button
                                            color="secondary"
                                            onClick={() => {
                                                setParsedData(null);
                                                setSelectedFields({});
                                                setError(null);
                                                setVcfError(null);
                                                setFieldMappings({});
                                                setAddressBlockMappings({ business: 'business', private: 'private', postal: 'postal' });
                                                setIsOpen(false);
                                            }}
                                        >
                                            {ui.translations.cancel || 'Cancel'}
                                        </Button>
                                    )}
                                </div>

                                {/* Confidence legend - only show if there are fields with confidence values */}
                                {parsedData.fields && Object.values(parsedData.fields).some((f) => typeof f === 'object' && f.confidence) && (
                                    <div className="mt-3" style={{ fontSize: '0.85em', color: '#6c757d' }}>
                                        <strong>
                                            {ui.translations['address.parseText.confidence.legend'] || 'Confidence levels'}
                                            :
                                        </strong>
                                        {' '}
                                        <Badge color="success" className="ml-2">HIGH</Badge>
                                        {' '}
                                        {ui.translations['address.parseText.confidence.high'] || 'High confidence - value is very likely correct'}
                                        {' / '}
                                        <Badge color="warning" className="ml-2">MEDIUM</Badge>
                                        {' '}
                                        {ui.translations['address.parseText.confidence.medium'] || 'Medium confidence - please verify'}
                                        {' / '}
                                        <Badge color="danger" className="ml-2">LOW</Badge>
                                        {' '}
                                        {ui.translations['address.parseText.confidence.low'] || 'Low confidence - manual review recommended'}
                                    </div>
                                )}
                            </div>
                        )}

                        {/* Error alert (text parser mode only) */}
                        {!isVcfImportMode && error && parsedData && (
                            <Alert color="danger" className="mt-3">
                                {error}
                            </Alert>
                        )}
                    </CardBody>
                </Card>
            </Collapse>
        </div>
    );
}

export default AddressImportReconciler;
