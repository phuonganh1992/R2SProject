package com.example.food.dto.view;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseBody {
    private String responseCode;
    private String responseMessage;
    private Object responseData;

    public ResponseBody(Response response, Object data) {
        this.responseCode = response.getResponseCode();
        this.responseMessage = response.getResponseMessage();
        this.responseData = data;
    }
}
