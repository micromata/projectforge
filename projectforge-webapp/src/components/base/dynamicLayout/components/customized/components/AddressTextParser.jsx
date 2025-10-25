/* eslint-disable max-len */
import React, { useState } from 'react';
import PropTypes from 'prop-types';
import {
    Button, Card, CardBody, Collapse, FormGroup, Label, Input, FormFeedback,
    Alert, Badge,
} from 'reactstrap';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faChevronRight } from '@fortawesome/free-solid-svg-icons';
import { fetchJsonPost } from '../../../../../../utilities/rest';
import { DynamicLayoutContext } from '../../../context';

function AddressTextParser({ values }) {
    const { ui, setData, data } = React.useContext(DynamicLayoutContext);
    const {
        title = 'Parse Address from Text',
        buttonText = 'Parse from Text',
        initiallyCollapsed = true,
        buttonIcon = 'paste',
    } = values || {};

    const [isOpen, setIsOpen] = useState(!initiallyCollapsed);
    const [inputText, setInputText] = useState('');
    const [parsedData, setParsedData] = useState(null);
    const [selectedFields, setSelectedFields] = useState({});
    const [fieldMappings, setFieldMappings] = useState({});
    const [addressBlockType, setAddressBlockType] = useState('business');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    // Helper functions for field type detection
    const isPhoneField = (fieldName) => [
        'businessPhone',
        'mobilePhone',
        'fax',
        'privatePhone',
        'privateMobilePhone',
    ].includes(fieldName);

    const isEmailField = (fieldName) => ['email', 'privateEmail'].includes(fieldName);

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
                            // (both null/undefined/empty are considered same)
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
                    Object.entries(filteredFields).forEach(([fieldName, field]) => {
                        selected[fieldName] = field.selected !== false;
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

    const handleApply = () => {
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
                    targetFieldName = addressFieldMap[fieldName]?.[addressBlockType]
                        || fieldName;
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

                    // Close collapse and reset
                    setIsOpen(false);
                    setInputText('');
                    setParsedData(null);
                    setSelectedFields({});
                    setFieldMappings({});
                    setAddressBlockType('business');
                } else {
                    setError('Error applying data');
                }
            },
        );
    };

    const getConfidenceBadgeColor = (confidence) => {
        switch (confidence) {
            case 'HIGH':
                return 'success';
            case 'MEDIUM':
                return 'warning';
            case 'LOW':
                return 'danger';
            default:
                return 'secondary';
        }
    };

    const getFieldLabel = (fieldName) => {
        // Map field names to i18n keys from AddressDO
        const i18nKeyMap = {
            title: 'address.title',
            firstName: 'firstName',
            name: 'name',
            organization: 'organization',
            division: 'address.division',
            positionText: 'address.positionText',
            businessPhone: 'address.phone',
            mobilePhone: 'address.phoneType.mobile',
            fax: 'address.phoneType.fax',
            privatePhone: 'address.phone',
            privateMobilePhone: 'address.phoneType.mobile',
            email: 'email',
            privateEmail: 'email',
            addressText: 'address.addressText',
            addressText2: 'address.addressText2',
            zipCode: 'address.zipCode',
            city: 'address.city',
            state: 'address.state',
            country: 'address.country',
            website: 'address.website',
        };

        const i18nKey = i18nKeyMap[fieldName];
        const label = i18nKey ? ui.translations[i18nKey] : fieldName;

        // Add context suffix for private fields
        if (fieldName === 'privatePhone' || fieldName === 'privateMobilePhone') {
            const privateLabel = ui.translations['address.private'];
            return privateLabel ? `${label} (${privateLabel})` : label;
        }
        if (fieldName === 'privateEmail') {
            const privateLabel = ui.translations['address.private'];
            return privateLabel ? `${label} (${privateLabel})` : label;
        }
        // Add context suffix for business fields in dropdowns
        if (fieldName === 'businessPhone' || fieldName === 'mobilePhone'
            || fieldName === 'fax' || fieldName === 'email') {
            const businessLabel = ui.translations['address.business'];
            return businessLabel ? `${label} (${businessLabel})` : label;
        }

        return label || fieldName;
    };

    const getPhoneFieldOptions = () => [
        { value: 'businessPhone', label: getFieldLabel('businessPhone') },
        { value: 'mobilePhone', label: getFieldLabel('mobilePhone') },
        { value: 'fax', label: getFieldLabel('fax') },
        { value: 'privatePhone', label: getFieldLabel('privatePhone') },
        { value: 'privateMobilePhone', label: getFieldLabel('privateMobilePhone') },
    ];

    const getEmailFieldOptions = () => [
        { value: 'email', label: getFieldLabel('email') },
        { value: 'privateEmail', label: getFieldLabel('privateEmail') },
    ];

    return (
        <div className="address-text-parser mb-3">
            <Button
                color="secondary"
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
                            onClick={handleParse}
                            disabled={!inputText.trim() || loading}
                        >
                            {loading
                                ? `${ui.translations.parse || 'Parse'}...`
                                : ui.translations.parse || 'Parse'}
                        </Button>

                        {parsedData && (
                            <div className="mt-4">
                                <h6>
                                    {ui.translations['address.parseText.fieldsParsed']}
                                </h6>

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

                                {parsedData.fields
                                    && Object.keys(parsedData.fields).length === 0 && (
                                    <Alert color="info" className="mt-2">
                                        {ui.translations['address.parseText.info.noChanges']}
                                    </Alert>
                                )}

                                <div className="parsed-fields-list mt-3">
                                    {parsedData.fields
                                        && (() => {
                                            const entries = Object.entries(parsedData.fields)
                                                .filter(([, field]) => field.value);
                                            const addressFields = entries.filter(
                                                ([fn]) => isAddressField(fn),
                                            );
                                            const nonAddressFields = entries.filter(
                                                ([fn]) => !isAddressField(fn),
                                            );
                                            const hasAddressFields = addressFields.length > 0;

                                            return (
                                                <>
                                                    {/* Non-address fields first */}
                                                    {nonAddressFields.map(([fieldName, field]) => (
                                                        <div
                                                            key={fieldName}
                                                            className="d-flex align-items-start mb-2"
                                                        >
                                                            <div style={{ minWidth: '30px' }}>
                                                                <Input
                                                                    type="checkbox"
                                                                    checked={
                                                                        selectedFields[fieldName]
                                                                        || false
                                                                    }
                                                                    onChange={
                                                                        () => handleFieldToggle(
                                                                            fieldName,
                                                                        )
                                                                    }
                                                                />
                                                            </div>
                                                            <div
                                                                style={{
                                                                    minWidth: '200px',
                                                                    marginRight: '12px',
                                                                }}
                                                            >
                                                                {isPhoneField(fieldName)
                                                                    || isEmailField(fieldName) ? (
                                                                        <Input
                                                                            type="select"
                                                                            value={
                                                                                fieldMappings[
                                                                                    fieldName
                                                                                ] || fieldName
                                                                            }
                                                                            onChange={(e) => {
                                                                                const newMappings = {
                                                                                    ...fieldMappings,
                                                                                    [fieldName]:
                                                                                        e.target.value,
                                                                                };
                                                                                setFieldMappings(
                                                                                    newMappings,
                                                                                );
                                                                            }}
                                                                            style={{
                                                                                width: '100%',
                                                                            }}
                                                                        >
                                                                            {isPhoneField(fieldName)
                                                                                && getPhoneFieldOptions()
                                                                                    .map((opt) => (
                                                                                        <option
                                                                                            key={
                                                                                                opt.value
                                                                                            }
                                                                                            value={
                                                                                                opt.value
                                                                                            }
                                                                                        >
                                                                                            {opt.label}
                                                                                        </option>
                                                                                    ))}
                                                                            {isEmailField(fieldName)
                                                                                && getEmailFieldOptions()
                                                                                    .map((opt) => (
                                                                                        <option
                                                                                            key={
                                                                                                opt.value
                                                                                            }
                                                                                            value={
                                                                                                opt.value
                                                                                            }
                                                                                        >
                                                                                            {opt.label}
                                                                                        </option>
                                                                                    ))}
                                                                        </Input>
                                                                    ) : (
                                                                        <strong>
                                                                            {getFieldLabel(fieldName)}
                                                                            :
                                                                        </strong>
                                                                    )}
                                                            </div>
                                                            <div className="flex-grow-1">
                                                                {field.value}
                                                                {' '}
                                                                <Badge
                                                                    color={
                                                                        getConfidenceBadgeColor(
                                                                            field.confidence,
                                                                        )
                                                                    }
                                                                    className="ml-2"
                                                                >
                                                                    {field.confidence}
                                                                </Badge>
                                                            </div>
                                                        </div>
                                                    ))}

                                                    {/* Address block with all address fields */}
                                                    {hasAddressFields && (
                                                        <div className="mb-3 p-3 border rounded bg-light">
                                                            <FormGroup>
                                                                <Label for="addressBlockType">
                                                                    <strong>
                                                                        {ui.translations[
                                                                            'address.parseText.addressBlock'
                                                                        ] || 'Address Block'}
                                                                    </strong>
                                                                </Label>
                                                                <Input
                                                                    type="select"
                                                                    id="addressBlockType"
                                                                    value={addressBlockType}
                                                                    onChange={(e) => setAddressBlockType(
                                                                        e.target.value,
                                                                    )}
                                                                    style={{ maxWidth: '300px' }}
                                                                    className="mb-3"
                                                                >
                                                                    <option value="business">
                                                                        {ui.translations[
                                                                            'address.parseText.addressType.business'
                                                                        ] || 'Business Address'}
                                                                    </option>
                                                                    <option value="postal">
                                                                        {ui.translations[
                                                                            'address.parseText.addressType.postal'
                                                                        ] || 'Postal/Mailing Address'}
                                                                    </option>
                                                                    <option value="private">
                                                                        {ui.translations[
                                                                            'address.parseText.addressType.private'
                                                                        ] || 'Private Address'}
                                                                    </option>
                                                                </Input>
                                                            </FormGroup>

                                                            {addressFields.map(([fieldName, field]) => (
                                                                <div
                                                                    key={fieldName}
                                                                    className="d-flex align-items-start mb-2"
                                                                >
                                                                    <div style={{ minWidth: '30px' }}>
                                                                        <Input
                                                                            type="checkbox"
                                                                            checked={
                                                                                selectedFields[fieldName]
                                                                                || false
                                                                            }
                                                                            onChange={
                                                                                () => handleFieldToggle(
                                                                                    fieldName,
                                                                                )
                                                                            }
                                                                        />
                                                                    </div>
                                                                    <div
                                                                        style={{
                                                                            minWidth: '200px',
                                                                            marginRight: '12px',
                                                                        }}
                                                                    >
                                                                        <strong>
                                                                            {getFieldLabel(fieldName)}
                                                                            :
                                                                        </strong>
                                                                    </div>
                                                                    <div className="flex-grow-1">
                                                                        {field.value}
                                                                        {' '}
                                                                        <Badge
                                                                            color={
                                                                                getConfidenceBadgeColor(
                                                                                    field.confidence,
                                                                                )
                                                                            }
                                                                            className="ml-2"
                                                                        >
                                                                            {field.confidence}
                                                                        </Badge>
                                                                    </div>
                                                                </div>
                                                            ))}
                                                        </div>
                                                    )}
                                                </>
                                            );
                                        })()}
                                </div>

                                <div className="mt-3">
                                    <Button
                                        color="success"
                                        onClick={handleApply}
                                        className="mr-2"
                                    >
                                        {ui.translations.apply || 'Apply'}
                                    </Button>
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
                                </div>
                            </div>
                        )}

                        {error && parsedData && (
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
        buttonIcon: PropTypes.string,
    }),
};

export default AddressTextParser;
