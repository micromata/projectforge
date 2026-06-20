import React, { useReducer, useEffect, useCallback } from 'react';
import { useDispatch } from 'react-redux';
import { loadMenu } from '../../actions/menu';
import {
  DndContext,
  DragOverlay,
  useSensor,
  useSensors,
  PointerSensor,
  KeyboardSensor,
  closestCenter,
  pointerWithin,
} from '@dnd-kit/core';
import { sortableKeyboardCoordinates } from '@dnd-kit/sortable';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faFloppyDisk, faRotateLeft } from '@fortawesome/free-solid-svg-icons';
import { menuCustomizerReducer, initialState } from './menuCustomizerReducer';
import { useMenuDragDrop } from './useMenuDragDrop';
import * as api from './menuCustomizerApi';
import { cleanMenuForSave } from './menuCustomizerUtils';
import { MenuItem } from './menuCustomizerTypes';
import { CustomMenuPanel } from './components/CustomMenuPanel';
import { TemplateMenuPanel } from './components/TemplateMenuPanel';
import { DragOverlayContent } from './components/DragOverlayContent';
import { Toast } from './components/Toast';
import LoadingContainer from '../design/loading-container';
import styles from './MenuCustomizer.module.scss';

function customCollisionDetection(args: Parameters<typeof pointerWithin>[0]) {
  const pointerCollisions = pointerWithin(args);
  if (pointerCollisions.length > 0) return pointerCollisions;
  return closestCenter(args);
}

