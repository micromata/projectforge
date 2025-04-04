'use client';

import React, { useState, useEffect } from 'react';
import { Button } from '@/components/ui/button';
import { PlusIcon } from 'lucide-react';
import InvoicePositionRow from './InvoicePositionRow';

interface Position {
  id?: number;
  number: number;
  text: string;
  menge: number;
  einzelNetto: number;
  vat: number;
  auftragsPosition?: any;
  periodOfPerformanceType: string;
  periodOfPerformanceBegin?: Date | null;
  periodOfPerformanceEnd?: Date | null;
}

interface InvoicePositionsProps {
  positions?: Position[];
  onChange?: (positions: Position[]) => void;
}

// Helper to ensure a position has valid numeric values
const sanitizePosition = (position: Position): Position => {
  return {
    ...position,
    // Only generate an id if it doesn't exist
    id: position.id || Date.now() + Math.floor(Math.random() * 1000),
    number: position.number || 0,
    menge: isNaN(Number(position.menge)) ? 0 : Number(position.menge),
    einzelNetto: isNaN(Number(position.einzelNetto)) ? 0 : Number(position.einzelNetto),
    vat: isNaN(Number(position.vat)) ? 0 : Number(position.vat),
    text: position.text || '',
    s_text: position.s_text || position.text || '',
    periodOfPerformanceType: position.periodOfPerformanceType || 'SEEABOVE'
  };
};

export default function InvoicePositions({ 
  positions = [], 
  onChange 
}: InvoicePositionsProps) {
  // Create a sanitized copy of the positions
  const sanitizedPositions = React.useMemo(() => {
    if (positions.length === 0) {
      return [{
        id: 1,
        number: 1, // The position number, starting with 1
        text: '',
        s_text: '',
        menge: 1,
        einzelNetto: 0,
        vat: 19,
        periodOfPerformanceType: 'SEEABOVE'
      }];
    }
    
    // Sort positions by number and ensure each has a valid number
    return positions.map(sanitizePosition)
      .sort((a, b) => (a.number || 0) - (b.number || 0))
      .map((pos, index) => ({
        ...pos,
        number: index + 1 // Ensure sequential numbering starting from 1
      }));
  }, [positions]);
  
  // Calculate totals from the sanitized positions
  const totals = React.useMemo(() => {
    return sanitizedPositions.reduce(
      (sums, position) => {
        const netSum = position.menge * position.einzelNetto;
        const vatAmount = netSum * (position.vat / 100);
        
        return {
          net: sums.net + netSum,
          vat: sums.vat + vatAmount,
          gross: sums.gross + netSum + vatAmount
        };
      }, 
      { net: 0, vat: 0, gross: 0 }
    );
  }, [sanitizedPositions]);
  
  // Handlers that create a new array instead of mutating the existing one
  const handleAddPosition = React.useCallback(() => {
    if (!onChange) return;
    
    const maxId = Math.max(...sanitizedPositions.map(p => p.id || 0), 0) + 1;
    // Number should be 1-based and sequential
    const nextNumber = sanitizedPositions.length + 1;
    const vat = sanitizedPositions.length > 0 ? sanitizedPositions[sanitizedPositions.length - 1].vat : 19;
    
    const newPosition = {
      id: maxId,
      number: nextNumber, // Always use the next sequential number
      text: '',
      s_text: '',
      menge: 1,
      einzelNetto: 0,
      vat,
      periodOfPerformanceType: 'SEEABOVE'
    };
    
    onChange([...sanitizedPositions, newPosition]);
  }, [onChange, sanitizedPositions]);
  
  const handleDeletePosition = React.useCallback((id: number) => {
    if (!onChange) return;
    
    if (sanitizedPositions.length <= 1) {
      // Don't delete the last position, just reset it
      onChange([{
        id: 1,
        number: 1,
        text: '',
        s_text: '',
        menge: 1,
        einzelNetto: 0,
        vat: 19,
        periodOfPerformanceType: 'SEEABOVE'
      }]);
      return;
    }
    
    // Filter out the deleted position and renumber sequentially
    const updatedPositions = sanitizedPositions
      .filter(position => position.id !== id)
      .map((position, index) => ({
        ...position,
        number: index + 1 // Ensure positions are numbered 1, 2, 3, etc.
      }));
    
    onChange(updatedPositions);
  }, [onChange, sanitizedPositions]);
  
  const handlePositionChange = React.useCallback((id: number, field: string, value: any) => {
    if (!onChange) return;
    
    // Process and sanitize the value based on field type
    let processedValue = value;
    if (['menge', 'einzelNetto', 'vat'].includes(field)) {
      // Ensure numeric fields have numeric values
      processedValue = isNaN(Number(value)) ? 0 : Number(value);
    }
    
    const updatedPositions = sanitizedPositions.map(position => {
      if (position.id !== id) return position;
      
      // Create updated position
      const updatedPosition = { ...position, [field]: processedValue };
      
      // Synchronize text and s_text fields
      if (field === 'text') {
        updatedPosition.s_text = processedValue;
      } else if (field === 's_text') {
        updatedPosition.text = processedValue;
      }
      
      return updatedPosition;
    });
    
    onChange(updatedPositions);
  }, [onChange, sanitizedPositions]);

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h3 className="text-lg font-medium">Invoice Positions</h3>
        
        <div className="text-sm grid grid-cols-3 gap-4">
          <div>
            <div className="font-medium">Total Net:</div>
            <div className="font-bold">€{totals.net.toFixed(2)}</div>
          </div>
          <div>
            <div className="font-medium">Total VAT:</div>
            <div className="font-bold">€{totals.vat.toFixed(2)}</div>
          </div>
          <div>
            <div className="font-medium">Total Gross:</div>
            <div className="font-bold">€{totals.gross.toFixed(2)}</div>
          </div>
        </div>
      </div>
      
      {sanitizedPositions.map(position => (
        <InvoicePositionRow 
          key={position.id}
          position={position}
          onDelete={handleDeletePosition}
          onChange={handlePositionChange}
        />
      ))}
      
      <Button 
        variant="outline" 
        className="w-full"
        onClick={handleAddPosition}
      >
        <PlusIcon className="h-4 w-4 mr-2" />
        Add Position
      </Button>
    </div>
  );
}