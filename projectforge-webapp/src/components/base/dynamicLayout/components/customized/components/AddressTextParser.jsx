import React, { useState } from 'react';
import PropTypes from 'prop-types';
import {
    Button, Card, CardBody, Collapse, FormGroup, Label, Input, FormFeedback,
    Alert, Badge,
} from 'reactstrap';
import { fetchJsonPost } from '../../../../../../utilities/rest';
import { DynamicLayoutContext } from '../../../context';

function AddressTextParser({ values }) {
    const { setData, data } = React.useContext(DynamicLayoutContext);
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
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

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
                    setParsedData(parsed);

                    // Initialize selected fields based on parsed data
                    const selected = {};
                    if (parsed.fields) {
                        Object.entries(parsed.fields).forEach(([fieldName, field]) => {
                            selected[fieldName] = field.selected !== false;
                        });
                    }
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

        // Build map of selected fields with their values
        const fieldsToApply = {};
        Object.entries(parsedData.fields).forEach(([fieldName, field]) => {
            if (selectedFields[fieldName] && field.value) {
                fieldsToApply[fieldName] = field.value;
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

    return (
        <div className="address-text-parser mb-3">
            <Button
                color="secondary"
                onClick={handleToggle}
                className="mb-2"
            >
                <i className={`fa fa-${buttonIcon} mr-2`} />
                {isOpen ? `Hide ${buttonText}` : buttonText}
            </Button>

            <Collapse isOpen={isOpen}>
                <Card>
                    <CardBody>
                        <h5>{title}</h5>

                        <FormGroup>
                            <Label for="addressTextInput">
                                Paste email signature or contact information:
                            </Label>
                            <Input
                                type="textarea"
                                id="addressTextInput"
                                rows={10}
                                value={inputText}
                                onChange={(e) => setInputText(e.target.value)}
                                placeholder="Paste address text here...&#10;&#10;Example:&#10;Dr. Max Mustermann&#10;Senior Manager&#10;Example GmbH&#10;Musterstraße 1-5&#10;12345 Musterstadt&#10;Tel: +49 30 12345678&#10;m.mustermann@example.com"
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
                                    Parsing...
                                </>
                            ) : (
                                <>
                                    <i className="fa fa-search mr-2" />
                                    Parse
                                </>
                            )}
                        </Button>

                        {parsedData && (
                            <div className="mt-4">
                                <h6>Parsed Fields</h6>

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

                                <div className="parsed-fields-list mt-3">
                                    {parsedData.fields
                                        && Object.entries(parsedData.fields)
                                            .filter(([, field]) => field.value)
                                            .map(([fieldName, field]) => (
                                                <FormGroup check key={fieldName} className="mb-2">
                                                    <Label check>
                                                        <Input
                                                            type="checkbox"
                                                            checked={
                                                                selectedFields[fieldName] || false
                                                            }
                                                            onChange={
                                                                () => handleFieldToggle(fieldName)
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
                                            ))}
                                </div>

                                <div className="mt-3">
                                    <Button
                                        color="success"
                                        onClick={handleApply}
                                        className="mr-2"
                                    >
                                        <i className="fa fa-check mr-2" />
                                        Apply Selected Fields
                                    </Button>
                                    <Button
                                        color="secondary"
                                        onClick={() => {
                                            setParsedData(null);
                                            setSelectedFields({});
                                            setError(null);
                                        }}
                                    >
                                        Cancel
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
