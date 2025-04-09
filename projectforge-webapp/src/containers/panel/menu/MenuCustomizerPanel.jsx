import React from 'react';
import { Card, CardBody } from 'reactstrap';
import MenuCustomizer from '../../../components/menu/MenuCustomizer';

function MenuCustomizerPanel() {
    return (
        <Card>
            <CardBody>
                <MenuCustomizer />
            </CardBody>
        </Card>
    );
}

export default MenuCustomizerPanel;
