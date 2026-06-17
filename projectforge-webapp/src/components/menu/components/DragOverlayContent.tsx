import React from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faEllipsisV, faFolder } from '@fortawesome/free-solid-svg-icons';
import { MenuItem } from '../menuCustomizerTypes';
import { isGroup } from '../menuCustomizerUtils';
import styles from '../MenuCustomizer.module.scss';

interface Props {
  item: MenuItem;
}

export function DragOverlayContent({ item }: Props) {
  return (
    <div className={styles.dragOverlayItem}>
      <FontAwesomeIcon icon={faEllipsisV} className={styles.dragHandle} />
      {isGroup(item) && <FontAwesomeIcon icon={faFolder} className={styles.folderIcon} />}
      <span>{item.title}</span>
    </div>
  );
}
