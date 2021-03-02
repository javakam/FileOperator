package com.ando.file.sample.ui.upload;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import ando.file.core.FileLogger;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;

/**
 * <p>描述：上传请求体</p>
 * 1.具有上传进度回调通知功能<br>
 * 2.防止频繁回调，上层无用的刷新<br>
 *
 * @author xuexiang
 * @since 2018/6/21 上午2:02
 */
public class UploadProgressRequestBody extends RequestBody {

    protected RequestBody delegate;
    protected IProgressResponseCallBack progressCallBack;

    protected CountingSink countingSink;

    public UploadProgressRequestBody(IProgressResponseCallBack listener) {
        this.progressCallBack = listener;
    }

    public UploadProgressRequestBody(RequestBody delegate, IProgressResponseCallBack progressCallBack) {
        this.delegate = delegate;
        this.progressCallBack = progressCallBack;
    }

    public void setRequestBody(RequestBody delegate) {
        this.delegate = delegate;
    }

    @Override
    public MediaType contentType() {
        return delegate.contentType();

    }

    /**
     * 重写调用实际的响应体的contentLength
     */
    @Override
    public long contentLength() {
        try {
            return delegate.contentLength();
        } catch (IOException e) {
            FileLogger.INSTANCE.e(e.getMessage());
            return -1;
        }
    }

    @Override
    public void writeTo(@NotNull BufferedSink sink) throws IOException {
        BufferedSink bufferedSink;

        countingSink = new CountingSink(sink);
        bufferedSink = Okio.buffer(countingSink);

        delegate.writeTo(bufferedSink);

        bufferedSink.flush();
    }


    protected final class CountingSink extends ForwardingSink {
        private long bytesWritten = 0;
        private long contentLength = 0;  //总字节长度，避免多次调用contentLength()方法
        private long lastRefreshUiTime;  //最后一次刷新的时间

        public CountingSink(Sink delegate) {
            super(delegate);
        }

        @Override
        public void write(@NotNull Buffer source, long byteCount) throws IOException {
            super.write(source, byteCount);
            if (contentLength <= 0) {
                contentLength = contentLength(); //获得contentLength的值，后续不再调用
            }
            //增加当前写入的字节数
            bytesWritten += byteCount;

            long curTime = System.currentTimeMillis();
            //每100毫秒刷新一次数据,防止频繁无用的刷新
            if (curTime - lastRefreshUiTime >= 100 || bytesWritten == contentLength) {
                progressCallBack.onResponseProgress(bytesWritten, contentLength, bytesWritten == contentLength);
                lastRefreshUiTime = System.currentTimeMillis();
            }
            FileLogger.INSTANCE.d("bytesWritten=" + bytesWritten + " ,totalBytesCount=" + contentLength);
        }
    }

}
