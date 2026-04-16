package com.expense.splitter.service;


import com.expense.splitter.entity.User;
import com.expense.splitter.repository.UserRepository;
import org.springframework.stereotype.Service;


import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepsitory){
        this.userRepository  = userRepsitory;
    }

    public User createUser(User user){
        return userRepository.save(user);
    }

    public List<User> getAllUsers(){
        return userRepository.findAll();
    }

}
