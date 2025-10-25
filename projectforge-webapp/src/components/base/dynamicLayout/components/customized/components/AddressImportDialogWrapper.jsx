import React, { useState, useEffect, useContext } from 'react';
import { DynamicLayoutContext } from '../../../context';
import VCardImportDialog from './VCardImportDialog';

/**
 * Wrapper component for Address Import Page that provides VCard import dialog functionality.
 * Registers a global function to open the dialog when a row is clicked in the AG-Grid.
 */
function AddressImportPageWrapper() {
    const { ui } = useContext(DynamicLayoutContext);
    const [dialogOpen, setDialogOpen] = useState(false);
    const [selectedIndex, setSelectedIndex] = useState(null);

    useEffect(() => {
        // Register global function for AG-Grid row click
        window.openVCardImportDialog = (event) => {
            if (event && event.node && event.node.rowIndex !== undefined) {
                setSelectedIndex(event.node.rowIndex);
                setDialogOpen(true);
            }
        };

        // Cleanup on unmount
        return () => {
            delete window.openVCardImportDialog;
        };
    }, []);

    const handleDialogClose = () => {
        setDialogOpen(false);
        setSelectedIndex(null);
    };

    const handleApplySuccess = () => {
        // Optionally reload the grid or show a success message
        // The grid should automatically update after the backend updates the entry
        if (window.location) {
            window.location.reload();
        }
    };

    return (
        <VCardImportDialog
            isOpen={dialogOpen}
            toggle={handleDialogClose}
            index={selectedIndex}
            onApplySuccess={handleApplySuccess}
            translations={ui?.translations || {}}
        />
    );
}

export default AddressImportPageWrapper;
