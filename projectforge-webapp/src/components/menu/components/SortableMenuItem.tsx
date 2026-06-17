import React from 'react';
import { useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faEllipsisV, faMinus, faTrash } from '@fortawesome/free-solid-svg-icons';
import { Button } from 'reactstrap';
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
    opacity: isDragging ? 0.4 : 1,
  };

  const isInGroup = container !== 'favorites';
  const itemId = getItemId(item);

  return (
    <div
      ref={setNodeRef}
      style={style}
      className={`${styles.menuItem} ${isDragging ? styles.dragging : ''}`}
      {...attributes}
      {...listeners}
    >
      <div className={styles.menuItemContent}>
        <FontAwesomeIcon icon={faEllipsisV} className={styles.dragHandle} />
        <span className={styles.itemTitle}>{item.title}</span>
        {isInGroup ? (
          <Button
            color="link"
            className={styles.actionButton}
            onClick={(e) => { e.stopPropagation(); onRemove(itemId, container); }}
            onMouseDown={(e) => e.stopPropagation()}
            title={translations.removeFromGroup || 'Remove from group'}
          >
            <FontAwesomeIcon icon={faMinus} />
          </Button>
        ) : (
          <Button
            color="link"
            className={styles.actionButton}
            onClick={(e) => { e.stopPropagation(); onRemove(itemId); }}
            onMouseDown={(e) => e.stopPropagation()}
            title={translations.removeFromFavorites || 'Remove from favorites'}
          >
            <FontAwesomeIcon icon={faTrash} />
          </Button>
        )}
      </div>
    </div>
  );
}
