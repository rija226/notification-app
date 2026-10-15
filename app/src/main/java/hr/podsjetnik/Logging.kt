package hr.podsjetnik

import android.util.Log

private const val TAG = "Podsjetnik"

/** Single logging entry point (`adb logcat -s Podsjetnik`). Never log task notes. */
fun log(message: String) {
    Log.i(TAG, message)
}
