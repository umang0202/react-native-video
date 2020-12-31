package com.brentvatne.exoplayer;

import android.content.Context;
import android.util.Log;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.upstream.cache.CacheSpan;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import androidx.annotation.NonNull;

public class ExoPlayerCache extends ReactContextBaseJavaModule {

    private static String DEFAULT_CACHE_CHILD_FOLDER = "exoplayercache";
    private static long DEFAULT_CACHE_MAX_SIZE = 100 * 1024 * 1024;

    private static ExoDatabaseProvider exoDatabaseProvider = null;
    private static File cacheFolder = null;
    private static SimpleCache videoCache = null;

    public ExoPlayerCache(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "ExoPlayerCache";
    }

    @ReactMethod
    public void initializeCache (@NonNull String cacheChildFolder, double cacheMaxSize) {
        _initializeCache(this.getReactApplicationContext(), cacheChildFolder, (new Double(cacheMaxSize)).longValue());
    }

    @ReactMethod
    public void getCacheStats (final Promise promise) {
        WritableMap cacheStats = new WritableNativeMap();
        try {
            if(videoCache != null) {
                cacheStats.putString("cacheFolder", cacheFolder.getPath());

                long cacheSpace = videoCache.getCacheSpace();
                cacheStats.putDouble("cacheSpace", (double)cacheSpace);

                WritableArray entries = new WritableNativeArray();
                for(String key : videoCache.getKeys()) {
                    WritableMap entry = new WritableNativeMap();
                    entry.putString("key", key);

                    WritableArray cachedSpans = new WritableNativeArray();
                    for(CacheSpan cachedSpan : videoCache.getCachedSpans(key)) {
                        WritableMap cachedSpanMap = new WritableNativeMap();
                        cachedSpanMap.putBoolean("isCached", cachedSpan.isCached);
                        cachedSpanMap.putDouble("length", (double)cachedSpan.length);
                        cachedSpanMap.putDouble("position", (double)cachedSpan.position);

                        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                        cachedSpanMap.putString("lastTouch", dateFormat.format(new java.util.Date(cachedSpan.lastTouchTimestamp)));

                        cachedSpans.pushMap(cachedSpanMap);
                    }
                    entry.putArray("cachedSpans", cachedSpans);

                    entries.pushMap(entry);
                }
                cacheStats.putArray("entries", entries);
            }
        } catch (Exception e) {
            promise.reject("getCacheStats error", e.getMessage());
            return;
        }

        promise.resolve(cacheStats);
    }

    private static void _initializeCache(Context context, String cacheChildFolder, long cacheMaxSize) {
        if(videoCache != null) {
            return;
        }
        exoDatabaseProvider = new ExoDatabaseProvider(context);
        cacheFolder = new File(context.getExternalCacheDir(), cacheChildFolder);
        videoCache = new SimpleCache(
            cacheFolder,
            new LeastRecentlyUsedCacheEvictor(cacheMaxSize),
            exoDatabaseProvider
        );
    }

    public static SimpleCache getVideoCache(Context context) {
        if(videoCache == null) {
            _initializeCache(context, DEFAULT_CACHE_CHILD_FOLDER, DEFAULT_CACHE_MAX_SIZE);
        }
        return videoCache;
    }
}
