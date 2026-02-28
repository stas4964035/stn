package com.k44.stn.realtime.ws;

import com.k44.stn.common.error.ErrorResponse;

public record WsErrorMessage(String type, ErrorResponse error) {
    public static WsErrorMessage of(ErrorResponse error){
        return new WsErrorMessage("ERROR", error);
    }
}

//TODO: пример формирования WS ошибки(в месте, где валидируем JWT/accountStatus при WS connect/handshake)
//        ErrorResponse err = errorResponseFactory.ws(
//        HttpStatus.FORBIDDEN,
//        ErrorCode.ACCOUNT_BLOCKED,
//        "Аккаунт заблокирован",
//        Map.of()
//        );
//        WsErrorMessage msg = WsErrorMessage.of(err);
//        отправить msg клиенту и закрыть сессию

