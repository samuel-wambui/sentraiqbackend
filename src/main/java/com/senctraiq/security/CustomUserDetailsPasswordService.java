package com.senctraiq.security;




import com.senctraiq.users.User;
import com.senctraiq.users.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsPasswordService implements UserDetailsPasswordService {

    // Assuming you have an EmployeeRepository or similar repository to update passwords
    private final UserRepository userRepository;

    public CustomUserDetailsPasswordService(UserRepository  userRepository ) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails updatePassword(UserDetails user, String newPassword) {
        // Update the password in your data source (e.g., database)
        User user1 = userRepository.findByUsernameAndDeleted(user.getUsername(),false)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        user1.setPassword(newPassword);  // Set the new password (should be encoded)
        userRepository .save(user1);  // Save updated employee to the database

        return user;  // Return the updated UserDetails object
    }
}
