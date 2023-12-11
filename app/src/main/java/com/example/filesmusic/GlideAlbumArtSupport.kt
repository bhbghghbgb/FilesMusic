package com.example.filesmusic

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Priority
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import java.io.IOException
import java.nio.ByteBuffer

@GlideModule
class FilesMusicGlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        super.applyOptions(context, builder)
        builder.run {
            // disable disk cache
            setDiskCache { null }
            setDefaultRequestOptions {
                RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE).dontAnimate().fitCenter()
            }
            setLogLevel(Log.DEBUG)
        }
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        super.registerComponents(context, glide, registry)
        registry.prepend(
            FileItem.AudioFileItem::class.java,
            ByteBuffer::class.java,
            GlideAudioFileItemAlbumArtModelLoaderFactory()
        )
    }
}

class GlideAudioFileItemAlbumArtModelLoader : ModelLoader<FileItem.AudioFileItem, ByteBuffer> {
    override fun buildLoadData(
        model: FileItem.AudioFileItem, width: Int, height: Int, options: Options
    ): ModelLoader.LoadData<ByteBuffer> =
        ModelLoader.LoadData(ObjectKey(model.file), GlideAudioFileItemAlbumArtDataFetcher(model))

    override fun handles(model: FileItem.AudioFileItem): Boolean =
        model.file !== null // for instance

}

class GlideAudioFileItemAlbumArtDataFetcher(private val model: FileItem.AudioFileItem) :
    DataFetcher<ByteBuffer> {
    private var mediaMetadataRetriever: MediaMetadataRetriever? = null
    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in ByteBuffer>) {
        mediaMetadataRetriever =
            MediaMetadataRetriever().also { it.setDataSource(model.file.absolutePath) }
        callback.onDataReady(mediaMetadataRetriever?.embeddedPicture?.let {
            model.embeddedAlbumArtLoadSuccess = true
            ByteBuffer.wrap(it)
        }.also { cleanup() })
    }

    override fun cleanup() {
        try {
            mediaMetadataRetriever?.release()
        } catch (_: IOException) {
            // ignore
        }
    }

    override fun cancel() {
        // cannot cancel
    }

    override fun getDataClass(): Class<ByteBuffer> = ByteBuffer::class.java

    override fun getDataSource(): DataSource = DataSource.LOCAL

}

class GlideAudioFileItemAlbumArtModelLoaderFactory :
    ModelLoaderFactory<FileItem.AudioFileItem, ByteBuffer> {
    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<FileItem.AudioFileItem, ByteBuffer> =
        GlideAudioFileItemAlbumArtModelLoader()

    override fun teardown() {
        // Nothing to do
    }

}