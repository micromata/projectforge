package org.projectforge.framework.persistence.history;

enum class EntityOpType {
    /**
     * The Insert.
     */
    Insert,

    /**
     * The Update.
     */
    Update,

    /**
     * The Deleted.
     */
    Deleted,

    /**
     * The Mark deleted.
     */
    MarkDeleted,

    /**
     * The Umark deleted.
     */
    UmarkDeleted
}
