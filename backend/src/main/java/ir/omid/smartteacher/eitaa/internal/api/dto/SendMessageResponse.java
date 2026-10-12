package ir.omid.smartteacher.eitaa.internal.api.dto;

/** نتیجه ارسال. */
public record SendMessageResponse(
        boolean success,
        String messageId,
        String errorCode,
        String errorMessage
) {}
