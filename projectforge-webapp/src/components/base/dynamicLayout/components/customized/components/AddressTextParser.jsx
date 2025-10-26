/* eslint-disable max-len */
import React, { useState } from 'react';
import PropTypes from 'prop-types';
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

function AddressTextParser({ values }) {
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

    const vcfComparisonData = variables?.vcfComparisonData;
    const importIndex = variables?.importIndex;
    const addressId = data?.id;

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
    const prevContextKeyRef = React.useRef(contextKey);

    const [inputText, setInputText] = useState('');
    const [parsedData, setParsedData] = useState(null);
    const [selectedFields, setSelectedFields] = useState({});
    const [fieldMappings, setFieldMappings] = useState({});
    const [addressBlockMappings, setAddressBlockMappings] = useState({
        business: 'business',
        private: 'private',
        postal: 'postal',
    });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [vcfError, setVcfError] = useState(null);
    const [isOpen, setIsOpen] = useState(!initiallyCollapsed);
    const [dragActive, setDragActive] = useState(false);
    const [vcfUploading, setVcfUploading] = useState(false);

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

            prevContextKeyRef.current = contextKey;
        }
    }, [contextKey, isVcfImportMode, initiallyCollapsed]);

    // Load VCF data as parsedData (convert to same format as text parser)
    React.useEffect(() => {
        if (isVcfImportMode && vcfComparisonData) {
            // Convert VCF comparison data to parsedData format
            const fields = {};
            const selected = {};

            Object.keys(vcfComparisonData).forEach((fieldName) => {
                const vcfValue = vcfComparisonData[fieldName]?.vcf;
                const dbValue = vcfComparisonData[fieldName]?.db;

                // Only show fields that are new or changed
                // New: vcfValue exists but dbValue is empty/null
                // Changed: vcfValue differs from dbValue
                if (vcfValue && vcfValue !== dbValue) {
                    fields[fieldName] = {
                        value: vcfValue,
                        confidence: 'high', // VCF data is always high confidence
                        currentValue: dbValue || '',
                    };

                    // Pre-select all fields (since they're all new or changed)
                    selected[fieldName] = true;
                }
            });

            setParsedData({
                fields,
                warnings: [],
            });
            setSelectedFields(selected);
        }
    }, [isVcfImportMode, vcfComparisonData]);

    const isAddressField = (fieldName) => [
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

    const getAddressBlockType = (fieldName) => {
        if (fieldName.startsWith('private')) return 'private';
        if (fieldName.startsWith('postal')) return 'postal';
        return 'business';
    };

    const getBaseFieldName = (fieldName) => {
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

    const buildTargetFieldName = (baseFieldName, targetBlockType) => {
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
            (json) => {
                setLoading(false);
                if (json && json.variables && json.variables.parsedData) {
                    const parsed = json.variables.parsedData;

                    // Filter out fields that already match current form values
                    const filteredFields = {};
                    if (parsed.fields) {
                        Object.entries(parsed.fields).forEach(([fieldName, field]) => {
                            const currentValue = data[fieldName];
                            const parsedValue = field.value;

                            // Only include field if value is different from current
                            const currentNormalized = currentValue?.trim() || '';
                            const parsedNormalized = parsedValue?.trim() || '';

                            if (parsedNormalized && currentNormalized !== parsedNormalized) {
                                filteredFields[fieldName] = field;
                            }
                        });
                    }

                    const parsedWithFilteredFields = {
                        ...parsed,
                        fields: filteredFields,
                    };
                    setParsedData(parsedWithFilteredFields);

                    // Initialize selected fields based on filtered parsed data
                    const selected = {};
                    const isExistingAddress = data.id != null; // Check if this is an existing address
                    const nameFields = ['name', 'firstName']; // Name fields that need special handling

                    Object.entries(filteredFields).forEach(([fieldName, field]) => {
                        // For existing addresses: Do NOT pre-select name fields (safety measure)
                        // For new addresses: Pre-select all fields as before
                        if (isExistingAddress && nameFields.includes(fieldName)) {
                            selected[fieldName] = false; // Don't pre-select name fields for existing addresses
                        } else {
                            selected[fieldName] = field.selected !== false;
                        }
                    });
                    setSelectedFields(selected);
                } else {
                    setError('Failed to parse text. Please check the format.');
                }
            },
        );
    };

    const handleFieldToggle = (fieldName) => {
        setSelectedFields({
            ...selectedFields,
            [fieldName]: !selectedFields[fieldName],
        });
    };

    const handleFieldMappingChange = (fieldName, newMapping) => {
        setFieldMappings({
            ...fieldMappings,
            [fieldName]: newMapping,
        });
    };

    const handleAddressBlockMappingChange = (blockType, newMapping) => {
        setAddressBlockMappings({
            ...addressBlockMappings,
            [blockType]: newMapping,
        });
    };

    const handleVcfUpload = (file) => {
        setVcfUploading(true);
        setVcfError(null);

        // Reset manual mappings before uploading VCF
        setFieldMappings({});
        setAddressBlockMappings({ business: 'business', private: 'private', postal: 'postal' });

        const formData = new FormData();
        formData.append('file', file);
        formData.append('addressId', data.id || '0');

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

                    // Pre-select all fields
                    const selected = {};
                    Object.keys(parsed.fields || {}).forEach((fieldName) => {
                        selected[fieldName] = true;
                    });
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

    const handleVcfDrop = (e) => {
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

    const getDropAreaBorderStyle = () => {
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
        const fieldsToApply = {};
        Object.entries(parsedData.fields).forEach(([fieldName, field]) => {
            if (selectedFields[fieldName] && field.value) {
                let targetFieldName = fieldName;

                // Apply address block remapping
                if (isAddressField(fieldName)) {
                    const sourceBlockType = getAddressBlockType(fieldName);
                    const targetBlockType = addressBlockMappings[sourceBlockType] || sourceBlockType;
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
            },
            (json) => {
                if (json && json.variables && json.variables.data) {
                    // Update form data with parsed values
                    setData(json.variables.data);

                    // In VCF mode: keep collapse open for review
                    // In text parser mode: close collapse and reset
                    if (!isVcfImportMode) {
                        setIsOpen(false);
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
                                    <h6>
                                        {ui.translations['address.parseText.fieldsParsed']}
                                    </h6>
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
                                        highlightNameFields={data.id != null}
                                    />
                                </div>

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

AddressTextParser.propTypes = {
    values: PropTypes.shape({
        title: PropTypes.string,
        buttonText: PropTypes.string,
        initiallyCollapsed: PropTypes.bool,
    }),
};

export default AddressTextParser;
