"use client";

import React from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { Calendar } from "@/components/ui/calendar";
import { CalendarIcon, TrashIcon } from "lucide-react";

interface InvoicePositionRowProps {
  position: {
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
  };
  onDelete: (number: number) => void;
  onChange: (number: number, field: string, value: any) => void;
}

// Using React.memo to prevent unnecessary re-renders
const InvoicePositionRow = React.memo(function InvoicePositionRow({ 
  position, 
  onDelete, 
  onChange 
}: InvoicePositionRowProps) {
  // Calculate position totals with NaN protection
  const menge = isNaN(Number(position.menge)) ? 0 : Number(position.menge);
  const einzelNetto = isNaN(Number(position.einzelNetto)) ? 0 : Number(position.einzelNetto);
  const vat = isNaN(Number(position.vat)) ? 0 : Number(position.vat);
  
  const netSum = menge * einzelNetto;
  const vatAmount = netSum * (vat / 100);
  const grossSum = netSum + vatAmount;

  return (
    <div className="bg-muted/40 rounded-lg p-4 relative">
      <h4 className="font-medium mb-3">Position #{position.number}</h4>
      <div className="absolute top-2 right-2">
        <Button 
          variant="ghost" 
          size="sm" 
          onClick={() => onDelete(position.id)}
        >
          <TrashIcon className="h-4 w-4" />
        </Button>
      </div>
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="space-y-2">
          <Label htmlFor={`pos-order-${position.id}`}>Order</Label>
          <Input 
            id={`pos-order-${position.id}`} 
            placeholder="Order reference" 
            value={position.auftragsPosition?.auftragsPositionId || ""}
            onChange={(e) => onChange(position.id, "auftragsPosition", { auftragsPositionId: e.target.value })}
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor={`pos-amount-${position.id}`}>Amount</Label>
          <Input 
            id={`pos-amount-${position.id}`} 
            type="number" 
            step="0.01" 
            value={position.menge || 0}
            onChange={(e) => {
              const value = e.target.value === '' ? 0 : parseFloat(e.target.value);
              onChange(position.id, "menge", isNaN(value) ? 0 : value);
            }}
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor={`pos-netprice-${position.id}`}>Net Price</Label>
          <Input 
            id={`pos-netprice-${position.id}`} 
            type="number" 
            step="0.01" 
            value={position.einzelNetto || 0}
            onChange={(e) => {
              const value = e.target.value === '' ? 0 : parseFloat(e.target.value);
              onChange(position.id, "einzelNetto", isNaN(value) ? 0 : value);
            }}
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor={`pos-vat-${position.id}`}>VAT (%)</Label>
          <Input 
            id={`pos-vat-${position.id}`} 
            type="number" 
            step="0.1" 
            value={position.vat || 0}
            onChange={(e) => {
              const value = e.target.value === '' ? 0 : parseFloat(e.target.value);
              onChange(position.id, "vat", isNaN(value) ? 0 : value);
            }}
          />
        </div>
      </div>
      
      <div className="mt-4 grid grid-cols-3 gap-4">
        <div className="text-sm">
          <div className="font-medium">Net:</div>
          <div>€{netSum.toFixed(2)}</div>
        </div>
        <div className="text-sm">
          <div className="font-medium">VAT:</div>
          <div>€{vatAmount.toFixed(2)}</div>
        </div>
        <div className="text-sm">
          <div className="font-medium">Gross:</div>
          <div>€{grossSum.toFixed(2)}</div>
        </div>
      </div>

      <div className="mt-4">
        <Label htmlFor={`pos-text-${position.id}`}>Text</Label>
        <Textarea 
          id={`pos-text-${position.id}`} 
          placeholder="Position description"
          rows={2}
          className="mt-1"
          value={position.text}
          onChange={(e) => onChange(position.id, "s_text", e.target.value)}
        />
      </div>
      
      <div className="mt-4">
        <h5 className="font-medium mb-2">Period of Performance</h5>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <Select 
              value={position.periodOfPerformanceType || "SEEABOVE"}
              onValueChange={(value) => onChange(position.id, "periodOfPerformanceType", value)}
            >
              <SelectTrigger>
                <SelectValue placeholder="Select type" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="SEEABOVE">See Above</SelectItem>
                <SelectItem value="OWN">Own Period</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div>
            <Popover>
              <PopoverTrigger asChild>
                <Button
                  variant={"outline"}
                  className="w-full justify-start text-left font-normal"
                  disabled={position.periodOfPerformanceType !== "OWN"}
                >
                  <CalendarIcon className="mr-2 h-4 w-4" />
                  {position.periodOfPerformanceBegin ? (
                    <span>{new Date(position.periodOfPerformanceBegin).toLocaleDateString()}</span>
                  ) : (
                    <span>From</span>
                  )}
                </Button>
              </PopoverTrigger>
              <PopoverContent className="w-auto p-0">
                <Calendar 
                  mode="single" 
                  selected={position.periodOfPerformanceBegin ? new Date(position.periodOfPerformanceBegin) : undefined}
                  onSelect={(date) => onChange(position.id, "periodOfPerformanceBegin", date)}
                  disabled={position.periodOfPerformanceType !== "OWN"}
                />
              </PopoverContent>
            </Popover>
          </div>
          <div>
            <Popover>
              <PopoverTrigger asChild>
                <Button
                  variant={"outline"}
                  className="w-full justify-start text-left font-normal"
                  disabled={position.periodOfPerformanceType !== "OWN"}
                >
                  <CalendarIcon className="mr-2 h-4 w-4" />
                  {position.periodOfPerformanceEnd ? (
                    <span>{new Date(position.periodOfPerformanceEnd).toLocaleDateString()}</span>
                  ) : (
                    <span>To</span>
                  )}
                </Button>
              </PopoverTrigger>
              <PopoverContent className="w-auto p-0">
                <Calendar 
                  mode="single" 
                  selected={position.periodOfPerformanceEnd ? new Date(position.periodOfPerformanceEnd) : undefined}
                  onSelect={(date) => onChange(position.id, "periodOfPerformanceEnd", date)}
                  disabled={position.periodOfPerformanceType !== "OWN"}
                />
              </PopoverContent>
            </Popover>
          </div>
        </div>
      </div>

      {/* Cost Assignment section would go here - simplified for now */}
      <div className="mt-4 p-3 border border-dashed rounded-md border-muted-foreground/20">
        <div className="text-sm text-muted-foreground">
          Cost assignments would be configured here
        </div>
      </div>
    </div>
  );
});

export default InvoicePositionRow;