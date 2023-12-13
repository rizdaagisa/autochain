package com.danamon.autochain.service.impl;

import com.danamon.autochain.constant.UserRole;
import com.danamon.autochain.dto.auth.*;
import com.danamon.autochain.entity.User;
import com.danamon.autochain.repository.UserRepository;
import com.danamon.autochain.security.BCryptUtil;
import com.danamon.autochain.security.JwtUtil;
import com.danamon.autochain.service.AuthService;
import com.danamon.autochain.util.MailSender;
import com.danamon.autochain.util.OTPGenerator;
import com.danamon.autochain.util.ValidationUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.mail.MessagingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final BCryptUtil bCryptUtil;

    private final JwtUtil jwtUtil;
    private final ValidationUtil validationUtil;
    private final AuthenticationManager authenticationManager;
    @Value("${app.autochain.super-admin-email}")
    String superUserEmail;
    @Value("${app.autochain.super-admin-username}")
    String superUserUsername;
    @Value("${app.autochain.super-admin-password}")
    String superUserPassword;

//    @PostConstruct
//    private void init() {
//        seederUserSuperAdmin(UserRegisterRequest.builder()
//                .username(superUserUsername)
//                .email(superUserEmail)
//                .password(superUserPassword)
//                .company_id("0")
//                .build());
//    }

    private void seederUserSuperAdmin(UserRegisterRequest request) {
        log.info("Start registerSuperAdmin");
        validationUtil.validate(request);
        Optional<User> username = userRepository.findByUsername(request.getUsername());
        Optional<User> email = userRepository.findByEmail(request.getEmail());

        if(username.isPresent() || email.isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT, "username / email already exist");

        User dataUser = User.builder()
                .username(request.getUsername().toLowerCase())
                .email(request.getEmail())
                .password(bCryptUtil.hashPassword(request.getPassword()))
                .user_type(UserRole.ROLE_BACKOFFICE_SUPER_ADMIN)
                .build();
        userRepository.saveAndFlush(dataUser);
        log.info("End registerSuperAdmin");
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public UserRegisterResponse registerUser(UserRegisterRequest request) {
        try {
            log.info("Start register user");
            validationUtil.validate(request);

            Optional<User> username = userRepository.findByUsername(request.getUsername());
            Optional<User> email = userRepository.findByEmail(request.getEmail());

            if (username.isPresent() || email.isPresent()) return null;

            User dataUser = User.builder()
                    .username(request.getUsername().toLowerCase())
                    .email(request.getEmail().toLowerCase())
                    .password(bCryptUtil.hashPassword(request.getPassword()))
                    .user_type(request.getUserRole())
                    .build();

            userRepository.saveAndFlush(dataUser);
            log.info("End register user");

            return UserRegisterResponse.builder()
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .user_type(dataUser.getUser_type().name())
                    .build();

        } catch (DataIntegrityViolationException e) {
            log.error("Error register user: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "user already exist");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String loginUser(UserLoginRequest request) {
        log.info("Start login User");

        validationUtil.validate(request);

        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        HashMap<String, String> info = new HashMap<>();

        // Generate OTP
        try {
            log.info("End login User");

            String url = OTPGenerator.generateURL(request.getEmail());
            OtpResponse otpResponse = OTPGenerator.getOtp();

            info.put("Code", otpResponse.getCode()+"<br>");
            info.put("Secret", otpResponse.getSecret()+"<br>");
            info.put("counter", otpResponse.getPeriod()+"<br>");
            info.put("url", url);

            MailSender.mailer("OTP", info, user.getEmail());

            return "Please check your email to see OTP code";
        }catch (Exception e){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public UserLoginResponse verifyOneTimePassword(OtpRequest otpRequest) {
        try {
            Boolean verifyOtp = OTPGenerator.verifyOtp(otpRequest);
            if (!verifyOtp) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            }
        }catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        User user = userRepository.findByEmail(otpRequest.getEmail()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        /*Authentication authenticate = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                user.getEmailX().toLowerCase(),
                null,
                user.getAuthorities()
        ));

        SecurityContextHolder.getContext().setAuthentication(authenticate);*/
        String token = jwtUtil.generateToken(user);
        return UserLoginResponse.builder()
                .user_type(user.getUser_type().name())
                .token(token)
                .build();
    }

    @Override
    public UserRegisterResponse registerBackOffice(AuthRequest request) {
        return null;
    }

    @Override
    public UserLoginResponse loginBackOffice(AuthRequest request) {
        return null;
    }

    @Override
    public String getByEmail(String email){
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT));

        HashMap<String,String> info = new HashMap<>();

        String urlBuilder = "http://localhost:5432/user/forget/"+user.getUser_id();

        try{
            URL url = new URI(urlBuilder).toURL();
            info.put("url", url.toString());

            MailSender.mailer("Password Recovery Link", info, email);
            return "Success send link for email recovery";
        }catch (MessagingException e){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (MalformedURLException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updatePassword(String id, String password) {
        User user = userRepository.findById(id).orElseThrow(EntityNotFoundException::new);
        user.setPassword(bCryptUtil.hashPassword(password));

        userRepository.saveAndFlush(user);
    }

}
