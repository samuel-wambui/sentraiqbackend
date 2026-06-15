package com.senctraiq;

import com.senctraiq.roles.Role;
import com.senctraiq.roles.RoleRepository;
import com.senctraiq.users.User;
import com.senctraiq.users.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@SpringBootApplication
@EnableScheduling
public class SenctraIqApplication {
    public static void main(String[] args) {
        SpringApplication.run(SenctraIqApplication.class, args);
    }


    @Bean
    CommandLineRunner initUsers(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder

    ) {
        return args -> {
            // 1. Ensure ADMIN role exists
            Role superuser = roleRepository.findByName("SUPERUSER")
                    .orElseGet(() -> {
                        Role role = new Role();
                        role.setName("SUPERUSER");
                        role.setPostedBy("SYSTEM");
                        role.setVerifiedBy("SYSTEM");
                        role.setVerifiedTime(LocalDateTime.now());
                        role.setPostedTime(LocalDateTime.now());
                        return roleRepository.save(role);
                    });


            // 2. Check if user already exists by email
            Optional<User> existingUser = userRepository.findByEmailAndDeleted("samuelngari13@gmail.com", false);

            if (existingUser.isEmpty()) {
                User superUser = new User();
                superUser.setUsername("sw54903");
                superUser.setFirstName("Samuel");
                superUser.setLastName("Ngari");
                superUser.setEmail("samuelngari13@gmail.com");
                superUser.setPassword(passwordEncoder.encode("Bobo@092025@!"));

                List<Role> roles = new ArrayList<>();
                roles.add(superuser);
                superUser.setRole(roles); // make sure your setter is named setRole()

                userRepository.save(superUser);

                System.out.println("✅ Superuser created successfully.");
            } else {
                System.out.println(" Superuser already exists.");
            }
        };
    }
}
