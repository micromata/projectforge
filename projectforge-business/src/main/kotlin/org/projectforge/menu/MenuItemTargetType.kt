package org.projectforge.menu

enum class MenuItemTargetType {
    /**
     * The client should redirect to the given url. If no type is given, REDIRECT is used as default.
     */
    REDIRECT,
    /**
     * The client will receive a download file after calling the rest service with the given url.
     */
    DOWNLOAD,
    /**
     * The client calls the rest service with the given url and will receive a response.
     */
    RESTCALL }