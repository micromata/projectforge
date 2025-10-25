/* eslint-disable max-len */
import React, { useState } from 'react';
import PropTypes from 'prop-types';
import {
    Button, Card, CardBody, Collapse, FormGroup, Label, Input, FormFeedback,
    Alert, Badge,
} from 'reactstrap';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faChevronRight } from '@fortawesome/free-solid-svg-icons';
import { useLocation } from 'react-router';
import { fetchJsonPost } from '../../../../../../utilities/rest';
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
    const [addressBlockType, setAddressBlockType] = useState('business');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [isOpen, setIsOpen] = useState(!initiallyCollapsed);

    // Reset state completely when context key changes (address or import context changed)
    React.useEffect(() => {
        if (prevContextKeyRef.current !== contextKey) {
            // Context changed - reset all state
            setInputText('');
            setParsedData(null);
            setSelectedFields({});
            setFieldMappings({});
            setAddressBlockType('business');
            setLoading(false);
            setError(null);
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
    ].includes(fieldName);

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
                    const addressFieldMap = {
                        addressText: {
                            business: 'addressText',
                            postal: 'postalAddressText',
                            private: 'privateAddressText',
                        },
                        addressText2: {
                            business: 'addressText2',
                            postal: 'postalAddressText2',
                            private: 'privateAddressText2',
                        },
                        zipCode: {
                            business: 'zipCode',
                            postal: 'postalZipCode',
                            private: 'privateZipCode',
                        },
                        city: {
                            business: 'city',
                            postal: 'postalCity',
                            private: 'privateCity',
                        },
                        state: {
                            business: 'state',
                            postal: 'postalState',
                            private: 'privateState',
                        },
                        country: {
                            business: 'country',
                            postal: 'postalCountry',
                            private: 'privateCountry',
                        },
                    };
                    targetFieldName = addressFieldMap[fieldName]?.[addressBlockType] || fieldName;
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
                        setAddressBlockType('business');
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
                                        addressBlockType={addressBlockType}
                                        onAddressBlockTypeChange={setAddressBlockType}
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
