# Building an Android project via prompt (Windows)

## 1. Setting environment

### 1.1. Downloading SDK tools

Download Android SDK from [Android site](https://developer.android.com/studio).

In *Command line tools only* section, click in Windows option. Until this moment, the filename is `sdk-tools-windows-4333796.zip`.
After downloaded, extract the ZIP file inside another folder in your computer.

    C:\Users\Example\SDK_Folder

### 1.2. Adding reference to SDK tools 

If it doesn't exist, create a file named `local.properties` in project folder.

Inside this file, insert the following line:

    sdk.dir=C:\Users\Example\SDK_Folder

This directory is the folder where the ZIP file was extracted.

### 1.3. Accepting licenses of SDK tools

Using command prompt, change the directory to SDK folder:

    C:
    cd C:\Users\Example\SDK_Folder\tools\bin

After that, execute `sdkmanager` file with `licenses` argument:

    start sdkmanager --licenses

Accept all the licenses that will appear until the message `All SDK package licenses accepted` shows up.    
  
### 1.4. Reducing memory of `gradle build` (optional)

For low-memory computers, you can reduce the memory of *gradle* building, otherwise an error will appear.

Inside project folder, open `gradle.properties` file with a text editor.

Change the `org.gradle.jvmargs` attribute line to:
    
    org.gradle.jvmargs=-Xmx512m
    
### 1.5. Downloading JDK

If it doesn't exist, download JDK 8 from [Oracle site](https://www.oracle.com/technetwork/pt/java/javase/downloads/jdk8-downloads-2133151.html).

In *Java SE Development Kit 8u211* section, accept the license and click in Windows option.
Until this moment, the 32-bit version name of the file is `jdk-8u211-windows-i586.exe`.
After installed the executable, execute and install the program.

## 2. Building the project

## 2.1. Compiling the project

Using command prompt, change the directory to project folder:

    C:
    cd C:\Users\Example\Project_Folder

After that, execute `gradlew` file:

    start gradlew

The project will be compiled. If it's all right, the message `BUILD SUCCESSFUL` will appear.

## 2.2. Generating APK project

Using command prompt, change the directory to project folder:

    C:
    cd C:\Users\Example\Project_Folder

Execute `gradlew` file with `assembleDebug` argument:

    start gradlew assembleDebug
    
After executed successfully, the APK file will appear in `C:\Users\Example\Project_Folder\app\build\outputs\apk\debug`.

## 2.3. Installing APK project in device

There are two ways the user can install APK in their device.
First one is generating APK by 2.2 section, passing it via USB or Google Drive and installing manually.
Second one is by command prompt.

Using command prompt, change the directory to project folder:

    C:
    cd C:\Users\Example\Project_Folder

Execute `gradlew` file with `installDebug` argument:

    start gradlew installDebug
    
After executed successfully, the app will be available to use in user device.

## 2.4. Showing crash logs of app

After you've installed the application in your device and runned it, a crash may occur.
Since you can't see in your phone why the app crashed, you can do it by command line.

Using command prompt, change the directory to `platform-tools` inside SDK folder:

    C:
    cd C:\Users\Example\SDK_Folder\platform-tools
    
Execute `adb` file with the following arguments:

    adb logcat AndroidRuntime:E *:S
    
An empty screen will appear.

After you've connected your device in PC, try to reproduce the crash, and log will appear in command prompt.
