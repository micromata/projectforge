/* eslint-disable */
import React, { useState, useEffect } from 'react';
import { Card, CardHeader, CardBody, Button, Alert } from 'reactstrap';
import {
    DndContext,
    DragOverlay,
    useSensor,
    useSensors,
    PointerSensor,
    KeyboardSensor,
    closestCenter,
    DragStartEvent,
    DragEndEvent,
    DragOverEvent,
    useDroppable,
} from '@dnd-kit/core';
import {
    SortableContext,
    sortableKeyboardCoordinates,
    verticalListSortingStrategy,
    useSortable,
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faPlus, faArrowDown, faArrowUp, faMinus, faEllipsisV, faPencilAlt, faSave, faUndo, faTrash, faFolder } from '@fortawesome/free-solid-svg-icons';
import { baseRestURL, handleHTTPErrors } from '../../utilities/rest';
import LoadingContainer from '../design/loading-container';
import styles from './MenuCustomizer.module.scss';

/**
 * Component that allows users to customize their menu via drag and drop.
 * Allows to:
 * - Drag items from main menu to favorites
 * - Drag items around within favorites menu
 * - Create groups and add items to groups
 * - Save the customized menu
 */
function MenuCustomizer() {
    const [loading, setLoading] = useState(true);
    const [menuItems, setMenuItems] = useState({ mainMenu: [], favoritesMenu: [] });
    const [customMenu, setCustomMenu] = useState([]);
    const [newGroupName, setNewGroupName] = useState('');
    const [editingGroup, setEditingGroup] = useState(null);
    const [showGroupInput, setShowGroupInput] = useState(false);
    const [error, setError] = useState(null);
    const [success, setSuccess] = useState(null);
    const [activeId, setActiveId] = useState(null);
    
    const sensors = useSensors(
        useSensor(PointerSensor, {
            activationConstraint: {
                distance: 8,
            },
        }),
        useSensor(KeyboardSensor, {
            coordinateGetter: sortableKeyboardCoordinates,
        })
    );

    const loadMenuData = () => {
        setLoading(true);
        // Direct URL call to prevent double URL issue
        fetch(`${baseRestURL}/menu`, {
            method: 'GET',
            credentials: 'include',
            headers: {
                Accept: 'application/json',
            },
        })
            .then(handleHTTPErrors)
            .then((response) => response.json())
            .then((json) => {
                // Keep original menu structure for rendering categories
                const menuStructure = json.mainMenu.menuItems || [];

                // Also create a flattened list for internal operations
                const flattenMenuItems = (items) => {
                    let result = [];
                    if (!items) return result;

                    items.forEach((item) => {
                        result.push(item);
                        if (item.subMenu && item.subMenu.length > 0) {
                            result = result.concat(flattenMenuItems(item.subMenu));
                        }
                    });
                    return result;
                };

                const allMenuItems = flattenMenuItems(menuStructure);

                setMenuItems({
                    // Store both the structured and flattened menu items
                    mainMenu: allMenuItems || [],
                    mainMenuStructured: menuStructure || [],
                    favoritesMenu: json.favoritesMenu.menuItems || [],
                });
                setCustomMenu(json.favoritesMenu.menuItems || []);
                setLoading(false);
            })
            .catch((err) => {
                // eslint-disable-next-line no-console
                console.error('Error loading menu data:', err);
                setError('Error loading menu data. Please try again.');
                setLoading(false);
            });
    };

    useEffect(() => {
        loadMenuData();
    }, []);

    const handleDragStart = (event) => {
        setActiveId(event.active.id);
    };

    const handleDragEnd = (event) => {
        setActiveId(null);
        
        const { active, over } = event;
        
        if (!over) {
            return;
        }

        const activeId = active.id;
        const overId = over.id;
        

        // Determine containers based on our data structure
        const activeData = active.data.current;
        const overData = over.data.current;
        
        // Check if we're trying to drag a group into another group - this is not allowed
        if (activeData?.type === 'group' && overData?.groupId) {
            return; // Prevent groups from being dropped into other groups
        }
        
        let sourceContainer = 'mainMenu';
        let destContainer = 'favorites';
        
        // Determine source container
        if (activeData?.groupId) {
            sourceContainer = `group-${activeData.groupId}`;
        } else if (activeData?.isMainMenu) {
            sourceContainer = 'mainMenu';
        } else {
            sourceContainer = 'favorites';
        }
        
        // Determine destination container
        if (overData?.groupId) {
            destContainer = `group-${overData.groupId}`;
        } else if (overData?.isMainMenu) {
            destContainer = 'mainMenu';
        } else if (overId.toString().startsWith('group-')) {
            destContainer = overId.toString();
        } else {
            destContainer = 'favorites';
        }
        
        // For groups, only allow reordering within favorites (top level)
        if (activeData?.type === 'group' && destContainer !== 'favorites') {
            return;
        }

        if (sourceContainer === destContainer) {
            // Same container reordering
            handleSameContainerReorder(sourceContainer, activeId, overId);
        } else {
            // Cross-container move
            handleCrossContainerMove(sourceContainer, destContainer, activeId, overId);
        }
    };
    
    const handleSameContainerReorder = (containerId, activeId, overId) => {
        
        if (containerId === 'favorites') {
            // Reordering in main favorites
            const oldIndex = customMenu.findIndex(item => getItemId(item) === activeId);
            const newIndex = customMenu.findIndex(item => getItemId(item) === overId);
            
            if (oldIndex !== -1 && newIndex !== -1 && oldIndex !== newIndex) {
                const newCustomMenu = [...customMenu];
                const [movedItem] = newCustomMenu.splice(oldIndex, 1);
                newCustomMenu.splice(newIndex, 0, movedItem);
                setCustomMenu(newCustomMenu);
            }
        } else if (containerId.startsWith('group-')) {
            // Reordering within a group
            const groupId = containerId.replace('group-', '');
            const groupIndex = customMenu.findIndex(item => getItemId(item) === groupId);
            
            if (groupIndex !== -1 && customMenu[groupIndex].subMenu) {
                const subMenu = customMenu[groupIndex].subMenu;
                const oldIndex = subMenu.findIndex(item => getItemId(item) === activeId);
                const newIndex = subMenu.findIndex(item => getItemId(item) === overId);
                
                if (oldIndex !== -1 && newIndex !== -1 && oldIndex !== newIndex) {
                    const newCustomMenu = [...customMenu];
                    const newSubMenu = [...subMenu];
                    const [movedItem] = newSubMenu.splice(oldIndex, 1);
                    newSubMenu.splice(newIndex, 0, movedItem);
                    
                    newCustomMenu[groupIndex] = {
                        ...newCustomMenu[groupIndex],
                        subMenu: newSubMenu,
                    };
                    
                    setCustomMenu(newCustomMenu);
                }
            }
        }
    };
    
    const handleCrossContainerMove = (sourceContainer, destContainer, activeId, overId) => {
        // Find the item being moved
        let sourceItem = null;
        let sourceIndex = -1;
        let sourceGroupIndex = -1;
        
        if (sourceContainer === 'mainMenu') {
            sourceItem = menuItems.mainMenu.find(item => item.id === activeId);
        } else if (sourceContainer === 'favorites') {
            sourceIndex = customMenu.findIndex(item => getItemId(item) === activeId);
            if (sourceIndex !== -1) {
                sourceItem = customMenu[sourceIndex];
            }
        } else if (sourceContainer.startsWith('group-')) {
            const groupId = sourceContainer.replace('group-', '');
            sourceGroupIndex = customMenu.findIndex(item => getItemId(item) === groupId);
            if (sourceGroupIndex !== -1 && customMenu[sourceGroupIndex].subMenu) {
                sourceIndex = customMenu[sourceGroupIndex].subMenu.findIndex(item => getItemId(item) === activeId);
                if (sourceIndex !== -1) {
                    sourceItem = customMenu[sourceGroupIndex].subMenu[sourceIndex];
                }
            }
        }
        
        if (!sourceItem) {
            return;
        }
        
        // Handle the move
        const newCustomMenu = [...customMenu];
        
        // Remove from source
        if (sourceContainer === 'favorites') {
            newCustomMenu.splice(sourceIndex, 1);
        } else if (sourceContainer.startsWith('group-') && sourceGroupIndex !== -1) {
            newCustomMenu[sourceGroupIndex].subMenu.splice(sourceIndex, 1);
        }
        
        // Add to destination at the correct position
        if (destContainer === 'favorites') {
            // Find the position to insert based on overId
            const overIndex = newCustomMenu.findIndex(item => getItemId(item) === overId);
            if (overIndex !== -1) {
                newCustomMenu.splice(overIndex, 0, { ...sourceItem });
            } else {
                newCustomMenu.push({ ...sourceItem });
            }
        } else if (destContainer.startsWith('group-')) {
            // Add to a group at the correct position
            const destGroupId = destContainer.replace('group-', '');
            const destGroupIndex = newCustomMenu.findIndex(item => getItemId(item) === destGroupId);
            
            if (destGroupIndex !== -1) {
                if (!newCustomMenu[destGroupIndex].subMenu) {
                    newCustomMenu[destGroupIndex].subMenu = [];
                }
                
                const subMenu = newCustomMenu[destGroupIndex].subMenu;
                const overIndex = subMenu.findIndex(item => getItemId(item) === overId);
                
                if (overIndex !== -1) {
                    subMenu.splice(overIndex, 0, { ...sourceItem });
                } else {
                    subMenu.push({ ...sourceItem });
                }
            }
        }
        
        setCustomMenu(newCustomMenu);
    };
    
    const getItemId = (item) => {
        return item.id || item.key || `fallback-${Date.now()}`;
    };

    const addNewGroup = () => {
        if (!newGroupName.trim()) {
            setError('Group name cannot be empty');
            return;
        }

        const groupId = `custom_group_${Date.now()}`;
        const newGroup = {
            id: groupId,
            title: newGroupName,
            subMenu: [],
        };

        setCustomMenu([...customMenu, newGroup]);
        setNewGroupName('');
        setShowGroupInput(false);
        setError(null);
    };

    const editGroup = (groupId, newName) => {
        if (!newName.trim()) {
            setError('Group name cannot be empty');
            return;
        }

        const newCustomMenu = customMenu.map((item) => {
            const itemId = item.id || item.key;
            if (itemId === groupId) {
                return { ...item, title: newName };
            }
            return item;
        });

        setCustomMenu(newCustomMenu);
        setEditingGroup(null);
        setError(null);
    };

    const removeItem = (itemId, groupId = null) => {
        if (groupId) {
            // Remove item from group
            const newCustomMenu = customMenu.map((item) => {
                const currentItemId = item.id || item.key;
                if (currentItemId === groupId && item.subMenu) {
                    return {
                        ...item,
                        subMenu: item.subMenu.filter((subItem) => subItem.id !== itemId),
                    };
                }
                return item;
            });
            setCustomMenu(newCustomMenu);
        } else {
            // Remove item from top level
            setCustomMenu(customMenu.filter((item) => {
                const currentItemId = item.id || item.key;
                return currentItemId !== itemId;
            }));
        }
    };

    const saveMenu = () => {
        setLoading(true);
        fetch(`${baseRestURL}/menu/customized`, {
            method: 'POST',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ favoritesMenu: customMenu }),
        })
            .then(handleHTTPErrors)
            .then((response) => response.json())
            .then(() => {
                setSuccess('Menu saved successfully');
                setLoading(false);
                // Refresh the menu data
                loadMenuData();
            })
            .catch((err) => {
                // eslint-disable-next-line no-console
                console.error('Error saving menu:', err);
                setError('Error saving menu. Please try again.');
                setLoading(false);
            });
    };

    const resetMenu = () => {
        // eslint-disable-next-line no-alert
        if (window.confirm('Are you sure you want to reset your menu to default?')) {
            setLoading(true);
            fetch(`${baseRestURL}/menu/reset`, {
                method: 'POST',
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({}),
            })
                .then(handleHTTPErrors)
                .then((response) => response.json())
                .then(() => {
                    setSuccess('Menu reset successfully');
                    setLoading(false);
                    // Refresh the menu data
                    loadMenuData();
                })
                .catch((err) => {
                    // eslint-disable-next-line no-console
                    console.error('Error resetting menu:', err);
                    setError('Error resetting menu. Please try again.');
                    setLoading(false);
                });
        }
    };

    // Create sequential indexes for main menu items (memoized to avoid recalculation)
    const mainMenuIndexMap = React.useMemo(() => {
        let currentIndex = 0;
        const indexMap = new Map();

        if (menuItems.mainMenuStructured) {
            menuItems.mainMenuStructured.forEach((category) => {
                if (category.subMenu) {
                    category.subMenu.forEach((item) => {
                        indexMap.set(item.id, currentIndex);
                        currentIndex += 1;
                    });
                }
            });
        }

        return indexMap;
    }, [menuItems.mainMenuStructured]);

    // Sortable item component for @dnd-kit
    const SortableItem = ({ item, groupId = null, isMainMenu = false }) => {
        const itemId = getItemId(item);
        const {
            attributes,
            listeners,
            setNodeRef,
            transform,
            transition,
            isDragging,
        } = useSortable({ 
            id: itemId,
            data: {
                type: 'item',
                item,
                groupId,
                isMainMenu,
            }
        });

        const style = {
            transform: CSS.Transform.toString(transform),
            transition,
            opacity: isDragging ? 0.5 : 1,
        };


        return (
            <div
                ref={setNodeRef}
                style={style}
                className={isDragging ? `${styles.menuItem} ${styles.dragging}` : styles.menuItem}
                {...attributes}
                {...listeners}
            >
                <div className={styles.menuItemContent}>
                    <FontAwesomeIcon
                        icon={faEllipsisV}
                        className={styles.dragHandle}
                    />
                    <span className={styles.itemTitle}>{item.title}</span>
                    {groupId && (
                        <Button
                            color="link"
                            className={styles.actionButton}
                            onClick={() => removeItem(item.id, groupId)}
                            title="Remove from group"
                        >
                            <FontAwesomeIcon icon={faMinus} />
                        </Button>
                    )}
                    {!groupId && !isMainMenu && (
                        <Button
                            color="link"
                            className={styles.actionButton}
                            onClick={() => removeItem(item.id)}
                            title="Remove from favorites"
                        >
                            <FontAwesomeIcon icon={faTrash} />
                        </Button>
                    )}
                </div>
            </div>
        );
    };

    const DroppableArea = ({ id, children, className }) => {
        const {
            setNodeRef,
            isOver,
        } = useDroppable({
            id: id,
        });

        return (
            <div 
                ref={setNodeRef}
                data-droppable-id={id} 
                className={`${className} ${isOver ? styles.draggingOver : ''}`}
            >
                {children}
            </div>
        );
    };

    // Function to render menu in category columns with new @dnd-kit
    const renderCategoryColumns = (menuStructure) => {
        const numColumns = 4;
        const itemsPerColumn = Math.ceil(menuStructure.length / numColumns);
        const columns = [];

        // Create all items for SortableContext
        const allMainMenuItems = [];
        menuStructure.forEach(category => {
            if (category.subMenu) {
                category.subMenu.forEach(item => {
                    allMainMenuItems.push(item);
                });
            }
        });

        for (let i = 0; i < numColumns; i += 1) {
            const startIndex = i * itemsPerColumn;
            const endIndex = Math.min(startIndex + itemsPerColumn, menuStructure.length);
            const columnItems = menuStructure.slice(startIndex, endIndex);

            columns.push(
                <div key={`column-${i}`} className={styles.categoryColumn}>
                    {columnItems.map((category) => (
                        <div key={category.id} className={styles.categoryContainer}>
                            <button type="button" className={styles.categoryTitle}>
                                {category.title}
                            </button>
                            <div className="collapse show">
                                <ul className={styles.categoryLinks}>
                                    {category.subMenu && category.subMenu.map((item) => (
                                        <li key={item.id} className={styles.categoryLink}>
                                            <SortableItem 
                                                item={item} 
                                                isMainMenu={true}
                                            />
                                        </li>
                                    ))}
                                    {(!category.subMenu || category.subMenu.length === 0) && (
                                        <li className={styles.categoryLink}>
                                            <div className={styles.emptyGroup}>
                                                <p>No items in this category</p>
                                            </div>
                                        </li>
                                    )}
                                </ul>
                            </div>
                        </div>
                    ))}
                </div>,
            );
        }

        return columns;
    };

    const SortableGroup = ({ item }) => {
        const groupId = getItemId(item);
        const {
            attributes,
            listeners,
            setNodeRef,
            transform,
            transition,
            isDragging,
        } = useSortable({ 
            id: groupId,
            data: {
                type: 'group',
                item,
            }
        });

        const style = {
            transform: CSS.Transform.toString(transform),
            transition,
            opacity: isDragging ? 0.5 : 1,
        };

        // Get all item IDs for this group's SortableContext
        const groupItemIds = (item.subMenu || []).map(subItem => getItemId(subItem));

        return (
            <div
                ref={setNodeRef}
                style={style}
                className={`${styles.menuItem} ${styles.groupItem} ${isDragging ? styles.dragging : ''}`}
            >
                <div className={styles.menuItemContent}>
                    <div {...attributes} {...listeners}>
                        <FontAwesomeIcon icon={faEllipsisV} className={styles.dragHandle} />
                    </div>
                    <FontAwesomeIcon icon={faFolder} className={styles.folderIcon} />

                    {editingGroup === groupId ? (
                        <div className={styles.groupEditForm}>
                            <input
                                type="text"
                                className={styles.groupNameInput}
                                value={newGroupName}
                                onChange={(e) => setNewGroupName(e.target.value)}
                                autoFocus
                            />
                            <Button
                                color="primary"
                                size="sm"
                                className={styles.saveGroupButton}
                                onClick={() => editGroup(groupId, newGroupName)}
                            >
                                <FontAwesomeIcon icon={faSave} />
                            </Button>
                            <Button
                                color="secondary"
                                size="sm"
                                onClick={() => setEditingGroup(null)}
                            >
                                <FontAwesomeIcon icon={faUndo} />
                            </Button>
                        </div>
                    ) : (
                        <>
                            <span className={styles.itemTitle}>{item.title}</span>
                            <Button
                                color="link"
                                className={styles.actionButton}
                                onClick={() => {
                                    setEditingGroup(groupId);
                                    setNewGroupName(item.title);
                                }}
                                title="Edit group name"
                            >
                                <FontAwesomeIcon icon={faPencilAlt} />
                            </Button>
                            <Button
                                color="link"
                                className={styles.actionButton}
                                onClick={() => removeItem(groupId)}
                                title="Remove group"
                            >
                                <FontAwesomeIcon icon={faTrash} />
                            </Button>
                        </>
                    )}
                </div>

                <SortableContext 
                    id={`group-${groupId}`}
                    items={groupItemIds} 
                    strategy={verticalListSortingStrategy}
                >
                    <DroppableArea id={`group-${groupId}`} className={styles.groupContent}>
                        {item.subMenu && item.subMenu.map((subItem) => {
                            return (
                                <SortableItem 
                                    key={getItemId(subItem)}
                                    item={subItem} 
                                    groupId={groupId}
                                />
                            );
                        })}
                        {(!item.subMenu || item.subMenu.length === 0) && (
                            <div className={styles.emptyGroup}>
                                <p>Empty group - drop items here</p>
                                <p>Drag menu items from the left or move items between groups</p>
                            </div>
                        )}
                    </DroppableArea>
                </SortableContext>
            </div>
        );
    };

    if (loading) {
        return <LoadingContainer />;
    }

    return (
        <div className={styles.menuCustomizer}>
            <h2>Customize Your Menu</h2>
            {error && <Alert color="danger" timeout={5000}>{error}</Alert>}
            {success && <Alert color="success" timeout={5000}>{success}</Alert>}

            <div className={styles.instructionsBox}>
                <p>
                    Customize your menu by dragging items from the available menu
                    items on the left to your custom menu on the right.
                </p>
                <ul>
                    <li>Drag items from available menu to your custom menu</li>
                    <li>Create groups to organize your menu items</li>
                    <li>Drag items into groups, between groups, or from groups to main level</li>
                    <li>Reorder items within groups by dragging them</li>
                    <li>Reorder items in your main custom menu by dragging them</li>
                    <li>Remove items with the trash/minus icon</li>
                    <li>Save your changes when finished customizing</li>
                </ul>
            </div>

            <div className={styles.menuContainer}>
                <DndContext
                    sensors={sensors}
                    collisionDetection={closestCenter}
                    onDragStart={handleDragStart}
                    onDragEnd={handleDragEnd}
                >
                    <div className={styles.menuSection}>
                        <Card>
                            <CardHeader>Available Menu Items</CardHeader>
                            <CardBody>
                                <div className={`${styles.menuList} ${styles.mainMenuList}`}>
                                    {menuItems.mainMenuStructured && renderCategoryColumns(menuItems.mainMenuStructured)}
                                </div>
                            </CardBody>
                        </Card>
                    </div>

                    <div className={styles.menuControls}>
                        <div className={styles.controlArrow}>
                            <FontAwesomeIcon icon={faArrowDown} />
                            <FontAwesomeIcon icon={faArrowUp} />
                        </div>
                    </div>

                    <div className={styles.menuSection}>
                        <Card>
                            <CardHeader>
                                Your Custom Menu
                                <div className={styles.headerActions}>
                                    {!showGroupInput ? (
                                        <Button
                                            color="primary"
                                            size="sm"
                                            onClick={() => setShowGroupInput(true)}
                                            title="Add a new group"
                                        >
                                            <FontAwesomeIcon icon={faPlus} />
                                            <span>Add Group</span>
                                        </Button>
                                    ) : (
                                        <div className={styles.groupForm}>
                                            <input
                                                type="text"
                                                className={styles.groupNameInput}
                                                value={newGroupName}
                                                onChange={(e) => setNewGroupName(e.target.value)}
                                                placeholder="Group name"
                                                autoFocus
                                            />
                                            <Button
                                                color="primary"
                                                size="sm"
                                                onClick={addNewGroup}
                                            >
                                                Add
                                            </Button>
                                            <Button
                                                color="secondary"
                                                size="sm"
                                                onClick={() => {
                                                    setShowGroupInput(false);
                                                    setNewGroupName('');
                                                }}
                                            >
                                                Cancel
                                            </Button>
                                        </div>
                                    )}
                                </div>
                            </CardHeader>
                            <CardBody>
                                <SortableContext 
                                    id="favorites"
                                    items={customMenu.map(item => getItemId(item))} 
                                    strategy={verticalListSortingStrategy}
                                >
                                    <DroppableArea id="favorites" className={styles.menuList}>
                                        {customMenu.map((item) => {
                                            return item.subMenu ? (
                                                <SortableGroup key={getItemId(item)} item={item} />
                                            ) : (
                                                <SortableItem key={getItemId(item)} item={item} />
                                            );
                                        })}
                                        {customMenu.length === 0 && (
                                            <div className={styles.emptyMenu}>
                                                <p>Your custom menu is empty.</p>
                                                <p>Drag items from available menu items.</p>
                                            </div>
                                        )}
                                    </DroppableArea>
                                </SortableContext>
                            </CardBody>
                        </Card>
                    </div>

                    <DragOverlay>
                        {activeId ? (
                            <div className={`${styles.menuItem} ${styles.dragging}`}>
                                <div className={styles.menuItemContent}>
                                    <FontAwesomeIcon icon={faEllipsisV} className={styles.dragHandle} />
                                    <span className={styles.itemTitle}>
                                        {/* Find the actual item title */}
                                        {(() => {
                                            // Try to find in main menu
                                            const mainItem = menuItems.mainMenu?.find(item => item.id === activeId);
                                            if (mainItem) return mainItem.title;
                                            
                                            // Try to find in custom menu (flat search)
                                            const findInCustomMenu = (items) => {
                                                for (const item of items) {
                                                    if (getItemId(item) === activeId) return item.title;
                                                    if (item.subMenu) {
                                                        const found = findInCustomMenu(item.subMenu);
                                                        if (found) return found;
                                                    }
                                                }
                                                return null;
                                            };
                                            
                                            const customItem = findInCustomMenu(customMenu);
                                            return customItem || 'Dragging...';
                                        })()}
                                    </span>
                                </div>
                            </div>
                        ) : null}
                    </DragOverlay>
                </DndContext>
            </div>

            <div className={styles.actionButtons}>
                <Button color="primary" onClick={saveMenu}>
                    <FontAwesomeIcon icon={faSave} />
                    <span>Save Changes</span>
                </Button>
                <Button color="secondary" onClick={resetMenu}>
                    <FontAwesomeIcon icon={faUndo} />
                    <span>Reset to Default</span>
                </Button>
            </div>
        </div>
    );
}

export default MenuCustomizer;
