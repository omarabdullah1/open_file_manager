part of 'open_file_manager.dart';

final class AndroidConfig {
  final FolderType folderType;
  final String subFolderPath;

  AndroidConfig({required this.folderType,required this.subFolderPath});
}

final class IosConfig {
  final String subFolderPath;

  IosConfig({required this.subFolderPath});
}

enum FolderType { recent, download,subFolder, }
