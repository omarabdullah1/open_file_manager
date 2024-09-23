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
                openFileManager(result, args?.get("folderType") as String?, args?.get("subFolderPath") as String?)
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
            Log.i("OpenFileManagerPlugin", "Folder type: $folderType, Sub-folder path: $subFolderPath")

            if (folderType == null || folderType == "download") {
                // Open the Downloads folder
                val folder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath)

                if (folder.exists() && folder.isDirectory) {
                    openFolder(result, folder)
                } else {
                    Log.e("OpenFileManagerPlugin", "Downloads folder does not exist.")
                    result.error("FOLDER_NOT_FOUND", "Downloads folder does not exist.", null)
                }
            } else if (folderType == "subFolder") {
                // Open the custom sub-folder
                if (subFolderPath != null && subFolderPath.isNotEmpty()) {
                    val folder = File(subFolderPath)

                    if (folder.exists() && folder.isDirectory) {
                        openFolder(result, folder)
                    } else {
                        Log.e("OpenFileManagerPlugin", "Sub-folder does not exist or is not a directory: $subFolderPath")
                        result.error("FOLDER_NOT_FOUND", "Sub-folder does not exist or is not a directory", null)
                    }
                } else {
                    Log.e("OpenFileManagerPlugin", "Sub-folder path is null or empty")
                    result.error("INVALID_SUBFOLDER", "Sub-folder path is required", null)
                }
            }
        } catch (e: Exception) {
            Log.e("OpenFileManagerPlugin", "Exception: ${e.localizedMessage}")
            result.error("EXCEPTION", "Unable to open the file manager: ${e.localizedMessage}", null)
        }
    }

    // Helper function to open a folder using FileProvider
    private fun openFolder(result: Result, folder: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider", // Authority must match what's declared in AndroidManifest.xml
                folder
            )

            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "resource/folder")
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(Intent.createChooser(intent, "Open folder"))
            result.success(true)
        } catch (e: Exception) {
            Log.e("OpenFileManagerPlugin", "Error opening folder: ${e.localizedMessage}")
            result.error("FOLDER_ERROR", "Unable to open the folder: ${e.localizedMessage}", null)
        }
    }
}
