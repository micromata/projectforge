import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { Button,
    Col,
    Modal,
    ModalBody,
    ModalFooter,
    ModalHeader,
    Row } from 'reactstrap';
import { SketchPicker } from 'react-color';
import { DynamicLayoutContext } from '../../../context';
import DynamicValidationManager from '../../input/DynamicValidationManager';
import Input from '../../../../../design/input';

function CustomizedColorChooser({ values }) {
    const { label, id, defaultColor } = values;

    const { data, setData, ui } = React.useContext(DynamicLayoutContext);

    // Color picker visible or not
    const [open, setOpen] = useState(false);

    const value = Object.getByString(data, id) || '';

    const handleInputChange = ({ target }) => setData({ [id]: target.value });

    const setDefaultValue = () => setData({ [id]: defaultColor });

    const handleColorChange = (color) => {
        setData({ [id]: color.hex });
    };

    const toggle = () => {
        setOpen(!open);
    };

    return React.useMemo(
        () => (
            <Row>
                <Col md="12" lg="7">
                    <DynamicValidationManager id={id}>
                        <Input
                            id={`color-${id}`}
                            onChange={handleInputChange}
                            type="text"
                            label={label}
                            value={value}
                        />
                    </DynamicValidationManager>
                </Col>
                <Col md="12" lg="5">
                    <div style={{ display: 'flex' }}>
                        {defaultColor
                    && (
                        <Button onClick={setDefaultValue} color="primary" outline style={{ marginLeft: '1em', marginTop: '2em' }}>
                            {ui.translations.default}
                        </Button>
                    )}
                        {/* eslint-disable-next-line max-len */}
                        {/* eslint-disable-next-line jsx-a11y/click-events-have-key-events,jsx-a11y/no-static-element-interactions */}
                        <div
                            style={{
                                backgroundColor: `${value}33`,
                                marginLeft: '1em',
                                marginTop: '2.5em',
                                padding: '5px',
                                width: '10em',
                                borderColor: value,
                                borderWidth: '1px',
                                borderStyle: 'solid',
                                borderRadius: '5px',
                                textAlign: 'center',
                                cursor: 'pointer',
                            }}
                            onClick={toggle}
                        >
                            {ui.translations.select}
                        </div>
                    </div>
                </Col>
                <Modal
                    isOpen={open}
                    toggle={toggle}
                    size="sm"
                >
                    <ModalHeader toggle={toggle}>
                        {label}
                    </ModalHeader>
                    <ModalBody>
                        <div style={{ marginLeft: '30px' }}>
                            <SketchPicker
                                color={value}
                                onChangeComplete={handleColorChange}
                                disableAlpha
                            />
                        </div>
                    </ModalBody>
                    <ModalFooter>
                        <Button color="primary" onClick={toggle} outline>
                            {ui.translations.finish}
                        </Button>
                    </ModalFooter>
                </Modal>
            </Row>
        ),
        [
            values.label,
            values.id,
            values.defaultColor,
            data,
            open,
            value,
        ],
    );
}

CustomizedColorChooser.propTypes = {
    values: PropTypes.shape({
        label: PropTypes.string.isRequired,
        id: PropTypes.string.isRequired,
        defaultColor: PropTypes.string,
    }).isRequired,
};

CustomizedColorChooser.defaultProps = {};

export default CustomizedColorChooser;
