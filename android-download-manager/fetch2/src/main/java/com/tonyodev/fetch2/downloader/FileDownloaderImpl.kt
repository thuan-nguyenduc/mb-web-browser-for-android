package com.tonyodev.fetch2.downloader

import com.tonyodev.fetch2.*
import com.tonyodev.fetch2.exception.FetchException
import com.tonyodev.fetch2.provider.NetworkInfoProvider
import com.tonyodev.fetch2.util.*
import java.io.BufferedInputStream
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import kotlin.math.ceil

class FileDownloaderImpl(private val initialDownload: Download,
                         private val downloader: Downloader,
                         private val progressReportingIntervalMillis: Long,
                         private val downloadBufferSizeBytes: Int,
                         private val logger: Logger,
                         private val networkInfoProvider: NetworkInfoProvider,
                         private val retryOnNetworkGain: Boolean) : FileDownloader {

    @Volatile
    override var interrupted = false
    @Volatile
    override var terminated = false
    @Volatile
    override var completedDownload = false
    override var delegate: FileDownloader.Delegate? = null
    private var total: Long = 0
    private var downloaded: Long = 0
    private var estimatedTimeRemainingInMilliseconds: Long = -1
    private var downloadInfo = initialDownload.toDownloadInfo()
    private var averageDownloadedBytesPerSecond = 0.0
    private val movingAverageCalculator = AverageCalculator(5)

    override val download: Download
        get () {
            downloadInfo.downloaded = downloaded
            downloadInfo.total = total
            return downloadInfo
        }

    override fun run() {
        var output: RandomAccessFile? = null
        var input: BufferedInputStream? = null
        var response: Downloader.Response? = null
        try {
            var file = getFile(initialDownload)
            downloaded = file.length()
            if (!interrupted) {
                val request = getRequest()
                val originalFileName = downloadInfo.originalFileName
                response = downloader.execute(request)
                val isResponseSuccessful = response?.isSuccessful ?: false

                if (!interrupted && response != null && isResponseSuccessful) {
                    total = if (response.contentLength == (-1).toLong()) {
                        -1
                    } else {
                        downloaded + response.contentLength
                    }

                    val unknownName: String = "downloadfile.bin"
                    //If we don't know filename, we need to detect it from header
                    if (originalFileName != null && originalFileName.equals(unknownName) && downloaded <= 0) {
                        val contentDisposition = response.contentDisposition
                        var fileName = DownloadUtils.parseContentDisposition(contentDisposition)
                        var newFileName = DownloadUtils.addAdditionalIntoFilename(fileName)

                        if (newFileName != null) {
                            val index = downloadInfo.file.lastIndexOf(downloadInfo.fileName)

                            if (index > 0) {
                                downloadInfo.file = downloadInfo.file.replaceRange(index, index + downloadInfo.fileName.length, newFileName)

                                if (file.exists()) {
                                    file.delete()
                                }

                                file = getFile(downloadInfo)
                                downloaded = file.length()
                            }

                            downloadInfo.fileName = newFileName
                            downloadInfo.originalFileName = fileName!!
                        }
                    }

                    output = RandomAccessFile(file, "rw")
                    if (response.code == HttpURLConnection.HTTP_PARTIAL) {
                        output.seek(downloaded)
                        logger.d("FileDownloader resuming Download $download")
                    } else {
                        output.seek(0)
                        logger.d("FileDownloader starting Download $download")
                    }
                    if (!interrupted) {
                        input = BufferedInputStream(response.byteStream, downloadBufferSizeBytes)
                        downloadInfo.downloaded = downloaded
                        downloadInfo.total = total
                        downloadInfo.fileType = getFileType(response.contentType, downloadInfo.file)

                        delegate?.onStarted(
                                download = downloadInfo,
                                etaInMilliseconds = estimatedTimeRemainingInMilliseconds,
                                downloadedBytesPerSecond = getAverageDownloadedBytesPerSecond())
                        writeToOutput(input, output)
                    }
                } else if (response == null) {
                    throw FetchException(EMPTY_RESPONSE_BODY,
                            FetchException.Code.EMPTY_RESPONSE_BODY)
                } else if (!isResponseSuccessful) {
                    throw FetchException(RESPONSE_NOT_SUCCESSFUL,
                            FetchException.Code.REQUEST_NOT_SUCCESSFUL)
                } else {
                    throw FetchException(UNKNOWN_ERROR,
                            FetchException.Code.UNKNOWN)
                }
            }
            if (!completedDownload) {
                downloadInfo.downloaded = downloaded
                downloadInfo.total = total
                delegate?.onProgress(
                        download = downloadInfo,
                        etaInMilliSeconds = estimatedTimeRemainingInMilliseconds,
                        downloadedBytesPerSecond = getAverageDownloadedBytesPerSecond())
            }
        } catch (e: Exception) {
            logger.e("FileDownloader", e)
            if (!interrupted) {
                var error = getErrorFromMessage(e.message)
                if (retryOnNetworkGain) {
                    try {
                        Thread.sleep(4000)
                    } catch (e: InterruptedException) {
                        logger.e("FileDownloader", e)
                    }
                    if (!networkInfoProvider.isNetworkAvailable) {
                        error = Error.NO_NETWORK_CONNECTION
                    }
                }
                downloadInfo.downloaded = downloaded
                downloadInfo.total = total
                downloadInfo.error = error
                delegate?.onError(download = downloadInfo)
            }
        } finally {
            try {
                output?.close()
            } catch (e: Exception) {
                logger.e("FileDownloader", e)
            }
            try {
                input?.close()
            } catch (e: Exception) {
                logger.e("FileDownloader", e)
            }
            if (response != null) {
                try {
                    downloader.disconnect(response)
                } catch (e: Exception) {
                    logger.e("FileDownloader", e)
                }
            }
            terminated = true
        }
    }

    private fun getFileType(contentType: String, file: String): FileTypeValue {
        var fileType = FileTypeUtils.getFileTypeByName(file)

        if (fileType == FileTypeValue.FILETYPE_OTHER) {
            fileType = FileTypeUtils.getFileTypeByContentType(contentType)
        }

        return fileType
    }

    private fun writeToOutput(input: BufferedInputStream, output: RandomAccessFile) {
        var reportingStopTime: Long
        var downloadSpeedStopTime: Long
        var downloadedBytesPerSecond = downloaded
        val buffer = ByteArray(downloadBufferSizeBytes)
        var reportingStartTime = System.nanoTime()
        var downloadSpeedStartTime = System.nanoTime()

        var read = input.read(buffer, 0, downloadBufferSizeBytes)
        while (!interrupted && read != -1) {
            output.write(buffer, 0, read)
            downloaded += read

            downloadSpeedStopTime = System.nanoTime()
            val downloadSpeedCheckTimeElapsed = hasIntervalTimeElapsed(downloadSpeedStartTime,
                    downloadSpeedStopTime, DEFAULT_DOWNLOAD_SPEED_REPORTING_INTERVAL_IN_MILLISECONDS)

            if (downloadSpeedCheckTimeElapsed) {
                downloadedBytesPerSecond = downloaded - downloadedBytesPerSecond
                movingAverageCalculator.add(downloadedBytesPerSecond.toDouble())
                averageDownloadedBytesPerSecond =
                        movingAverageCalculator.getMovingAverageWithWeightOnRecentValues()
                estimatedTimeRemainingInMilliseconds = calculateEstimatedTimeRemainingInMilliseconds(
                        downloadedBytes = downloaded,
                        totalBytes = total,
                        downloadedBytesPerSecond = getAverageDownloadedBytesPerSecond())
                downloadedBytesPerSecond = downloaded
            }

            reportingStopTime = System.nanoTime()
            val hasReportingTimeElapsed = hasIntervalTimeElapsed(reportingStartTime,
                    reportingStopTime, progressReportingIntervalMillis)

            if (hasReportingTimeElapsed) {
                downloadInfo.downloaded = downloaded
                downloadInfo.total = total
                delegate?.onProgress(
                        download = downloadInfo,
                        etaInMilliSeconds = estimatedTimeRemainingInMilliseconds,
                        downloadedBytesPerSecond = getAverageDownloadedBytesPerSecond())
                reportingStartTime = System.nanoTime()
            }

            if (downloadSpeedCheckTimeElapsed) {
                downloadSpeedStartTime = System.nanoTime()
            }
            read = input.read(buffer, 0, downloadBufferSizeBytes)
        }
        if (read == -1 && !interrupted) {
            total = downloaded
            completedDownload = true
            downloadInfo.downloaded = downloaded
            downloadInfo.total = total
            delegate?.onComplete(
                    download = downloadInfo)
        }
    }

    private fun getFile(download: Download): File {
        val file = File(download.file)
        if (!file.exists()) {
            if (file.parentFile != null && !file.parentFile.exists()) {
                if (file.parentFile.mkdirs()) {
                    file.createNewFile()
                    logger.d("FileDownloader download file ${file.absolutePath} created")
                }
            } else {
                file.createNewFile()
                logger.d("FileDownloader download file ${file.absolutePath} created")
            }
        }
        return file
    }

    private fun getRequest(): Downloader.Request {
        val headers = initialDownload.headers.toMutableMap()
        headers["Range"] = "bytes=$downloaded-"
        return Downloader.Request(initialDownload.url, headers)
    }

    private fun getAverageDownloadedBytesPerSecond(): Long {
        if (averageDownloadedBytesPerSecond < 1) {
            return 0L
        }
        return ceil(averageDownloadedBytesPerSecond).toLong()
    }

}