import React from 'react';
import { useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faCheck } from '@fortawesome/free-solid-svg-icons';
import { MenuItem, DragData, Translations } from '../menuCustomizerTypes';
import { getItemId } from '../menuCustomizerUtils';
import styles from '../MenuCustomizer.module.scss';

interface Props {
  item: MenuItem;
  isAdded: boolean;
  translations: Translations;
  onClickAdd: (item: MenuItem) => void;
}

export function TemplateChip({ item, isAdded, translations, onClickAdd }: Props) {
  const sortableId = `tpl_${getItemId(item)}`;
  const data: DragData = { type: 'item', item, container: 'template' };

  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: sortableId, data, disabled: isAdded });

  if (isAdded) {
    return (
      <div className={styles.chipAdded} title={translations.alreadyAdded || 'Already in your menu'}>
        <FontAwesomeIcon icon={faCheck} className={styles.chipCheck} size="xs" />
        <span className={styles.chipLabel}>{item.title}</span>
      </div>
    );
  }

  const style: React.CSSProperties = {
    transform: CSS.Transform.toString(transform),
    transition: isDragging ? 'none' : (transition || undefined),
    opacity: isDragging ? 0.45 : 1,
  };

  return (
    <div
      ref={setNodeRef}
      style={style}
      className={`${styles.chip} ${isDragging ? styles.dragging : ''}`}
      onClick={() => onClickAdd(item)}
      {...attributes}
      {...listeners}
    >
      <span className={styles.chipDot} />
      <span className={styles.chipLabel}>{item.title}</span>
    </div>
  );
}
