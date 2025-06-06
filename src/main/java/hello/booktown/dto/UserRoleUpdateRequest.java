package hello.booktown.dto;

import hello.booktown.domain.enums.UserRole;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRoleUpdateRequest {
    private UserRole role;
}