package com.abmo

import com.abmo.model.Config
import com.abmo.services.ProviderDispatcher
import com.abmo.services.VideoDownloader
import com.abmo.util.CryptoHelper
import com.abmo.util.JavaScriptExecutor
import com.abmo.util.*
import java.io.File
import kotlin.system.exitProcess


suspend fun main(args: Array<String>) {

    val javaScriptExecutor = JavaScriptExecutor()
    val cryptoHelper = CryptoHelper(javaScriptExecutor)
    val videoDownloader = VideoDownloader(cryptoHelper)
    val cliArguments = CliArguments(args)
    val providerDispatcher = ProviderDispatcher(javaScriptExecutor)


    val outputFileName = cliArguments.getOutputFileName(args)
    val headers = cliArguments.getHeaders()


    if (outputFileName != null && !isValidPath(outputFileName)) {
        exitProcess(0)
    }

    val scanner = java.util.Scanner(System.`in`)


    try {
        println("Enter the video URL or ID (e.g., K8R6OOjS7):")
        val videoUrl = scanner.nextLine()

        val dispatcher = providerDispatcher.getProviderForUrl(videoUrl)
        val videoID = dispatcher.getVideoID(videoUrl)

        val defaultHeader = if (videoUrl.isValidUrl()) {
            mapOf("Referer" to videoUrl?.extractReferer())
        } else { emptyMap() }

        val url = "https://abysscdn.com/?v=$videoID"
        val videoMetadata = videoDownloader.getVideoMetaData(url, headers ?: defaultHeader)

        val videoSources = videoMetadata?.sources
            ?.sortedBy { it?.label?.filter { char -> char.isDigit() }?.toInt() }

        if (videoSources == null) {
            println("Video not found")
            exitProcess(0)
        }

        println("Choose the resolution you want to download:")
        videoSources
            .forEachIndexed { index, video ->
                println("${index + 1}] ${video?.label} - ${formatBytes(video?.size)}")
            }

        val choice = scanner.nextInt()
        val resolution = videoSources[choice - 1]?.label


        if (resolution != null) {

            val config = if (outputFileName == null) {
                println("\nOutput file not specified. The video will be saved in the 'Downloads' folder as '${url.getVParameter()}_$resolution.mp4'.")
                Config(url, resolution, header = headers)
            } else {
                Config(url, resolution, File(outputFileName), header = headers)
            }
            println("\nvideo with id $videoID and resolution $resolution being processed....")
            videoDownloader.downloadVideo(config, videoMetadata)
        }
    } catch (e: NoSuchElementException) {
        println("\nCtrl + C detected. Exiting...")
    }


}