package com.tonyodev.fetch2.util;

/**
 * Created by thuan.nguyenduc on 14/12/2015.
 */
public class FileTypeUtils {
    public static final String[][] FILETYPE_DOC = new String[][] {
            {"application/msword",".doc"},
            {"application/vnd.ms-powerpoint",".ppt"},
            {"application/vnd.ms-excel",".xls"},
            {"application/x-msaccess",".mdb"},
            {"application/vnd.openxmlformats-officedocument.wordprocessingml.document",".docx"},
            {"application/vnd.openxmlformats-officedocument.presentationml.presentation",".pptx"},
            {"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",".xls"},
            {"application/pdf",".pdf"},
            {"text/plain",".txt"}
            //{"application/x-msdownload",".exe"},
    };
    public static final String[][] FILETYPE_IMAGE = new String[][] {
            {"image/png",".png"},
            {"image/bmp",".bmp"},
            {"image/jpeg",".jpeg"},
            {"image/jpeg",".jpg"},
            {"image/gif",".gif"},
            {"image/x-icon",".ico"},
            {"image/svg+xml",".svg"},
            {"image/tiff",".tiff"},
            {"image/webp",".webp"}
    };
    public static final String[][] FILETYPE_ARCHIVE = new String[][] {
            {"application/zip",".zip"},
            {"application/x-tar",".tar"},
            {"application/x-rar-compressed",".rar"},
            {"application/x-7z-compressed",".7z"},
            {"application/x-bzip",".bz"},
            {"application/x-bzip2",".bz2"},
            {"application/x-tar-gz",".tgz"}
    };
    public static final String[][] FILETYPE_VIDEO = new String[][] {
            {"video/3gpp",".3gp"},
            {"video/3gpp2",".3g2"},
            {"video/x-msvideo",".avi"},
            {"video/x-flv",".flv"},
            {"video/h261",".h261"},
            {"video/h263",".h263"},
            {"video/h264",".h264"},
            {"video/jpm",".jpm"},
            {"video/jpeg",".jpgv"},
            {"video/x-m4v",".m4v"},
            {"video/x-ms-wmv",".wmv"},
            {"video/mpeg",".mpeg"},
            {"video/mp4",".mp4"},
            {"application/mp4",".mp4"},
            {"video/ogg",".ogv"},
            {"video/webm",".webm"},
            {"video/x-sgi-movie",".movie"},
            {"video/quicktime",".qt"},
            {"video/vnd.vivo",".viv"},
    };
    public static final String[][] FILETYPE_MUSIC = new String[][] {
            {"audio/x-wav",".wav"},
            {"audio/x-ms-wma",".wma"},
            {"audio/midi",".mid"},
            {"audio/mpeg",".mpga"},
            {"audio/mp4",".mp4a"},
            {"audio/ogg",".oga"},
            {"audio/webm",".weba"},
            {"audio/webm",".weba"},
            {"audio/webm",".weba"},
            {"application/octet-stream",".mp3"}
    };
    public static final String[][] FILETYPE_PROGRAM= new String[][] {
            {"application/vnd.android.package-archive",".apk"}
    };

