package com.danamon.autochain.controller;

import com.danamon.autochain.dto.DataResponse;
import com.danamon.autochain.dto.auth.*;
import com.danamon.autochain.service.AuthService;
import com.danamon.autochain.service.BackOfficeService;
import com.danamon.autochain.service.UserService;
import com.danamon.autochain.service.impl.AuthServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthServiceImpl authService;
//    private final UserService userService;
//    private final BackOfficeService backOfficeService;


//    ===================================== USER AUTH ===========================================

    @PostMapping("/user/register")
    public ResponseEntity<?> registerUser(@RequestBody UserRegisterRequest request){
        UserRegisterResponse user = authService.registerUser(request);
        DataResponse<UserRegisterResponse> response = DataResponse.<UserRegisterResponse>builder()
                .message("User Successfully Register")
                .statusCode(HttpStatus.OK.value())
                .data(user)
                .build();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PostMapping("/user/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest request){
        LoginResponse data = authService.authenticateUser(request, "user");
        DataResponse<LoginResponse> response = DataResponse.<LoginResponse>builder()
                .message("User Successfully login")
                .statusCode(HttpStatus.OK.value())
                .data(data)
                .build();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

//    ==================================== BACK OFFICE AUTH ======================================

    @PostMapping("/backoffice/register")
    public ResponseEntity<?> registerBackOffice(@RequestBody BackOfficeRegisterRequest request){
        BackOfficeRegisterResponse user = authService.registerBackOffice(request);
        DataResponse<BackOfficeRegisterResponse> response = DataResponse.<BackOfficeRegisterResponse>builder()
                .message("User Successfully Register")
                .statusCode(HttpStatus.OK.value())
                .data(user)
                .build();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PostMapping("/backoffice/login")
    public ResponseEntity<?> loginBackOffice(@RequestBody LoginRequest request){
        LoginResponse data = authService.authenticateUser(request, "backoffice");
        DataResponse<LoginResponse> response = DataResponse.<LoginResponse>builder()
                .message("User Successfully login")
                .statusCode(HttpStatus.OK.value())
                .data(data)
                .build();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }


}
