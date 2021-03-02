package com.ando.file.sample.ui.upload;

/**
 * <p>描述：上传进度回调接口</p>
 *
 * @author xuexiang
 * @since 2018/6/21 上午1:57
 */
public interface IProgressResponseCallBack {
    /**
     * 回调进度
     *
     * @param bytesWritten  当前读取响应体字节长度
     * @param contentLength 总长度
     * @param done          是否读取完成
     */
    void onResponseProgress(long bytesWritten, long contentLength, boolean done);
}
