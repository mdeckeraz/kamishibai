package com.kamishibai.dto;

import lombok.Data;
import java.util.Set;

@Data
public class BoardShareRequest {
    private Set<String> emails;  // emails of accounts to share with
}
