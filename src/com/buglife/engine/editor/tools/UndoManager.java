package com.buglife.engine.editor.tools;

import com.buglife.engine.editor.data.LevelData;

import java.util.*;

/**
 * Undo/Redo system for the level editor.
 * Supports tile edits, entity changes, and compound operations.
 */
public class UndoManager {
    
    private static final int MAX_HISTORY = 100;
    
    private final Deque<EditAction> undoStack = new ArrayDeque<>();
    private final Deque<EditAction> redoStack = new ArrayDeque<>();
    private final List<Runnable> listeners = new ArrayList<>();
    
    private CompoundAction currentCompound = null;
    
    public interface EditAction {
        void undo(LevelData data);
        void redo(LevelData data);
        String getDescription();
    }
    
    // ========== COMPOUND ACTIONS ==========
    
    /**
     * Start a compound action (groups multiple edits into one undo step).
     */
    public void beginCompound(String description) {
        if (currentCompound != null) {
            endCompound();
        }
        currentCompound = new CompoundAction(description);
    }
    
    /**
     * End and commit the current compound action.
     */
    public void endCompound() {
        if (currentCompound != null && !currentCompound.actions.isEmpty()) {
            pushAction(currentCompound);
        }
        currentCompound = null;
    }
    
    // ========== ACTION REGISTRATION ==========
    
    /**
     * Record an action for undo/redo.
     */
    public void pushAction(EditAction action) {
        if (currentCompound != null) {
            currentCompound.actions.add(action);
        } else {
            undoStack.push(action);
            redoStack.clear();
            
            // Limit history size
            while (undoStack.size() > MAX_HISTORY) {
                undoStack.removeLast();
            }
            
            notifyListeners();
        }
    }

    /**
     * Execute an action and register it for undo.
     * The action should already be applied to the data.
     */
    public void execute(EditAction action, LevelData data) {
        if (action == null) return;
        pushAction(action);
    }
    
    // ========== UNDO/REDO ==========
    
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }
    
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }
    
    public void undo(LevelData data) {
        if (!canUndo()) return;
        
        EditAction action = undoStack.pop();
        action.undo(data);
        redoStack.push(action);
        notifyListeners();
    }
    
    public void redo(LevelData data) {
        if (!canRedo()) return;
        
        EditAction action = redoStack.pop();
        action.redo(data);
        undoStack.push(action);
        notifyListeners();
    }
    
    public String getUndoDescription() {
        return canUndo() ? undoStack.peek().getDescription() : null;
    }
    
    public String getRedoDescription() {
        return canRedo() ? redoStack.peek().getDescription() : null;
    }
    
    public void clear() {
        undoStack.clear();
        redoStack.clear();
        currentCompound = null;
        notifyListeners();
    }
    
    // ========== LISTENERS ==========
    
    public void addListener(Runnable listener) {
        listeners.add(listener);
    }
    
    private void notifyListeners() {
        listeners.forEach(Runnable::run);
    }
    
    // ========== ACTION IMPLEMENTATIONS ==========
    
    /**
     * Single tile change.
     */
    public static class TileAction implements EditAction {
        private final int x, y;
        private final int oldTile, newTile;
        
        public TileAction(int x, int y, int oldTile, int newTile) {
            this.x = x;
            this.y = y;
            this.oldTile = oldTile;
            this.newTile = newTile;
        }
        
        @Override
        public void undo(LevelData data) {
            data.setTile(x, y, oldTile);
        }
        
        @Override
        public void redo(LevelData data) {
            data.setTile(x, y, newTile);
        }
        
        @Override
        public String getDescription() {
            return "Paint tile at (" + x + "," + y + ")";
        }
    }
    
    /**
     * Multiple tile changes grouped together.
     */
    public static class MultiTileAction implements EditAction {
        private final List<int[]> changes; // [x, y, oldTile, newTile]
        private final String description;
        
        public MultiTileAction(List<int[]> changes, String description) {
            this.changes = new ArrayList<>(changes);
            this.description = description;
        }
        
        @Override
        public void undo(LevelData data) {
            for (int[] c : changes) {
                data.setTile(c[0], c[1], c[2]); // restore old
            }
        }
        
        @Override
        public void redo(LevelData data) {
            for (int[] c : changes) {
                data.setTile(c[0], c[1], c[3]); // apply new
            }
        }
        
        @Override
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Compound action grouping multiple actions.
     */
    public static class CompoundAction implements EditAction {
        private final String description;
        private final List<EditAction> actions = new ArrayList<>();
        
        public CompoundAction(String description) {
            this.description = description;
        }
        
        @Override
        public void undo(LevelData data) {
            // Undo in reverse order
            for (int i = actions.size() - 1; i >= 0; i--) {
                actions.get(i).undo(data);
            }
        }
        
        @Override
        public void redo(LevelData data) {
            for (EditAction action : actions) {
                action.redo(data);
            }
        }
        
        @Override
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Entity add action.
     */
    public static class EntityAddAction<T> implements EditAction {
        private final List<T> list;
        private final T entity;
        private final String entityType;
        
        public EntityAddAction(List<T> list, T entity, String entityType) {
            this.list = list;
            this.entity = entity;
            this.entityType = entityType;
        }
        
        @Override
        public void undo(LevelData data) {
            list.remove(entity);
        }
        
        @Override
        public void redo(LevelData data) {
            list.add(entity);
        }
        
        @Override
        public String getDescription() {
            return "Add " + entityType;
        }
    }
    
    /**
     * Entity remove action.
     */
    public static class EntityRemoveAction<T> implements EditAction {
        private final List<T> list;
        private final T entity;
        private final int index;
        private final String entityType;
        
        public EntityRemoveAction(List<T> list, T entity, int index, String entityType) {
            this.list = list;
            this.entity = entity;
            this.index = index;
            this.entityType = entityType;
        }
        
        @Override
        public void undo(LevelData data) {
            list.add(Math.min(index, list.size()), entity);
        }
        
        @Override
        public void redo(LevelData data) {
            list.remove(entity);
        }
        
        @Override
        public String getDescription() {
            return "Remove " + entityType;
        }
    }
    
    /**
     * Entity move action.
     */
    public static class EntityMoveAction implements EditAction {
        private final LevelData.PointData point;
        private final int oldX, oldY;
        private final int newX, newY;
        private final String entityType;
        
        public EntityMoveAction(LevelData.PointData point, int oldX, int oldY, 
                                int newX, int newY, String entityType) {
            this.point = point;
            this.oldX = oldX;
            this.oldY = oldY;
            this.newX = newX;
            this.newY = newY;
            this.entityType = entityType;
        }
        
        @Override
        public void undo(LevelData data) {
            point.x = oldX;
            point.y = oldY;
        }
        
        @Override
        public void redo(LevelData data) {
            point.x = newX;
            point.y = newY;
        }
        
        @Override
        public String getDescription() {
            return "Move " + entityType;
        }
    }
}
