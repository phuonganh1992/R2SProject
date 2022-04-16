package com.example.food.endpoint;

import com.example.food.advice.CommonException;
import com.example.food.advice.ValidationErrorResponse;
import com.example.food.advice.Violation;
import com.example.food.constant.Constant;
import com.example.food.domain.User;
import com.example.food.dto.command.UserLoginCommand;
import com.example.food.dto.command.UserRegisterCommand;
import com.example.food.dto.view.JwtView;
import com.example.food.dto.view.Response;
import com.example.food.dto.view.ResponseBody;
import com.example.food.security.jwt.JwtTokenUserProvider;
import com.example.food.security.principal.UserPrinciple;
import com.example.food.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Optional;

@RequestMapping("/auths")
@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuthEndpoint {
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenUserProvider jwtTokenUserProvider;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<ResponseBody> register(@RequestBody UserRegisterCommand command) {
        ValidationErrorResponse error = new ValidationErrorResponse();
        try {
            return new ResponseEntity<>(new ResponseBody(Response.SUCCESS, userService.register(command)), HttpStatus.CREATED);
        } catch (ConstraintViolationException e) {
            for (ConstraintViolation c : e.getConstraintViolations()) {
                error.getViolations().add(new Violation(c.getPropertyPath().toString(), c.getMessage()));
            }
            return new ResponseEntity<>(new ResponseBody(Response.OBJECT_INVALID, error), HttpStatus.BAD_REQUEST);
        } catch (CommonException e) {
            return new ResponseEntity<>(new ResponseBody(e.getResponse(), e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResponseBody(Response.SYSTEM_ERROR, e.getMessage()), HttpStatus.OK);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ResponseBody> login(@RequestBody UserLoginCommand command) {
        Optional<User> optionalUser = userService.findFirstByUsername(command.getUsername());
        try {
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(command.getUsername(), command.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtTokenUserProvider.generateTokenLogin(authentication);
            UserPrinciple userPrinciple = (UserPrinciple) authentication.getPrincipal();
            User currentUser = optionalUser.get();

            if (currentUser.getStatus().equals(Constant.UserStatus.INACTIVATE)) {
                return new ResponseEntity<>(new ResponseBody(Response.ACCOUNT_IS_LOCK, null), HttpStatus.BAD_REQUEST);
            }
            return new ResponseEntity<>(new ResponseBody(Response.SUCCESS,
                    new JwtView(currentUser.getId(), jwt, userPrinciple.getUsername(), currentUser.getName(), userPrinciple.getAuthorities())),
                    HttpStatus.OK);
        } catch (
                BadCredentialsException e) {
            if (!optionalUser.isPresent()) {
                return new ResponseEntity<>(new ResponseBody(Response.USERNAME_NOT_FOUND, null), HttpStatus.BAD_REQUEST);
            } else {
                String encodePassword = optionalUser.get().getPassword();
                if (!passwordEncoder.matches(command.getPassword(), encodePassword)) {
                    return new ResponseEntity<>(new ResponseBody(Response.PASSWORD_INCORRECT, null), HttpStatus.BAD_REQUEST);
                }
                return new ResponseEntity<>(new ResponseBody(Response.OBJECT_NOT_FOUND, null), HttpStatus.FORBIDDEN);
            }
        }
    }
}
