/* eslint-disable max-len */
import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import {
    Button, Modal, ModalHeader, ModalBody, ModalFooter, Alert,
} from 'reactstrap';
import { fetchJsonPost } from '../../../../../../utilities/rest';
import AddressFieldSelector from './AddressFieldSelector';

/**
 * Dialog component for VCard import field selection.
 * Allows user to select which fields to import from a VCard into an address.
 * Uses the shared AddressFieldSelector component.
 */
function VCardImportDialog({
    isOpen,
    toggle,
    index,
    onApplySuccess,
    translations,
}) {
    const [vcardData, setVcardData] = useState(null);
    const [dbData, setDbData] = useState(null);
    const [hasMatch, setHasMatch] = useState(false);
    const [selectedFields, setSelectedFields] = useState({});
    const [fieldMappings, setFieldMappings] = useState({});
    const [addressBlockType, setAddressBlockType] = useState('business');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const loadAddressData = () => {
        setLoading(true);
        setError(null);

        fetchJsonPost(
            'importAddress/openAddressDialog',
            { index },
            (json) => {
                setLoading(false);
                if (json) {
                    setVcardData(json.vcardAddress || null);
                    setDbData(json.dbAddress || null);
                    setHasMatch(json.hasMatch || false);

                    // Pre-select fields that differ from DB
                    const selected = {};
                    if (json.vcardAddress) {
                        Object.entries(json.vcardAddress).forEach(([fieldName, value]) => {
                            if (value && (!json.dbAddress || json.dbAddress[fieldName] !== value)) {
                                selected[fieldName] = true;
                            }
                        });
                    }
                    setSelectedFields(selected);
                } else {
                    setError('Failed to load address data');
                }
            },
            (err) => {
                setLoading(false);
                setError(err.message || 'Error loading address data');
            },
        );
    };

    // Load address data when dialog opens
    useEffect(() => {
        if (isOpen && index !== null && index !== undefined) {
            loadAddressData();
        }
    // eslint-disable-next-line
    }, [isOpen, index]);

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

    const isAddressField = (fieldName) => [
        'addressText',
        'addressText2',
        'zipCode',
        'city',
        'state',
        'country',
    ].includes(fieldName);

    const handleApply = () => {
        if (!vcardData) return;

        setLoading(true);
        setError(null);

        // Build map of selected fields with their values
        const fieldsToApply = {};
        Object.entries(vcardData).forEach(([fieldName, value]) => {
            if (selectedFields[fieldName] && value) {
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

                fieldsToApply[targetFieldName] = value;
            }
        });

        fetchJsonPost(
            'importAddress/applyVCardFields',
            {
                index,
                selectedFields: fieldsToApply,
            },
            (json) => {
                setLoading(false);
                if (json && json.success) {
                    toggle();
                    if (onApplySuccess) {
                        onApplySuccess();
                    }
                } else {
                    setError('Error applying fields');
                }
            },
            (err) => {
                setLoading(false);
                setError(err.message || 'Error applying fields');
            },
        );
    };

    if (!vcardData && !loading && isOpen) {
        return (
            <Modal isOpen={isOpen} toggle={toggle} size="lg">
                <ModalHeader toggle={toggle}>
                    {translations['address.book.vCardsImport.fieldSelection'] || 'Select Fields to Import'}
                </ModalHeader>
                <ModalBody>
                    <Alert color="warning">
                        {translations['address.book.vCardsImport.noData'] || 'No data available'}
                    </Alert>
                </ModalBody>
                <ModalFooter>
                    <Button color="secondary" onClick={toggle}>
                        {translations.close || 'Close'}
                    </Button>
                </ModalFooter>
            </Modal>
        );
    }

    return (
        <Modal isOpen={isOpen} toggle={toggle} size="lg">
            <ModalHeader toggle={toggle}>
                {translations['address.book.vCardsImport.fieldSelection'] || 'Select Fields to Import'}
            </ModalHeader>
            <ModalBody>
                {loading && (
                    <div className="text-center">
                        <div className="spinner-border" role="status">
                            <span className="sr-only">Loading...</span>
                        </div>
                    </div>
                )}

                {error && (
                    <Alert color="danger">
                        {error}
                    </Alert>
                )}

                {!loading && vcardData && (
                    <div>
                        {hasMatch && (
                            <Alert color="info">
                                {translations['address.book.vCardsImport.matchFound'] || 'Match found in database. Select fields to update.'}
                            </Alert>
                        )}

                        {!hasMatch && (
                            <Alert color="success">
                                {translations['address.book.vCardsImport.newAddress'] || 'New address will be created.'}
                            </Alert>
                        )}

                        <div className="mt-3">
                            <AddressFieldSelector
                                fields={vcardData}
                                currentData={dbData}
                                selectedFields={selectedFields}
                                onFieldToggle={handleFieldToggle}
                                fieldMappings={fieldMappings}
                                onFieldMappingChange={handleFieldMappingChange}
                                addressBlockType={addressBlockType}
                                onAddressBlockTypeChange={setAddressBlockType}
                                translations={translations}
                                showConfidence={false}
                                showComparison
                            />
                        </div>
                    </div>
                )}
            </ModalBody>
            <ModalFooter>
                <Button
                    color="success"
                    onClick={handleApply}
                    disabled={loading || !vcardData}
                >
                    {translations.apply || 'Apply'}
                </Button>
                <Button color="secondary" onClick={toggle}>
                    {translations.cancel || 'Cancel'}
                </Button>
            </ModalFooter>
        </Modal>
    );
}

VCardImportDialog.propTypes = {
    isOpen: PropTypes.bool.isRequired,
    toggle: PropTypes.func.isRequired,
    index: PropTypes.number,
    onApplySuccess: PropTypes.func,
    translations: PropTypes.shape({
        'address.book.vCardsImport.fieldSelection': PropTypes.string,
        'address.book.vCardsImport.noData': PropTypes.string,
        'address.book.vCardsImport.matchFound': PropTypes.string,
        'address.book.vCardsImport.newAddress': PropTypes.string,
        close: PropTypes.string,
        apply: PropTypes.string,
        cancel: PropTypes.string,
    }).isRequired,
};

export default VCardImportDialog;
