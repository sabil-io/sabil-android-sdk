# Quick start

Use the Sabil APIs in your Android app to manage user devices with ease.

### Installation

1. Add the JitPack repository to your build file
   ```groovy
   allprojects {
        repositories {
            ...
            maven { url 'https://jitpack.io' }
        }
    }
   ```
2. Add the dependency
   ```
   dependencies { implementation 'com.github.sabil-io:sabil-android-sdk:2.0.11' }
   ```

### Usage

3. Configure the SDK as soon as your app launches.

   ```kotlin
   // REQUIRED
   Sabil.configure(this, "<#client id#>")
   ```

4. Attach the device to the user or identify the device without a user.

   ```kotlin
   // Set this as soon as the user ID is available and attach the device.
   Sabil.userId = "<#user id#>"
   Sabil.attach(supportFragmentManager) {
    Toast.makeText(this, "Attached. Device Id -> ${Sabil.deviceId}", Toast.LENGTH_LONG).show()
   }
   ```

   If the user can perform actions on your app without login, use the `identify` method instead.
   Both `attach` and `identify` accept metadata

   ```kotlin
   Sabil.identify {
    Log.d("Sabil", "Device identified. Identity: ${it?.identity}")
   }
   ```

   Ensure you call the `attach` or `identify` when the app enters the foreground state as well.

5. (Optional) Implement the callbacks. These are useful depending on your use-case. For account
   sharing prevention, for example, you should implement the `onCurrentDeviceLogout`
   and `onLimitExceeded` callbacks.

   ```kotlin
   Sabil.onLogoutCurrentDevice = {
           // logout the current device
       }

       Sabil.onLimitExceeded = {
           // the user has too many devices using their account
       }
   ```
