"use client";

import React from "react";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";

interface FormCardProps {
  title: string;
  description?: string;
  children: React.ReactNode;
  onCancel?: () => void;
  onDelete?: () => void;
  onSave?: () => void;
  onClone?: () => void;
  isNew?: boolean;
  cancelLabel?: string;
  deleteLabel?: string;
  saveLabel?: string;
  cloneLabel?: string;
  className?: string;
  footerClassName?: string;
}

export default function FormCard({
  title,
  description,
  children,
  onCancel,
  onDelete,
  onSave,
  onClone,
  isNew = false,
  cancelLabel = "Cancel",
  deleteLabel = "Delete",
  saveLabel = "Save",
  cloneLabel = "Clone",
  className = "",
  footerClassName = "",
}: FormCardProps) {
  return (
    <Card className={className}>
      <CardHeader>
        <CardTitle className="text-2xl">{title}</CardTitle>
        {description && <CardDescription>{description}</CardDescription>}
      </CardHeader>
      <CardContent>
        {children}
      </CardContent>
      <CardFooter className={`flex justify-between border-t p-4 ${footerClassName}`}>
        <div>
          {onCancel && (
            <Button variant="outline" className="mr-2" onClick={onCancel}>
              {cancelLabel}
            </Button>
          )}
          {onDelete && (
            <Button variant="destructive" onClick={onDelete}>
              {deleteLabel}
            </Button>
          )}
        </div>
        <div>
          {onClone && (
            <Button variant="outline" className="mr-2" onClick={onClone}>
              {cloneLabel}
            </Button>
          )}
          {onSave && (
            <Button variant="default" onClick={onSave}>
              {isNew ? "Create" : saveLabel}
            </Button>
          )}
        </div>
      </CardFooter>
    </Card>
  );
}