import { useCallback } from 'react';
import { DragStartEvent, DragEndEvent } from '@dnd-kit/core';
import { MenuCustomizerState, MenuAction, DragData, MenuItem } from './menuCustomizerTypes';
import { getItemId } from './menuCustomizerUtils';

export function useMenuDragDrop(
  state: MenuCustomizerState,
  dispatch: React.Dispatch<MenuAction>,
) {
  const handleDragStart = useCallback((event: DragStartEvent) => {
    const data = event.active.data.current as DragData | undefined;
    if (data?.item) {
      dispatch({ type: 'DRAG_START', payload: { id: String(event.active.id), item: data.item } });
    }
  }, [dispatch]);

  const handleDragEnd = useCallback((event: DragEndEvent) => {
    dispatch({ type: 'DRAG_END' });

    const { active, over } = event;
    if (!over || active.id === over.id) return;

    const activeData = active.data.current as DragData | undefined;
    const overData = over.data.current as DragData | undefined;
    if (!activeData?.item) return;

    const sourceContainer = activeData.container;
    const targetContainer = overData?.container || 'favorites';

    // Don't drop into template
    if (targetContainer === 'template') return;

    // Prevent groups from being dropped into other groups
    if (activeData.type === 'group' && targetContainer !== 'favorites') return;

    // From template: add to target
    if (sourceContainer === 'template') {
      const targetIndex = overData?.item
        ? getItemIndex(state.customMenu, targetContainer, overData.item)
        : getEndIndex(state.customMenu, targetContainer);
      dispatch({
        type: 'ADD_FROM_TEMPLATE',
        payload: { item: activeData.item, targetContainer, targetIndex: targetIndex !== -1 ? targetIndex : getEndIndex(state.customMenu, targetContainer) },
      });
      return;
    }

    // Same container: reorder
    if (sourceContainer === targetContainer) {
      const oldIndex = getItemIndex(state.customMenu, sourceContainer, activeData.item);
      let newIndex = overData?.item
        ? getItemIndex(state.customMenu, targetContainer, overData.item)
        : -1;

      // Dropped on the container background (green) → move to end
      if (newIndex === -1) {
        newIndex = getEndIndex(state.customMenu, targetContainer) - 1;
      }

      if (oldIndex !== -1 && newIndex !== -1 && oldIndex !== newIndex) {
        dispatch({ type: 'REORDER', payload: { container: sourceContainer, oldIndex, newIndex } });
      }
      return;
    }

    // Cross-container: move
    const sourceIndex = getItemIndex(state.customMenu, sourceContainer, activeData.item);
    const targetIndex = overData?.item
      ? getItemIndex(state.customMenu, targetContainer, overData.item)
      : getEndIndex(state.customMenu, targetContainer);

    if (sourceIndex !== -1) {
      dispatch({
        type: 'MOVE_TO_CONTAINER',
        payload: {
          item: activeData.item,
          sourceContainer,
          sourceIndex,
          targetContainer,
          targetIndex: targetIndex !== -1 ? targetIndex : getEndIndex(state.customMenu, targetContainer),
        },
      });
    }
  }, [state.customMenu, dispatch]);

  return { handleDragStart, handleDragEnd };
}

function getItemIndex(customMenu: MenuItem[], container: string, item: MenuItem): number {
  const itemId = getItemId(item);
  if (!itemId) return -1;
  if (container === 'favorites') {
    return customMenu.findIndex(i => getItemId(i) === itemId);
  }
  const group = customMenu.find(g => getItemId(g) === container);
  return group?.subMenu?.findIndex(i => getItemId(i) === itemId) ?? -1;
}

function getEndIndex(customMenu: MenuItem[], container: string): number {
  if (container === 'favorites') return customMenu.length;
  const group = customMenu.find(g => getItemId(g) === container);
  return group?.subMenu?.length || 0;
}
