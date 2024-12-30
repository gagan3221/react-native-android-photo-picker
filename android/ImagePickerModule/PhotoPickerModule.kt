package com.yourappname.app

import android.app.Activity
import android.content.Intent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.facebook.react.bridge.ActivityEventListener
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.BaseActivityEventListener
import com.facebook.react.bridge.Callback
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableArray

class PhotoPickerModule internal constructor(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {
    private var callback: Callback? = null

    private val mActivityEventListener: ActivityEventListener =
        object : BaseActivityEventListener() {
            override fun onActivityResult(
                activity: Activity,
                requestCode: Int,
                resultCode: Int,
                intent: Intent?
            ) {
                try {
                    if (requestCode == SINGLE_PHOTO_PICKER_REQUEST_CODE || requestCode == MULTIPLE_PHOTO_PICKER_REQUEST_CODE) {
                        if (resultCode == Activity.RESULT_OK) {
                            val resultUris = Arguments.createArray()
                            if (intent != null) {
                                if (intent.dataString != null) {
                                    resultUris.pushString(intent.dataString)
                                    sendMessageToJS(PhotoPickerConstants.SUCCESS, resultUris)
                                } else {
                                    val clipData = intent.clipData
                                    if (clipData != null) {
                                        val count = clipData.itemCount
                                        for (i in 0 until count) {
                                            val item = clipData.getItemAt(i)
                                            val uri = item.uri.toString()
                                            resultUris.pushString(uri)
                                        }
                                        sendMessageToJS(PhotoPickerConstants.SUCCESS, resultUris)
                                    }
                                }
                            }
                        } else if (resultCode == Activity.RESULT_CANCELED) {
                            sendErrorToJS(
                                PhotoPickerConstants.CANCELLED,
                                PhotoPickerConstants.CANCEL_MESSAGE
                            )
                        }
                    }
                } catch (e: Exception) {
                    sendErrorToJS(PhotoPickerConstants.ERROR, e.toString())
                }
            }
        }

    init {
        reactContext.addActivityEventListener(mActivityEventListener)
    }

    override fun getName(): String {
        return PhotoPickerConstants.PACKAGE_NAME
    }

    private fun sendMessageToJS(status: String, data: WritableArray) {
        val params = Arguments.createMap()
        params.putString(PhotoPickerConstants.STATUS, status)
        params.putArray(PhotoPickerConstants.URIS, data)

        callback!!.invoke(params)
        callback = null
    }

    private fun sendErrorToJS(status: String, data: String) {
        val params = Arguments.createMap()
        params.putString(PhotoPickerConstants.STATUS, status)
        params.putString(PhotoPickerConstants.ERROR, data)

        callback!!.invoke(params)
        callback = null
    }

    @ReactMethod
    fun launchPhotoPicker(params: ReadableMap, cb: Callback?) {
        try {
            val currentActivity = currentActivity
            callback = cb

            var multipleMedia = false
            var mimeType: String? = null
            var mediaType: String? = null

            if (params.hasKey("multipleMedia")) {
                multipleMedia = params.getBoolean("multipleMedia")
            }
            if (params.hasKey("mimeType")) {
                mimeType = params.getString("mimeType")
            }
            if (params.hasKey("mediaType")) {
                mediaType = params.getString("mediaType")
            }

            val requestCode: Int
            val intent: Intent

            val builder = PickVisualMediaRequest.Builder()
            var request = PickVisualMediaRequest()

            if (mimeType != null) {
                request = builder.setMediaType(
                    ActivityResultContracts.PickVisualMedia.SingleMimeType(
                        mimeType
                    )
                ).build()
            } else {
                if (mediaType != null) {
                    when (mediaType) {
                        PhotoPickerConstants.IMAGE_AND_VIDEO -> request = builder.setMediaType(
                            ActivityResultContracts.PickVisualMedia.ImageAndVideo
                        ).build()

                        PhotoPickerConstants.IMAGE_ONLY -> request = builder.setMediaType(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        ).build()

                        PhotoPickerConstants.VIDEO_ONLY -> request = builder.setMediaType(
                            ActivityResultContracts.PickVisualMedia.VideoOnly
                        ).build()
                    }
                } else {
                    request =
                        builder.setMediaType(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            .build()
                }
            }

            if (multipleMedia) {
                requestCode = MULTIPLE_PHOTO_PICKER_REQUEST_CODE
                intent = ActivityResultContracts.PickMultipleVisualMedia()
                    .createIntent(reactApplicationContext, request)
            } else {
                requestCode = SINGLE_PHOTO_PICKER_REQUEST_CODE
                intent = ActivityResultContracts.PickVisualMedia()
                    .createIntent(reactApplicationContext, request)
            }
            currentActivity!!.startActivityForResult(intent, requestCode)
        } catch (e: Exception) {
            sendErrorToJS(PhotoPickerConstants.ERROR, e.toString())
        }
    }

    override fun getConstants(): Map<String, Any>? {
        val constants: MutableMap<String, Any> = HashMap()
        constants[PhotoPickerConstants.RN_SUCCESS] = PhotoPickerConstants.SUCCESS
        constants[PhotoPickerConstants.RN_ERROR] = PhotoPickerConstants.ERROR
        constants[PhotoPickerConstants.RN_CANCELLED] = PhotoPickerConstants.CANCELLED
        return constants
    }

    companion object {
        private const val SINGLE_PHOTO_PICKER_REQUEST_CODE = 3
        private const val MULTIPLE_PHOTO_PICKER_REQUEST_CODE = 4
    }
}