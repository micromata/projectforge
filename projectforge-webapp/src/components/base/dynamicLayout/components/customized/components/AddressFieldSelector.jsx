/* eslint-disable max-len */
import React from 'react';
import PropTypes from 'prop-types';
import {
    FormGroup, Label, Input, Badge, Alert,
} from 'reactstrap';

/**
 * Reusable component for selecting address fields with mapping options.
 * Used by both AddressTextParser and VCardImportDialog.
 *
 * Features:
 * - Checkbox selection for each field
 * - Dropdown mapping for phone/email fields
 * - Address block type selection (Business/Postal/Private)
 * - Field comparison display (new value vs. current value)
 */
function AddressFieldSelector({
    fields,
    currentData,
    selectedFields,
    onFieldToggle,
    fieldMappings,
    onFieldMappingChange,
    addressBlockType,
    onAddressBlockTypeChange,
    translations,
    showConfidence = false,
    showComparison = true,
}) {
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

    const getFieldLabel = (fieldName) => {
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
            comment: 'address.comment',
            birthName: 'address.birthName',
            birthday: 'address.birthday',
        };

        const i18nKey = i18nKeyMap[fieldName];
        const label = i18nKey ? translations[i18nKey] : fieldName;

        // Add context suffix for private fields
        if (fieldName === 'privatePhone' || fieldName === 'privateMobilePhone') {
            const privateLabel = translations['address.private'];
            return privateLabel ? `${label} (${privateLabel})` : label;
        }
        if (fieldName === 'privateEmail') {
            const privateLabel = translations['address.private'];
            return privateLabel ? `${label} (${privateLabel})` : label;
        }
        // Add context suffix for business fields in dropdowns
        if (fieldName === 'businessPhone' || fieldName === 'mobilePhone'
            || fieldName === 'fax' || fieldName === 'email') {
            const businessLabel = translations['address.business'];
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

    if (!fields || Object.keys(fields).length === 0) {
        return (
            <Alert color="info" className="mt-2">
                {translations['address.parseText.info.noChanges'] || 'No changes detected'}
            </Alert>
        );
    }

    const entries = Object.entries(fields).filter(([, field]) => {
        const value = typeof field === 'object' && field !== null ? field.value : field;
        return value;
    });
    const addressFields = entries.filter(([fn]) => isAddressField(fn));
    const nonAddressFields = entries.filter(([fn]) => !isAddressField(fn));
    const hasAddressFields = addressFields.length > 0;

    return (
        <div className="address-field-selector">
            {/* Non-address fields first */}
            {nonAddressFields.map(([fieldName, field]) => {
                const fieldValue = typeof field === 'object' && field !== null ? field.value : field;
                const confidence = typeof field === 'object' && field !== null ? field.confidence : null;
                const currentValue = currentData?.[fieldName];
                const isDifferent = currentValue !== fieldValue;

                return (
                    <div
                        key={fieldName}
                        className="d-flex align-items-start mb-2"
                    >
                        <div style={{ minWidth: '30px' }}>
                            <Input
                                type="checkbox"
                                checked={selectedFields[fieldName] || false}
                                onChange={() => onFieldToggle(fieldName)}
                            />
                        </div>
                        <div style={{ minWidth: '200px', marginRight: '12px' }}>
                            {isPhoneField(fieldName) || isEmailField(fieldName) ? (
                                <Input
                                    type="select"
                                    value={fieldMappings[fieldName] || fieldName}
                                    onChange={(e) => onFieldMappingChange(fieldName, e.target.value)}
                                    style={{ width: '100%' }}
                                >
                                    {isPhoneField(fieldName)
                                        && getPhoneFieldOptions().map((opt) => (
                                            <option key={opt.value} value={opt.value}>
                                                {opt.label}
                                            </option>
                                        ))}
                                    {isEmailField(fieldName)
                                        && getEmailFieldOptions().map((opt) => (
                                            <option key={opt.value} value={opt.value}>
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
                            {fieldValue}
                            {' '}
                            {showConfidence && confidence && (
                                <Badge
                                    color={getConfidenceBadgeColor(confidence)}
                                    className="ml-2"
                                >
                                    {confidence}
                                </Badge>
                            )}
                            {showComparison && isDifferent && currentValue && (
                                <Badge color="warning" className="ml-2">
                                    {translations.different || 'Different'}
                                </Badge>
                            )}
                            {showComparison && currentValue && (
                                <div className="text-muted small mt-1">
                                    <strong>
                                        {translations.current || 'Current'}
                                        :
                                    </strong>
                                    {' '}
                                    {currentValue}
                                </div>
                            )}
                        </div>
                    </div>
                );
            })}

            {/* Address block with all address fields */}
            {hasAddressFields && (
                <div className="mb-3 p-3 border rounded bg-light">
                    <FormGroup>
                        <Label for="addressBlockType">
                            <strong>
                                {translations['address.parseText.addressBlock'] || 'Address Block'}
                            </strong>
                        </Label>
                        <Input
                            type="select"
                            id="addressBlockType"
                            value={addressBlockType}
                            onChange={(e) => onAddressBlockTypeChange(e.target.value)}
                            style={{ maxWidth: '300px' }}
                            className="mb-3"
                        >
                            <option value="business">
                                {translations['address.parseText.addressType.business'] || 'Business Address'}
                            </option>
                            <option value="postal">
                                {translations['address.parseText.addressType.postal'] || 'Postal/Mailing Address'}
                            </option>
                            <option value="private">
                                {translations['address.parseText.addressType.private'] || 'Private Address'}
                            </option>
                        </Input>
                    </FormGroup>

                    {addressFields.map(([fieldName, field]) => {
                        const fieldValue = typeof field === 'object' && field !== null ? field.value : field;
                        const confidence = typeof field === 'object' && field !== null ? field.confidence : null;
                        const currentValue = currentData?.[fieldName];
                        const isDifferent = currentValue !== fieldValue;

                        return (
                            <div
                                key={fieldName}
                                className="d-flex align-items-start mb-2"
                            >
                                <div style={{ minWidth: '30px' }}>
                                    <Input
                                        type="checkbox"
                                        checked={selectedFields[fieldName] || false}
                                        onChange={() => onFieldToggle(fieldName)}
                                    />
                                </div>
                                <div style={{ minWidth: '200px', marginRight: '12px' }}>
                                    <strong>
                                        {getFieldLabel(fieldName)}
                                        :
                                    </strong>
                                </div>
                                <div className="flex-grow-1">
                                    {fieldValue}
                                    {' '}
                                    {showConfidence && confidence && (
                                        <Badge
                                            color={getConfidenceBadgeColor(confidence)}
                                            className="ml-2"
                                        >
                                            {confidence}
                                        </Badge>
                                    )}
                                    {showComparison && isDifferent && currentValue && (
                                        <Badge color="warning" className="ml-2">
                                            {translations.different || 'Different'}
                                        </Badge>
                                    )}
                                    {showComparison && currentValue && (
                                        <div className="text-muted small mt-1">
                                            <strong>
                                                {translations.current || 'Current'}
                                                :
                                            </strong>
                                            {' '}
                                            {currentValue}
                                        </div>
                                    )}
                                </div>
                            </div>
                        );
                    })}
                </div>
            )}
        </div>
    );
}

AddressFieldSelector.propTypes = {
    fields: PropTypes.shape({}).isRequired,
    currentData: PropTypes.shape({}),
    selectedFields: PropTypes.shape({}).isRequired,
    onFieldToggle: PropTypes.func.isRequired,
    fieldMappings: PropTypes.shape({}).isRequired,
    onFieldMappingChange: PropTypes.func.isRequired,
    addressBlockType: PropTypes.string.isRequired,
    onAddressBlockTypeChange: PropTypes.func.isRequired,
    translations: PropTypes.shape({
        'address.private': PropTypes.string,
        'address.business': PropTypes.string,
        'address.parseText.info.noChanges': PropTypes.string,
        'address.parseText.addressBlock': PropTypes.string,
        'address.parseText.addressType.business': PropTypes.string,
        'address.parseText.addressType.postal': PropTypes.string,
        'address.parseText.addressType.private': PropTypes.string,
        different: PropTypes.string,
        current: PropTypes.string,
    }).isRequired,
    showConfidence: PropTypes.bool,
    showComparison: PropTypes.bool,
};

export default AddressFieldSelector;