    public static FileTypeValue getFileTypeByName(String fileName) {
        fileName = fileName.toLowerCase();
        for(int i = 0; i<FILETYPE_ARCHIVE.length ;i++) {
            if(fileName.lastIndexOf(FILETYPE_ARCHIVE[i][1]) >= 0) {
                return FileTypeValue.FILETYPE_ARCHIVE;
            }
        }
        for(int i = 0; i<FILETYPE_DOC.length ;i++) {
            if(fileName.lastIndexOf(FILETYPE_DOC[i][1]) >= 0) {
                return FileTypeValue.FILETYPE_DOC;
            }
        }
        for(int i = 0; i<FILETYPE_IMAGE.length ;i++) {
            if(fileName.lastIndexOf(FILETYPE_IMAGE[i][1]) >= 0) {
                return FileTypeValue.FILETYPE_IMAGE;
            }
        }
        for(int i = 0; i<FILETYPE_MUSIC.length ;i++) {
            if(fileName.lastIndexOf(FILETYPE_MUSIC[i][1]) >= 0) {
                return FileTypeValue.FILETYPE_MUSIC;
            }
        }
        for(int i = 0; i<FILETYPE_PROGRAM.length ;i++) {
            if(fileName.lastIndexOf(FILETYPE_PROGRAM[i][1]) >= 0) {
                return FileTypeValue.FILETYPE_PROGRAM;
            }
        }
        for(int i = 0; i<FILETYPE_VIDEO.length ;i++) {
            if(fileName.lastIndexOf(FILETYPE_VIDEO[i][1]) >= 0) {
                return FileTypeValue.FILETYPE_VIDEO;
            }
        }
        return FileTypeValue.FILETYPE_OTHER;
    }

    public static FileTypeValue getFileTypeByContentType(String contentType) {
        contentType = contentType.toLowerCase();
        for(int i = 0; i<FILETYPE_ARCHIVE.length ;i++) {
            if(contentType.contains(FILETYPE_ARCHIVE[i][0])) {
                return FileTypeValue.FILETYPE_ARCHIVE;
            }
        }
        for(int i = 0; i<FILETYPE_DOC.length ;i++) {
            if(contentType.contains(FILETYPE_DOC[i][0])) {
                return FileTypeValue.FILETYPE_DOC;
            }
        }
        for(int i = 0; i<FILETYPE_IMAGE.length ;i++) {
            if(contentType.contains(FILETYPE_IMAGE[i][0])) {
                return FileTypeValue.FILETYPE_IMAGE;
            }
        }
        for(int i = 0; i<FILETYPE_MUSIC.length ;i++) {
            if(contentType.contains(FILETYPE_MUSIC[i][0])) {
                return FileTypeValue.FILETYPE_MUSIC;
            }
        }
        for(int i = 0; i<FILETYPE_PROGRAM.length ;i++) {
            if(contentType.contains(FILETYPE_PROGRAM[i][0])) {
                return FileTypeValue.FILETYPE_PROGRAM;
            }
        }
        for(int i = 0; i<FILETYPE_VIDEO.length ;i++) {
            if(contentType.contains(FILETYPE_VIDEO[i][0])) {
                return FileTypeValue.FILETYPE_VIDEO;
            }
        }
        return FileTypeValue.FILETYPE_OTHER;
    }

    public static String getExtByContentType(String contentType) {
        contentType = contentType.toLowerCase();
        for(int i = 0; i<FILETYPE_ARCHIVE.length ;i++) {
            if(contentType.contains(FILETYPE_ARCHIVE[i][0])) {
                return FILETYPE_ARCHIVE[i][1];
            }
        }
        for(int i = 0; i<FILETYPE_DOC.length ;i++) {
            if(contentType.contains(FILETYPE_DOC[i][0])) {
                return FILETYPE_DOC[i][1];
            }
        }
        for(int i = 0; i<FILETYPE_IMAGE.length ;i++) {
            if(contentType.contains(FILETYPE_IMAGE[i][0])) {
                return FILETYPE_IMAGE[i][1];
            }
        }
        for(int i = 0; i<FILETYPE_MUSIC.length ;i++) {
            if(contentType.contains(FILETYPE_MUSIC[i][0])) {
                return FILETYPE_MUSIC[i][1];
            }
        }
        for(int i = 0; i<FILETYPE_PROGRAM.length ;i++) {
            if(contentType.contains(FILETYPE_PROGRAM[i][0])) {
                return FILETYPE_PROGRAM[i][1];
            }
        }
        for(int i = 0; i<FILETYPE_VIDEO.length ;i++) {
            if(contentType.contains(FILETYPE_VIDEO[i][0])) {
                return FILETYPE_VIDEO[i][1];
            }
        }
        return "*.*";
    }
}
