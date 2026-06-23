package com.senctraiq.users;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.senctraiq.roles.Role;
import com.senctraiq.shifts.Shift;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", length = 20, unique = true, nullable = false)
    private String username;

    @Column(name = "pf_number", length = 20)
    private String pfNumber;

    @Column(name = "firstname", length = 10, nullable = false)
    private String firstName;

    @Column(name = "lastname", length = 10, nullable = false)
    private String lastName;

    @Column(name = "email", length = 150, nullable = false, unique = true)
    private String email;
    @JsonIgnore
    @Column(name = "password",  nullable = false)
    private String password;
    @Column(name = "on_shift")
    private Boolean isOnShift= false;
    @Column(name = "shift_start")
    private LocalDateTime shiftStart;
    @Column(name = "shift_end")
    private LocalDateTime shiftEnd;
    //private String verificationCode;
    private boolean deleted = false;
    private boolean verified;
    private boolean locked;
    @Column(name = "force_password_change", nullable = false)
    private boolean forcePasswordChange = false;
    @Column(name = "logged_in")
    private Boolean loggedIn = false;
    @Column(name = "on_leave")
    private Boolean isOnLeave = false;
    @Column(name = "available_for_shift")
    private Boolean availableForShift = true;
    @Column(name = "shift_group", length = 30)
    private String shiftGroup;
    @Transient
    private Long leaveDaysThisYear = 0L;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "User_roles",
            joinColumns = @JoinColumn(name = "User_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private List<Role> role = new ArrayList<>();
    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "shift_id")
    private Shift shift;

}
