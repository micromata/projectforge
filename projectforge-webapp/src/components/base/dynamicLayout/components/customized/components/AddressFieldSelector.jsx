/* eslint-disable max-len */
import React from 'react';
import PropTypes from 'prop-types';
import {
    FormGroup, Label, Input, Badge, Alert,
} from 'reactstrap';
import DiffText from '../../../../../design/DiffText';

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
    addressBlockMappings,
    onAddressBlockMappingChange,
    translations,
    showConfidence = false,
    showComparison = true,
    highlightNameFields = false,
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
            return withoutPrivate.charAt(0).toLowerCase() + withoutPrivate.substring(1);
        }
        if (fieldName.startsWith('postal')) {
            const withoutPostal = fieldName.substring(6); // Remove "postal"
            return withoutPostal.charAt(0).toLowerCase() + withoutPostal.substring(1);
        }
        return fieldName;
    };

    const buildTargetFieldName = (baseFieldName, targetBlockType) => {
        // Build target field name: "city" + "postal" → "postalCity"
        if (targetBlockType === 'private') {
            return `private${baseFieldName.charAt(0).toUpperCase()}${baseFieldName.substring(1)}`;
        }
        if (targetBlockType === 'postal') {
            return `postal${baseFieldName.charAt(0).toUpperCase()}${baseFieldName.substring(1)}`;
        }
        return baseFieldName;
    };

    const isNameField = (fieldName) => ['name', 'firstName'].includes(fieldName);

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
            privateAddressText: 'address.addressText',
            privateAddressText2: 'address.addressText2',
            privateZipCode: 'address.zipCode',
            privateCity: 'address.city',
            privateState: 'address.state',
            privateCountry: 'address.country',
            postalAddressText: 'address.addressText',
            postalAddressText2: 'address.addressText2',
            postalZipCode: 'address.zipCode',
            postalCity: 'address.city',
            postalState: 'address.state',
            postalCountry: 'address.country',
            website: 'address.website',
            comment: 'comment',
            birthName: 'address.birthName',
            birthday: 'address.birthday',
        };

        const i18nKey = i18nKeyMap[fieldName];
        const label = i18nKey ? translations[i18nKey] : fieldName;

        // Add context suffix for private fields
        if (fieldName === 'privatePhone' || fieldName === 'privateMobilePhone' || fieldName === 'privateEmail'
            || fieldName.startsWith('private')) {
            const privateLabel = translations['address.private'];
            return privateLabel ? `${label} (${privateLabel})` : label;
        }
        // Add context suffix for postal fields
        if (fieldName.startsWith('postal')) {
            const postalLabel = translations['address.postal'];
            return postalLabel ? `${label} (${postalLabel})` : label;
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

    /**
     * Get the mapped field name based on field mappings and address block mappings.
     * This is used to retrieve the correct currentValue from currentData.
     */
    const getMappedFieldName = (fieldName) => {
        // Phone/Email fields: Use fieldMappings from dropdown selection
        if (isPhoneField(fieldName) || isEmailField(fieldName)) {
            return fieldMappings[fieldName] || fieldName;
        }

        // Address fields: Map based on addressBlockMappings
        if (isAddressField(fieldName)) {
            const sourceBlockType = getAddressBlockType(fieldName);
            const targetBlockType = addressBlockMappings[sourceBlockType] || sourceBlockType;
            const baseFieldName = getBaseFieldName(fieldName);
            return buildTargetFieldName(baseFieldName, targetBlockType);
        }

        // Other fields: No mapping
        return fieldName;
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
    const nonAddressFields = entries.filter(([fn]) => !isAddressField(fn));

    // Sort non-address fields: other fields first, then phone fields, then email fields
    const phoneFieldOrder = ['businessPhone', 'mobilePhone', 'fax', 'privatePhone', 'privateMobilePhone'];
    const emailFieldOrder = ['email', 'privateEmail'];

    const sortedNonAddressFields = [...nonAddressFields].sort(([fieldNameA], [fieldNameB]) => {
        const isPhoneA = isPhoneField(fieldNameA);
        const isPhoneB = isPhoneField(fieldNameB);
        const isEmailA = isEmailField(fieldNameA);
        const isEmailB = isEmailField(fieldNameB);

        // Both are phone fields → sort by phoneFieldOrder
        if (isPhoneA && isPhoneB) {
            return phoneFieldOrder.indexOf(fieldNameA) - phoneFieldOrder.indexOf(fieldNameB);
        }

        // Both are email fields → sort by emailFieldOrder
        if (isEmailA && isEmailB) {
            return emailFieldOrder.indexOf(fieldNameA) - emailFieldOrder.indexOf(fieldNameB);
        }

        // One phone, one email → phone comes first
        if (isPhoneA && isEmailB) return -1;
        if (isEmailA && isPhoneB) return 1;

        // One is phone/email, other is neither → phone/email comes after
        if ((isPhoneA || isEmailA) && !isPhoneB && !isEmailB) return 1;
        if ((isPhoneB || isEmailB) && !isPhoneA && !isEmailA) return -1;

        // Neither are phone/email → keep original order
        return 0;
    });

    // Group address fields by block type (business/private/postal)
    const addressFieldsByBlock = {
        business: entries.filter(([fn]) => isAddressField(fn) && getAddressBlockType(fn) === 'business'),
        private: entries.filter(([fn]) => isAddressField(fn) && getAddressBlockType(fn) === 'private'),
        postal: entries.filter(([fn]) => isAddressField(fn) && getAddressBlockType(fn) === 'postal'),
    };

    return (
        <div className="address-field-selector">
            {/* Non-address fields first */}
            {sortedNonAddressFields.map(([fieldName, field]) => {
                const fieldValue = typeof field === 'object' && field !== null ? field.value : field;
                const confidence = typeof field === 'object' && field !== null ? field.confidence : null;
                const targetFieldName = getMappedFieldName(fieldName);
                const currentValue = currentData?.[targetFieldName];
                const isDifferent = currentValue != null && currentValue !== fieldValue;
                const shouldHighlightName = highlightNameFields && isNameField(fieldName);

                return (
                    <div
                        key={fieldName}
                        className="d-flex align-items-start mb-2"
                        style={shouldHighlightName ? {
                            padding: '8px',
                            backgroundColor: '#fff3cd',
                            border: '1px solid #ffc107',
                            borderRadius: '4px',
                        } : {}}
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
                            {showComparison && isDifferent && currentValue ? (
                                <div className="d-flex align-items-center">
                                    <DiffText oldValue={currentValue} newValue={fieldValue} inline />
                                    {shouldHighlightName && (
                                        <Badge color="danger" className="ml-2">
                                            <i className="fa fa-exclamation-triangle" />
                                            {' '}
                                            {translations['address.parseText.warning.nameDifferent'] || 'Name differs from current address'}
                                        </Badge>
                                    )}
                                    {showConfidence && confidence && (
                                        <Badge
                                            color={getConfidenceBadgeColor(confidence)}
                                            className="ml-2"
                                        >
                                            {confidence}
                                        </Badge>
                                    )}
                                </div>
                            ) : (
                                <>
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
                                </>
                            )}
                        </div>
                    </div>
                );
            })}

            {/* Address blocks - separate block for each type (business/private/postal) */}
            {['business', 'private', 'postal'].map((blockType) => {
                const blockFields = addressFieldsByBlock[blockType];
                if (blockFields.length === 0) return null;

                const blockTypeLabel = {
                    business: translations['address.parseText.addressType.business'] || 'Business Address',
                    private: translations['address.parseText.addressType.private'] || 'Private Address',
                    postal: translations['address.parseText.addressType.postal'] || 'Postal/Mailing Address',
                };

                return (
                    <div key={blockType} className="mb-3 p-3 border rounded bg-light">
                        <FormGroup>
                            <Label for={`addressBlockType-${blockType}`}>
                                <strong>
                                    {blockTypeLabel[blockType]}
                                </strong>
                            </Label>
                            <Input
                                type="select"
                                id={`addressBlockType-${blockType}`}
                                value={addressBlockMappings[blockType] || blockType}
                                onChange={(e) => onAddressBlockMappingChange(blockType, e.target.value)}
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

                        {blockFields.map(([fieldName, field]) => {
                            const fieldValue = typeof field === 'object' && field !== null ? field.value : field;
                            const confidence = typeof field === 'object' && field !== null ? field.confidence : null;
                            const targetFieldName = getMappedFieldName(fieldName);
                            const currentValue = currentData?.[targetFieldName];
                            const isDifferent = currentValue != null && currentValue !== fieldValue;

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
                                        {showComparison && isDifferent && currentValue ? (
                                            <div className="d-flex align-items-center">
                                                <DiffText oldValue={currentValue} newValue={fieldValue} inline />
                                                {showConfidence && confidence && (
                                                    <Badge
                                                        color={getConfidenceBadgeColor(confidence)}
                                                        className="ml-2"
                                                    >
                                                        {confidence}
                                                    </Badge>
                                                )}
                                            </div>
                                        ) : (
                                            <>
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
                                            </>
                                        )}
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                );
            })}
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
    addressBlockMappings: PropTypes.shape({}).isRequired,
    onAddressBlockMappingChange: PropTypes.func.isRequired,
    translations: PropTypes.shape({
        'address.private': PropTypes.string,
        'address.business': PropTypes.string,
        'address.postal': PropTypes.string,
        'address.parseText.info.noChanges': PropTypes.string,
        'address.parseText.addressBlock': PropTypes.string,
        'address.parseText.addressType.business': PropTypes.string,
        'address.parseText.addressType.postal': PropTypes.string,
        'address.parseText.addressType.private': PropTypes.string,
        'address.parseText.warning.nameDifferent': PropTypes.string,
        'address.parseText.confidence.legend': PropTypes.string,
        'address.parseText.confidence.high': PropTypes.string,
        'address.parseText.confidence.medium': PropTypes.string,
        'address.parseText.confidence.low': PropTypes.string,
        different: PropTypes.string,
        current: PropTypes.string,
    }).isRequired,
    showConfidence: PropTypes.bool,
    showComparison: PropTypes.bool,
    highlightNameFields: PropTypes.bool,
};

export default AddressFieldSelector;
