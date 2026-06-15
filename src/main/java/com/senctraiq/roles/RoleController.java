package com.senctraiq.roles;

import com.senctraiq.ApiResponse.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:8080")
public class RoleController {

    private final RoleService roleService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Role>> createRole(
            @RequestParam String roleName,
            @RequestParam Set<Permissions> permissions,
            @RequestParam String postedBy) {

        ApiResponse<Role> response = new ApiResponse<>();

        try {
            Role role = roleService.createNewRole(roleName, permissions, postedBy);

            response.setEntity(role);
            response.setStatusCode(HttpStatus.CREATED.value());
            response.setMessage("Role created successfully");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage( e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/update/{roleId}")
//    @PreAuthorize("hasAuthority('ADMIN_UPDATE')")
    public ResponseEntity<ApiResponse<Role>> updateRolePermissions(
            @PathVariable Long roleId,
            @RequestParam Set<Permissions> permissions) {

        ApiResponse<Role> response = new ApiResponse<>();

        try {
            Role role = roleService.updateRolePermissions(roleId, permissions);

            response.setEntity(role);
            response.setStatusCode(HttpStatus.OK.value());
            response.setMessage("Role permissions updated successfully");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.setStatusCode(HttpStatus.NOT_FOUND.value());
            response.setMessage(e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage(e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/getAllRoles")
    public ResponseEntity<ApiResponse<List<Role>>> getAllRoles() {
        ApiResponse<List<Role>> response = new ApiResponse<>();

        try {
            List<Role> roles = roleService.getAllRoles();

            if (roles.isEmpty()) {
                response.setStatusCode(HttpStatus.NOT_FOUND.value());
                response.setMessage("No roles found");
                response.setEntity(null);

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            response.setEntity(roles);
            response.setStatusCode(HttpStatus.OK.value());
            response.setMessage("Roles fetched successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage(e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/assignRole")
//    @PreAuthorize("hasAuthority('ADMIN_UPDATE')")
    public ResponseEntity<ApiResponse<Role>> assignRoleToUser(
            @RequestParam String username,
            @RequestParam Long roleId) {
        ApiResponse<Role> response = new ApiResponse<>();

        try {
            Role role = roleService.assignRoleToUser(username, roleId);

            response.setEntity(role);
            response.setStatusCode(HttpStatus.OK.value());
            response.setMessage("Role assigned successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage(e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/removeRole")
//    @PreAuthorize("hasAuthority('ADMIN_UPDATE')")
    public ResponseEntity<ApiResponse<Role>> removeRoleFromUser(
            @RequestParam String username,
            @RequestParam Long roleId) {
        ApiResponse<Role> response = new ApiResponse<>();

        try {
            Role role = roleService.removeRoleFromUser(username, roleId);
            response.setEntity(role);
            response.setStatusCode(HttpStatus.OK.value());
            response.setMessage("Role removed successfully");
            return ResponseEntity.ok(response);

        }
        catch (Exception e) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }


    }
       @DeleteMapping("/delete/{roleId}")
//       @PreAuthorize("hasAuthority('ADMIN_DELETE')")
       public ResponseEntity<ApiResponse<Role>> deleteRole(
               @PathVariable Long roleId) {
           ApiResponse<Role> response = new ApiResponse<>();

           try {
               roleService.deleteRole(roleId);
               response.setStatusCode(HttpStatus.OK.value());
               response.setMessage("Role deleted successfully");
               return ResponseEntity.ok(response);
           }
           catch (Exception e) {
               response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
               response.setMessage(e.getMessage());
               return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
           }
               }

}