package com.example.hbooks.util

import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.ImageRequest
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import okhttp3.internal.closeQuietly
import java.io.InputStream

/**
 * A Coil ImageLoader that is capable of fetching images from Firebase Storage.
 * This is needed because the official coil-firebase artifact is deprecated.
 */
class FirebaseStorageImageLoader(private val imageLoader: ImageLoader) : ImageLoader {

    override val components = imageLoader.components
    override val defaults = imageLoader.defaults
    override val diskCache: DiskCache? = imageLoader.diskCache
    override val memoryCache: MemoryCache? = imageLoader.memoryCache

    override fun enqueue(request: ImageRequest) = imageLoader.enqueue(request)

    override fun newBuilder() = imageLoader.newBuilder()

    override fun shutdown() = imageLoader.shutdown()

    override suspend fun execute(request: ImageRequest): coil.request.ImageResult {
        if (request.data is StorageReference) {
            val stream = (request.data as StorageReference).stream.await().stream
            return imageLoader.execute(request.newBuilder().data(stream).build())
        }
        return imageLoader.execute(request)
    }
}
