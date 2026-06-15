package com.senctraiq.roles;



import com.senctraiq.users.User;
import com.senctraiq.users.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
@Service
public class RoleService {


        private final RoleRepository roleRepository;
        private final UserRepository userRepository;

        @Autowired
        public RoleService(RoleRepository roleRepository, UserRepository userRepository) {
            this.roleRepository = roleRepository;
            this.userRepository = userRepository;
        }

        public Role createNewRole(String roleName, Set<Permissions> permissions, String postedBy) {
            Role role = new Role();
            role.setName(roleName);
            role.setPermissions(permissions);
            role.setPostedBy(postedBy);
            role.setPostedTime(LocalDateTime.now());
            return roleRepository.save(role);
        }

        public Role updateRolePermissions(Long roleId, Set<Permissions> newPermissions) {
            Role role = roleRepository.findById(roleId).orElseThrow(() -> new RuntimeException("Role not found"));
            role.setPermissions(newPermissions);
            return roleRepository.save(role);
        }

        public Role assignRoleToUser(String username, Long roleId) {
            User user = userRepository.findByUsernameAndDeleted(username, false).orElseThrow(() -> new RuntimeException("User not found"));
            Role role = roleRepository.findById(roleId).orElseThrow(() -> new RuntimeException("Role not found"));

            boolean alreadyAssigned = user.getRole().stream()
                    .anyMatch(existingRole -> existingRole.getId().equals(role.getId()));
            if (!alreadyAssigned) {
                user.getRole().add(role);
            }
            userRepository.save(user);

            return role;
        }

        public Role removeRoleFromUser(String username, Long roleId) {
            User user = userRepository.findByUsernameAndDeleted(username, false).orElseThrow(() -> new RuntimeException("User not found"));
            Role role = roleRepository.findById(roleId).orElseThrow(() -> new RuntimeException("Role not found"));

            user.getRole().remove(role);
            userRepository.save(user);

            return role;
        }

        public void deleteRole(Long roleId) {
           Role role = roleRepository.findById(roleId).orElseThrow(() -> new RuntimeException("Role not found"));
           role.setDeleted(true);
           role.setDeletedTime(LocalDateTime.now());
           roleRepository.save(role);
        }

        public List<Role> getAllRoles(){
            return roleRepository.getRoleByDeletedFalse();
        }
    }