function MenuCustomizer() {
  const [state, dispatch] = useReducer(menuCustomizerReducer, initialState);
  const reduxDispatch = useDispatch();
  const { handleDragStart, handleDragEnd } = useMenuDragDrop(state, dispatch);

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 8 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates }),
  );

  useEffect(() => {
    dispatch({ type: 'LOAD_START' });
    api.loadMenuData()
      .then(data => dispatch({ type: 'LOAD_SUCCESS', payload: data }))
      .catch(() => dispatch({ type: 'LOAD_FAILURE', payload: 'Error loading menu data.' }));
  }, []);

  useEffect(() => {
    if (state.toast) {
      const timer = setTimeout(() => dispatch({ type: 'SET_TOAST', payload: null }), 2200);
      return () => clearTimeout(timer);
    }
  }, [state.toast]);

  const handleSave = useCallback(async () => {
    dispatch({ type: 'SAVE_START' });
    try {
      await api.saveCustomMenu(cleanMenuForSave(state.customMenu));
      dispatch({ type: 'SAVE_SUCCESS' });
      reduxDispatch(loadMenu() as any);
    } catch {
      dispatch({ type: 'SET_ERROR', payload: state.translations.errorSavingMenu || 'Error saving menu.' });
    }
  }, [state.customMenu, state.translations, reduxDispatch]);

  const handleUndo = useCallback(() => dispatch({ type: 'UNDO' }), []);

  const handleLoadDefault = useCallback(async () => {
    try {
      const items = await api.loadDefaultMenu();
      dispatch({ type: 'SET_CUSTOM_MENU', payload: items });
      dispatch({ type: 'SET_TOAST', payload: state.translations.loadDefault || 'Default loaded' });
    } catch {
      dispatch({ type: 'SET_ERROR', payload: 'Error loading default menu.' });
    }
  }, [state.translations]);

  const handleReset = useCallback(async () => {
    if (!window.confirm(state.translations.confirmReset || 'Reset menu to default?')) return;
    dispatch({ type: 'LOAD_START' });
    try {
      await api.resetMenu();
      const data = await api.loadMenuData();
      dispatch({ type: 'LOAD_SUCCESS', payload: data });
      dispatch({ type: 'SET_TOAST', payload: state.translations.menuResetSuccessfully || 'Menü zurückgesetzt' });
      reduxDispatch(loadMenu() as any);
    } catch {
      dispatch({ type: 'LOAD_FAILURE', payload: 'Error resetting menu.' });
    }
  }, [state.translations, reduxDispatch]);

  const handleAddGroup = useCallback(() => {
    if (!state.newGroupName.trim()) {
      dispatch({ type: 'SET_ERROR', payload: state.translations.groupNameCannotBeEmpty || 'Group name cannot be empty' });
      return;
    }
    dispatch({ type: 'ADD_GROUP', payload: { name: state.newGroupName } });
  }, [state.newGroupName, state.translations]);

  const handleRemoveItem = useCallback((itemId: string, groupId?: string) => {
    dispatch({ type: 'REMOVE_ITEM', payload: { itemId, groupId } });
  }, []);

  const handleClickAdd = useCallback((item: MenuItem) => {
    dispatch({
      type: 'ADD_FROM_TEMPLATE',
      payload: { item, targetContainer: 'favorites', targetIndex: state.customMenu.length },
    });
  }, [state.customMenu.length]);

  if (state.loading) return <LoadingContainer className="" loading>{null}</LoadingContainer>;

  return (
    <DndContext
      sensors={sensors}
      collisionDetection={customCollisionDetection}
      onDragStart={handleDragStart}
      onDragEnd={handleDragEnd}
      autoScroll={{ threshold: { x: 0.1, y: 0.2 } }}
    >
      <div className={styles.menuCustomizer}>
        <div className={styles.header}>
          <div>
            <h1>{state.translations.title || 'Customize Your Menu'}</h1>
            <p>{state.translations.subtitle || ''}</p>
          </div>
        </div>

        {state.error && (
          <div style={{ padding: '0 28px 12px' }}>
            <div style={{ padding: '10px 14px', background: '#fdeeed', border: '1px solid #ecb3ae', borderRadius: 9, color: '#c44b43', fontSize: 13 }}>
              {state.error}
            </div>
          </div>
        )}

        <div className={styles.toolbar}>
          <button type="button" className={styles.btnPrimary} onClick={handleSave} disabled={!state.isDirty}>
            <FontAwesomeIcon icon={faFloppyDisk} size="sm" />
            {state.translations.saveChanges || 'Save'}
          </button>
          <button type="button" className={styles.btnSecondary} onClick={handleUndo} disabled={!state.isDirty}>
            <FontAwesomeIcon icon={faRotateLeft} size="sm" />
            {state.translations.undo || 'Undo'}
          </button>
          <button type="button" className={styles.btnWarning} onClick={handleLoadDefault}>
            {state.translations.loadDefault || 'Load Default'}
          </button>
          <button type="button" className={styles.btnDanger} onClick={handleReset}>
            {state.translations.resetToDefault || 'Reset'}
          </button>
        </div>

        <div className={styles.layout}>
          <CustomMenuPanel
            customMenu={state.customMenu}
            translations={state.translations}
            showGroupInput={state.showGroupInput}
            newGroupName={state.newGroupName}
            editingGroupId={state.editingGroupId}
            onAddGroup={handleAddGroup}
            onShowGroupInput={(show) => dispatch({ type: 'SET_SHOW_GROUP_INPUT', payload: show })}
            onGroupNameChange={(name) => dispatch({ type: 'SET_NEW_GROUP_NAME', payload: name })}
            onRemoveItem={handleRemoveItem}
            onStartEditGroup={(id) => dispatch({ type: 'SET_EDITING_GROUP', payload: id })}
            onSaveEditGroup={(groupId, name) => {
              if (!name.trim()) {
                dispatch({ type: 'SET_ERROR', payload: state.translations.groupNameCannotBeEmpty || 'Group name cannot be empty' });
                return;
              }
              dispatch({ type: 'RENAME_GROUP', payload: { groupId, name } });
            }}
            onCancelEditGroup={() => dispatch({ type: 'SET_EDITING_GROUP', payload: null })}
          />

          <TemplateMenuPanel
            mainMenuStructured={state.mainMenuStructured}
            customMenu={state.customMenu}
            translations={state.translations}
            search={state.search}
            collapsedCategories={state.collapsedCategories}
            onSearchChange={(value) => dispatch({ type: 'SET_SEARCH', payload: value })}
            onToggleCategory={(id) => dispatch({ type: 'TOGGLE_CATEGORY_COLLAPSE', payload: id })}
            onClickAdd={handleClickAdd}
          />
        </div>
      </div>

      <DragOverlay>
        {state.draggedItem ? <DragOverlayContent item={state.draggedItem} /> : null}
      </DragOverlay>

      {state.toast && <Toast message={state.toast} />}
    </DndContext>
  );
}

export default MenuCustomizer;
