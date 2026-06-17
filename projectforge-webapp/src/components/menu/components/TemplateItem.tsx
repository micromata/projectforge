import React from 'react';
import { useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faEllipsisV } from '@fortawesome/free-solid-svg-icons';
import { MenuItem, DragData } from '../menuCustomizerTypes';
import { getItemId } from '../menuCustomizerUtils';
import styles from '../MenuCustomizer.module.scss';

interface Props {
  item: MenuItem;
}

export function TemplateItem({ item }: Props) {
  const sortableId = `tpl_${getItemId(item)}`;
  const data: DragData = { type: 'item', item, container: 'template' };

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
      </div>
    </div>
  );
}
