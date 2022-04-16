package com.example.food.service.user;

import com.example.food.advice.CommonException;
import com.example.food.constant.Constant;
import com.example.food.domain.Role;
import com.example.food.domain.User;
import com.example.food.dto.command.UserRegisterCommand;
import com.example.food.dto.view.Response;
import com.example.food.dto.view.UserView;
import com.example.food.repository.IRoleRepository;
import com.example.food.repository.IUserRepository;
import com.example.food.security.principal.UserPrinciple;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class UserService implements IUserService {
    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private IRoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Validator validator;


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> userOptional = userRepository.findFirstByUsername(username);
        if (!userOptional.isPresent()) throw new UsernameNotFoundException(username);
        return UserPrinciple.build(userOptional.get());
    }

    public UserView register(@NonNull UserRegisterCommand command) {
        Set<ConstraintViolation<UserRegisterCommand>> constraintViolations = validator.validate(command);
        if (!constraintViolations.isEmpty()) {
            throw new ConstraintViolationException(constraintViolations);
        }
        if (userRepository.existsByEmail(command.getEmail()))
            throw new CommonException(Response.EMAIL_IS_EXISTS, Response.EMAIL_IS_EXISTS.getResponseMessage());
        if (userRepository.existsByUsername(command.getUsername()))
            throw new CommonException(Response.USERNAME_IS_EXISTS, Response.USERNAME_IS_EXISTS.getResponseMessage());

        User user = User.builder()
                .username(command.getUsername())
                .password(passwordEncoder.encode(command.getPassword()))
                .name(command.getUsername())
                .avatar(Constant.IMAGE_USER_DEFAULT)
                .phone(command.getPhone())
                .email(command.getEmail())
                .channel(Constant.ChannelName.USER)
                .status(Constant.UserStatus.ACTIVATE)
                .createdAt(Instant.now())
                .createdBy(command.getUsername())
                .build();
        Set<Role> userRoles=new HashSet<>();
        userRoles.add(roleRepository.findByName("USER"));
        user.setRoles(userRoles);
        User userSave = userRepository.save(user);
        return UserView.from(userSave);
    }
    public Optional<User> findFirstByUsername(String username){
        return userRepository.findFirstByUsername(username);
    }

    @Override
    public Iterable<User> findAll() {
        return null;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    @Override
    public User save(User user) {
        return null;
    }

    @Override
    public void delete(UUID id) {
        Optional<User> userOptional = findById(id);
        if (userOptional.isPresent()) userRepository.deleteById(id);
        else throw new CommonException(Response.OBJECT_NOT_FOUND, Response.OBJECT_NOT_FOUND.getResponseMessage());
    }
}
