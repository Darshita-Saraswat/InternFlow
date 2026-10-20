package com.internflow.auth;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AccountDetailsService implements UserDetailsService {

	private final AccountRepository accountRepository;

	public AccountDetailsService(AccountRepository accountRepository) {
		this.accountRepository = accountRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		try {
			return accountRepository.findByEmail(username);
		} catch (org.springframework.dao.EmptyResultDataAccessException exception) {
			throw new UsernameNotFoundException("Account not found.", exception);
		}
	}
}
