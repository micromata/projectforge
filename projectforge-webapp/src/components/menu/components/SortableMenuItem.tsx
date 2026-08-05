import React from 'react';
import { useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faTimes } from '@fortawesome/free-solid-svg-icons';
import { MenuItem, DragData, Translations } from '../menuCustomizerTypes';
import { getItemId } from '../menuCustomizerUtils';
import styles from '../MenuCustomizer.module.scss';

interface Props {
  item: MenuItem;
  container: string;
  sortableId: string;
  translations: Translations;
  onRemove: (itemId: string, groupId?: string) => void;
}

export function SortableMenuItem({ item, container, sortableId, translations, onRemove }: Props) {
  const data: DragData = { type: 'item', item, container };
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: sortableId, data });

  const style: React.CSSProperties = {
    transform: CSS.Transform.toString(transform),
    transition: isDragging ? 'none' : (transition || undefined),
  };

  const isInGroup = container !== 'favorites';
  const itemId = getItemId(item);

  return (
    <div
      ref={setNodeRef}
      style={style}
      className={`${styles.favChip} ${isDragging ? styles.dragging : ''}`}
      {...attributes}
      {...listeners}
    >
      <span className={styles.favChipDot} />
      <span className={styles.favChipLabel}>{item.title}</span>
      <button
        type="button"
        className={styles.favChipRemove}
        onClick={(e) => { e.stopPropagation(); onRemove(itemId, isInGroup ? container : undefined); }}
        onMouseDown={(e) => e.stopPropagation()}
        title={isInGroup ? (translations.removeFromGroup || 'Remove') : (translations.removeFromFavorites || 'Remove')}
      >
        <FontAwesomeIcon icon={faTimes} />
      </button>
    </div>
  );
}
