package com.tonyodev.fetch2.util

enum class FileTypeValue constructor(val value: Int) {

    FILETYPE_NONE (-1),

    FILETYPE_DOC(0),

    FILETYPE_IMAGE(1),

    FILETYPE_ARCHIVE(2),

    FILETYPE_VIDEO(3),

    FILETYPE_MUSIC(4),

    FILETYPE_OTHER(5),

    FILETYPE_PROGRAM(6);

    companion object {

        @JvmStatic
        fun valueOf(value: Int): FileTypeValue {
            return when (value) {
                -1 -> FILETYPE_NONE
                0 -> FILETYPE_DOC
                1 -> FILETYPE_IMAGE
                2 -> FILETYPE_ARCHIVE
                3 -> FILETYPE_VIDEO
                4 -> FILETYPE_MUSIC
                5 -> FILETYPE_OTHER
                6 -> FILETYPE_PROGRAM
                else -> FILETYPE_OTHER
            }
        }

    }

}