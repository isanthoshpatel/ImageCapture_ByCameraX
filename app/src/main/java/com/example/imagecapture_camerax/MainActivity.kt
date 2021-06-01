package com.example.imagecapture_camerax

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.impl.ImageCaptureConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {


    var frontCam = false
    var savedUri: Uri? = null
    var storagePermissionGranted = false
    var cameraPermissionGranted = false
    private var camera: Camera? = null
    lateinit var cameraSelector: CameraSelector
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request camera permissions


          reqPermissions()


        camera_capture_button.setOnClickListener {
            if (storagePermissionGranted && cameraPermissionGranted) {
                takePhoto()
            }else{
                reqPermissions()
            }
        }
        bt_changeCamera.setOnClickListener {
            frontCam=!frontCam
            startCamera()
        }

        iv2.setOnClickListener {
            var i = Intent(this@MainActivity, Activity2::class.java)
            i.data = savedUri
            startActivity(i)

        }
        bt_retry.setOnClickListener {

            fl.visibility = View.INVISIBLE
            camera_capture_button.visibility =View.VISIBLE
            bt_changeCamera.visibility = View.VISIBLE
            startCamera()
        }
        bt_ok.setOnClickListener {
            camera_capture_button.visibility =View.VISIBLE
            bt_changeCamera.visibility = View.VISIBLE
            fl.visibility = View.INVISIBLE
            var i = Intent(this@MainActivity, Activity2::class.java)
            i.data = savedUri
            startActivity(i)
        }
        // Setup the listener for take photo button

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        if (requestCode == 1 && grantResults.size >= 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                storagePermissionGranted = true
            } else {
             reqPermissions()
            }
        }
        if (requestCode == 2 && grantResults.size >= 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                cameraPermissionGranted = true
            } else {
               reqPermissions()
            }
        }
        if (storagePermissionGranted && cameraPermissionGranted) {
            startCamera()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            if (frontCam) {
                cameraSelector =
                    CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build()
            } else {
                cameraSelector =
                    CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build()
            }


            // Preview
            preview = Preview.Builder()
                .build()


            imageCapture = ImageCapture.Builder()
                .build()

            val orientationEventListener = object : OrientationEventListener(this as Context) {
                override fun onOrientationChanged(orientation: Int) {
                    // Monitors orientation values to determine the target rotation value
                    val rotation: Int = when (orientation) {
                        in 45..134 -> Surface.ROTATION_270
                        in 135..224 -> Surface.ROTATION_180
                        in 225..314 -> Surface.ROTATION_90
                        else -> Surface.ROTATION_0
                    }

                    imageCapture!!.targetRotation = rotation
                }
            }
            orientationEventListener.enable()

            // Select back camera

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                preview?.setSurfaceProvider(previewView.createSurfaceProvider(camera?.cameraInfo))
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }


    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return
        //.setTargetRotation(previewView.display.rotation)


        // Create timestamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Setup image capture listener which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    savedUri = Uri.fromFile(photoFile)

                    iv2.setImageURI(savedUri)
                    fl.visibility = View.VISIBLE
                    camera_capture_button.visibility = View.INVISIBLE
                    bt_changeCamera.visibility=View.INVISIBLE
                    iv_preview.setImageURI(savedUri)

                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                }
            })
    }


    fun getOutputDirectory(): File {
        val mediaDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            externalMediaDirs.firstOrNull()?.let {
                File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
            }
        } else {
            TODO("VERSION.SDK_INT < LOLLIPOP")
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }


    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    fun reqPermissions(){
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            storagePermissionGranted = true

        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1
            )
        }

        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionGranted = true

        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.CAMERA), 2
            )
        }
        if (storagePermissionGranted&&cameraPermissionGranted){
            startCamera()
        }
    }


}



