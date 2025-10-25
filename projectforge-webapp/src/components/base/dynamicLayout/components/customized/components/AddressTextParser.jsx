import React, { useState } from 'react';
import PropTypes from 'prop-types';
import {
    Button, Card, CardBody, Collapse, FormGroup, Label, Input, FormFeedback,
    Alert, Badge,
} from 'reactstrap';
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

    const isRemappableField = (fieldName) => isPhoneField(fieldName)
        || isEmailField(fieldName)
        || isAddressField(fieldName);

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
        const labels = {
            title: 'Title',
            firstName: 'First Name',
            name: 'Last Name',
            organization: 'Organization',
            division: 'Division',
            positionText: 'Position',
            businessPhone: 'Business Phone',
            mobilePhone: 'Mobile Phone',
            fax: 'Fax',
            privatePhone: 'Private Phone',
            privateMobilePhone: 'Private Mobile Phone',
            email: 'Business Email',
            privateEmail: 'Private Email',
            addressText: 'Street',
            addressText2: 'Street 2',
            zipCode: 'ZIP Code',
            city: 'City',
            state: 'State',
            country: 'Country',
            website: 'Website',
        };
        return labels[fieldName] || fieldName;
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
                <i className={`fa fa-${buttonIcon} mr-2`} />
                {isOpen
                    ? `${ui.translations.hide || 'Hide'} ${buttonText}`
                    : buttonText}
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
                            {loading ? (
                                <>
                                    <i className="fa fa-spinner fa-spin mr-2" />
                                    {ui.translations.parse || 'Parse'}
                                    ...
                                </>
                            ) : (
                                <>
                                    <i className="fa fa-search mr-2" />
                                    {ui.translations.parse || 'Parse'}
                                </>
                            )}
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
                                        && Object.keys(parsedData.fields).some(
                                            (fn) => isAddressField(fn),
                                        ) && (
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
                                        </div>
                                    )}
                                    {parsedData.fields
                                        && Object.entries(parsedData.fields)
                                            .filter(([, field]) => field.value)
                                            .map(([fieldName, field]) => (
                                                <div key={fieldName} className="mb-3">
                                                    <FormGroup check className="mb-1">
                                                        <Label check>
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
                                                            {' '}
                                                            <strong>
                                                                {getFieldLabel(fieldName)}
                                                                :
                                                            </strong>
                                                            {' '}
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
                                                        </Label>
                                                    </FormGroup>
                                                    {isRemappableField(fieldName) && (
                                                        <div className="ml-4">
                                                            <Label
                                                                for={`remap-${fieldName}`}
                                                                className="mr-2 small"
                                                            >
                                                                {ui.translations[
                                                                    'address.parseText.remapTo'
                                                                ] || 'Map to:'}
                                                            </Label>
                                                            <Input
                                                                type="select"
                                                                id={`remap-${fieldName}`}
                                                                value={
                                                                    fieldMappings[fieldName]
                                                                    || fieldName
                                                                }
                                                                onChange={(e) => setFieldMappings({
                                                                    ...fieldMappings,
                                                                    [fieldName]: e.target.value,
                                                                })}
                                                                style={{ width: '250px' }}
                                                                className="d-inline-block"
                                                            >
                                                                {isPhoneField(fieldName)
                                                                    && getPhoneFieldOptions().map(
                                                                        (opt) => (
                                                                            <option
                                                                                key={opt.value}
                                                                                value={opt.value}
                                                                            >
                                                                                {opt.label}
                                                                            </option>
                                                                        ),
                                                                    )}
                                                                {isEmailField(fieldName)
                                                                    && getEmailFieldOptions().map(
                                                                        (opt) => (
                                                                            <option
                                                                                key={opt.value}
                                                                                value={opt.value}
                                                                            >
                                                                                {opt.label}
                                                                            </option>
                                                                        ),
                                                                    )}
                                                                {isAddressField(fieldName) && (
                                                                    <option value={fieldName}>
                                                                        {getFieldLabel(fieldName)}
                                                                    </option>
                                                                )}
                                                            </Input>
                                                        </div>
                                                    )}
                                                </div>
                                            ))}
                                </div>

                                <div className="mt-3">
                                    <Button
                                        color="success"
                                        onClick={handleApply}
                                        className="mr-2"
                                    >
                                        <i className="fa fa-check mr-2" />
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
