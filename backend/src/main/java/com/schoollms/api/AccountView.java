package com.schoollms.api;

import com.schoollms.domain.Account;

public record AccountView(Long id, String email, String name, String phone, String role) {
  public static AccountView from(Account a) {
    return new AccountView(a.getId(), a.getEmail(), a.getName(), a.getPhone(), a.getRole().name());
  }
}
