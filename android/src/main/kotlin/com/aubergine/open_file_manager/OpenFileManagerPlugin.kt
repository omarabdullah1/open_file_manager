package com.aubergine.open_file_manager

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import android.util.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.io.File

/** OpenFileManagerPlugin */
class OpenFileManagerPlugin : FlutterPlugin, MethodCallHandler {
    
    private lateinit var channel: MethodChannel
    private lateinit var context: Context

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "open_file_manager")
        channel.setMethodCallHandler(this)
        context = flutterPluginBinding.applicationContext
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "openFileManager" -> {
                val args = call.arguments as HashMap<*, *>?
                val folderType = args?.get("folderType") as String?
                val subFolderPath = args?.get("subFolderPath") as String?
                
                // Logging the values for debugging
                Log.d("OpenFileManagerPlugin", "Folder type: $folderType, Sub-folder path: $subFolderPath")

                openFileManager(result, folderType, subFolderPath)
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    private fun openFileManager(result: Result, folderType: String?, subFolderPath: String?) {
        try {
            when (folderType) {
                null, "download" -> {
                    openDownloadFolder(result)
                }
                "recent" -> {
                    openRecentFolder(result)
                }
                "subFolder" -> {
                    // Ensure subFolderPath is valid
                    if (subFolderPath != null && subFolderPath.isNotEmpty()) {
                        openSubFolder(result, subFolderPath)
                    } else {
                        result.error("INVALID_SUBFOLDER", "Sub-folder path is required", null)
                    }
                }
                else -> {
                    result.error("UNKNOWN_FOLDER_TYPE", "Unknown folder type: $folderType", null)
                }
            }
        } catch (e: Exception) {
            result.error("EXCEPTION", "Unable to open the file manager: ${e.localizedMessage}", null)
        }
    }

    private fun openDownloadFolder(result: Result) {
        try {
            val downloadIntent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
            downloadIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(downloadIntent)
            result.success(true)
        } catch (e: Exception) {
            result.error("DOWNLOAD_FOLDER_ERROR", "Failed to open the download folder: ${e.localizedMessage}", null)
        }
    }

    private fun openRecentFolder(result: Result) {
        try {
            val uri = Environment.getExternalStorageDirectory().absolutePath
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.setDataAndType(Uri.parse(uri), "*/*")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            result.success(true)
        } catch (e: Exception) {
            result.error("RECENT_FOLDER_ERROR", "Failed to open the recent folder: ${e.localizedMessage}", null)
        }
    }

    private fun openSubFolder(result: Result, subFolderPath: String) {
        try {
            Log.d("OpenFileManagerPlugin", "Attempting to open sub-folder: $subFolderPath")

            // Validate folder path length
            if (subFolderPath.length > 1) {
                val folder = File(subFolderPath)
                
                if (folder.exists() && folder.isDirectory) {
                    val uri: Uri = FileProvider.getUriForFile(
                        context, 
                        "${context.packageName}.fileprovider", 
                        folder
                    )
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setDataAndType(uri, "resource/folder")
                    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(Intent.createChooser(intent, "Open folder"))
                    result.success(true)
                } else {
                    result.error("FOLDER_NOT_FOUND", "Sub-folder does not exist or is not a directory: $subFolderPath", null)
                }
            } else {
                result.error("INVALID_PATH", "Sub-folder path length is invalid: $subFolderPath", null)
            }
        } catch (e: Exception) {
            result.error("SUBFOLDER_ERROR", "Failed to open the sub-folder: ${e.localizedMessage}", null)
        }
    }
}
