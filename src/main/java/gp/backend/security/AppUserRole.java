package gp.backend.security;

import org.springframework.security.core.GrantedAuthority;

public enum AppUserRole implements GrantedAuthority {
  ROLE_ADMIN, ROLE_MANAGEMENT, ROLE_HELP_DESK;

  public String getAuthority() {
    return name();
  }

}
